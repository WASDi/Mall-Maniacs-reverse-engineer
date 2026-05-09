"""
SEN scene file parser
Reverse engineered from sub_432320 in the decompiled game binary.

File format overview
--------------------
SEN is a chunked binary format (similar in spirit to IFF/RIFF).
Every chunk header is 8 bytes:

    [0..3]  FourCC tag  (4 ASCII bytes, little-endian as uint32)
    [4..7]  uint32      payload byte length (not including the 8-byte header)

The file starts with a mandatory REV2 header chunk, then an arbitrary
sequence of data chunks.  The chunks are read in one sequential pass;
order matters for cross-chunk references but is otherwise free.

Chunk types
-----------
REV2  File header.  Payload = uint32 total_data_size (bytes following
      the header, i.e. file_size - 8).  Must be the first chunk.

ONAM  Object name table.  Payload = a flat buffer of null-terminated
      strings, one per scene object.  Each string is the file-system
      path prefix for that object (e.g. "menu\\char00").  Before use the
      engine prepends a global path prefix (byte_45E950) to each string,
      expanding the buffer in place (sub_432DD0).

TNAM  Texture name table.  Payload = flat null-terminated string list,
      one texture filename per entry.  Used by sub_432260 / sub_433420
      to resolve material/texture references by index.

KEEP  Persistent mesh/scene blob.  Loaded into a scenery-pool allocation
      and kept in memory for the lifetime of the scene.  The payload is
      itself a sequence of inner chunks using the same 8-byte header
      format (parsed by sub_432C00):
          ONAM  – object names (same as top-level ONAM)
          TNAM  – texture names
          ONAM* – additional object-name entries (second occurrence)
          COLS  – colour table  (see below)
          MAPI  – material map  (see below)
          SUBO  – sub-object data
      The KEEP blob is validated before use; if sub_432C00 returns 0
      loading aborts.

TEMP  Temporary mesh blob.  Same inner structure as KEEP but freed after
      processing.  Also validated by sub_432C00.

COLS  Colour table.  Payload = array of uint32 RGBA values.
      Entry count = payload_size / 4.

OBJI  Object instance table.  Payload = array of 32-byte records.
      Entry count = payload_size / 32.
      Each record (all little-endian):
          [+00]  int32   reserved / name offset (resolved from ONAM)
          [+04]  int32   object type  (1 = standard mesh object,
                                       3 = sentinel / end marker,
                                       other = billboard/sprite type)
          [+08]  int32   mesh handle  (index into KEEP mesh table)
          [+12]  float32 x position
          [+16]  float32 y position
          [+20]  float32 z position
          [+24]  int16   rotation x  (fixed-point degrees or engine units)
          [+26]  int16   rotation y
          [+28]  int16   rotation z  (also used as sprite-frame index
                                       for billboard objects)
          [+30]  int16   scenery entry index (index into the scene
                                       object table built during loading)

NAME  Inline name string.  Payload = null-terminated name for the
      preceding MESH entry.  Written into the scenery name pool.

MESH  Mesh reference.  Payload = opaque mesh-pointer / index data loaded
      into the scenery object table as a (flags=0, ptr) pair.

TANI  Texture animation reference.  Payload = texture-animation info
      block fed to sub_434A90.

MAPI  Material mapping table.  Payload = array of 16-byte records.
      Entry count = payload_size / 16.
      Each record (all little-endian):
          [+00]  int32   material index (max index + 1 = texture-name
                                        lookup count in sub_432260)
          [+04]  int32   flags
          [+08]  int32   texture handle placeholder (filled at load time)
          [+12]  int32   reserved / padding

SUBO  Sub-object data block.  Payload is an opaque blob passed directly
      to the engine's sub-object registration function.

Unknown FourCCs are silently skipped (seeked past) by the engine.

KEEP inner-chunk parsing (sub_432C00)
--------------------------------------
The KEEP / TEMP payload is itself walked as a stream of the same
chunk-header records.  The inner parser handles:
    ONAM (0x4F4E414D)  -> dword_45E934 = start of name buffer
    TNAM (0x544E414D)  -> dword_45E990 = texture name table ptr
    ONAM (second)      -> same as first
    COLS (0x434F4C53)  -> dword_45EB28 = colour table ptr,
                          dword_45EB2C = count (size >> 2)
    MAPI (0x4D415049)  -> dword_45EB20 = material map ptr,
                          dword_45EB24 = count (size >> 4)
    SUBO (0x5355424F)  -> dword_45EB30 = sub-object ptr

All integers are little-endian.
"""

import struct
import sys
import json
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Optional

# ---------------------------------------------------------------------------
# Known FourCC tags
# ---------------------------------------------------------------------------

FOURCC = {
    b'REV2': 'REV2',
    b'ONAM': 'ONAM',
    b'TNAM': 'TNAM',
    b'KEEP': 'KEEP',
    b'TEMP': 'TEMP',
    b'COLS': 'COLS',
    b'OBJI': 'OBJI',
    b'NAME': 'NAME',
    b'MESH': 'MESH',
    b'TANI': 'TANI',
    b'MAPI': 'MAPI',
    b'SUBO': 'SUBO',
}

# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class Chunk:
    fourcc: str
    offset: int       # file offset of the chunk header
    size: int         # payload byte count

@dataclass
class ObjInstance:
    """32-byte OBJI record"""
    name_offset: int
    object_type: int
    mesh_handle: int
    x: float
    y: float
    z: float
    rot_x: int
    rot_y: int
    rot_z: int
    scene_entry_idx: int

    @property
    def type_name(self) -> str:
        return {1: 'mesh', 3: 'end_sentinel'}.get(self.object_type, f'type_{self.object_type}')

@dataclass
class MatEntry:
    """16-byte MAPI record"""
    material_index: int
    flags: int
    texture_handle: int   # placeholder; resolved at runtime
    reserved: int

@dataclass
class MeshHeader:
    """Parsed header from a MESH payload blob.

    Layout (all little-endian):
        [+00]  uint32  flags          (always 1 in observed data)
        [+04]  uint32  unknown1       (always 1 in observed data)
        [+08]  uint8   num_extra_sub  extra sub-objects beyond the root
                                      (*((_BYTE*)v9+8) in sub_430200)
        [+09]  uint8   pad0
        [+10]  uint16  pad1
        [+12]  uint32  sub_transform_offset  byte offset into blob of sub-object
                                             transform table (v9[3])
        [+16]  uint32  sub_material_offset   byte offset into blob of per-sub
                                             material index table (v9[4])
        [+20]  uint32  lod_offset     byte offset into blob of the first LOD
                                      descriptor (v9[5]); lod[+4] = poly_count
        [+24]  uint32  mesh_handle    display-list handle / render-object index
                                      (v9[6], runtime value patched at load time)
        [+28]  uint32  zero
    LOD descriptor at lod_offset (v9[5]):
        [+00]  uint32  max_draw_dist  (checked against dword_45E634)
        [+04]  uint32  poly_count     (compared to dword_45E634 for LOD selection)
    """
    flags: int
    unknown1: int
    num_extra_sub: int
    sub_transform_offset: int
    sub_material_offset: int
    lod_offset: int
    mesh_handle: int
    lod_max_draw_dist: Optional[int]
    lod_poly_count: Optional[int]
    raw_size: int


@dataclass
class TaniEntry:
    """One animated-UV record within a TANI payload.

    Parsed from sub_434B00 in the decompiled binary.

    Layout per record:
        [+00]  int8    major_u_step    added to u-offset when v-counter wraps
        [+01]  int8    major_v_step    added to v-offset when v-counter wraps
        [+02]  int8    minor_u_step    added to u-offset each sub-advance
        [+03]  int8    minor_v_step    added to v-offset each sub-advance
        [+04]  uint8   tick_period     game-ticks between sub-advances
        [+05]  uint8   v_wrap_count    sub-advances before major step fires
        [+06]  uint8   tick_counter    runtime animation state (0 in file)
        [+07]  uint8   subtick_counter runtime animation state (0 in file)
        [+08]  uint16  num_textures    number of affected texture indices
        [+10]  uint16[num_textures]  texture_indices  (MAPI row indices)
    """
    major_u_step: int
    major_v_step: int
    minor_u_step: int
    minor_v_step: int
    tick_period: int
    v_wrap_count: int
    tick_counter: int
    subtick_counter: int
    texture_indices: list[int]


@dataclass
class SenFile:
    total_data_size: int
    object_names: list[str]
    texture_names: list[str]
    object_instances: list[ObjInstance]
    material_map: list[MatEntry]
    colour_table: list[int]      # raw uint32 RGBA values
    mesh_entries: list           # list of ('name', str) | ('mesh', MeshHeader) tuples
    tani_entries: list[list[TaniEntry]]  # each TANI chunk = list of TaniEntry records
    subo_entries: list[bytes]    # raw payloads (opaque — engine sub-object reg table)
    keep_blob: Optional[bytes]   # raw KEEP payload
    temp_blob: Optional[bytes]   # raw TEMP payload
    unknown_chunks: list[Chunk]

# ---------------------------------------------------------------------------
# Reader helper
# ---------------------------------------------------------------------------

class Reader:
    def __init__(self, data: bytes):
        self._data = data
        self.pos = 0

    def remaining(self) -> int:
        return len(self._data) - self.pos

    def read(self, n: int) -> bytes:
        if self.remaining() < n:
            raise ValueError(
                f"Unexpected EOF: need {n} bytes at offset {self.pos:#x}, "
                f"only {self.remaining()} remaining"
            )
        chunk = self._data[self.pos:self.pos + n]
        self.pos += n
        return chunk

    def u32(self) -> int:
        return struct.unpack_from('<I', self.read(4))[0]

    def i32(self) -> int:
        return struct.unpack_from('<i', self.read(4))[0]

    def f32(self) -> float:
        return struct.unpack_from('<f', self.read(4))[0]

    def i16(self) -> int:
        return struct.unpack_from('<h', self.read(2))[0]

    def skip(self, n: int) -> None:
        if self.remaining() < n:
            raise ValueError(
                f"Cannot skip {n} bytes at offset {self.pos:#x}: only {self.remaining()} remaining"
            )
        self.pos += n

    def read_chunk_header(self) -> tuple[str, int, int]:
        """Returns (fourcc_str, payload_size, header_offset)."""
        offset = self.pos
        raw = self.read(4)
        size = self.u32()
        tag = FOURCC.get(raw, raw.decode('latin-1', errors='replace'))
        return tag, size, offset

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse_string_table(data: bytes) -> list[str]:
    """Split a flat buffer of null-terminated strings."""
    strings = []
    current = bytearray()
    for b in data:
        if b == 0:
            if current:
                strings.append(current.decode('latin-1'))
            current = bytearray()
        else:
            current.append(b)
    if current:
        strings.append(current.decode('latin-1'))
    return strings

def parse_obji(data: bytes) -> list[ObjInstance]:
    if len(data) % 32 != 0:
        print(
            f"Warning: OBJI payload size {len(data)} is not a multiple of 32; "
            f"truncating to {len(data) // 32} entries.",
            file=sys.stderr,
        )
    entries = []
    for i in range(len(data) // 32):
        off = i * 32
        (name_offset, obj_type, mesh_handle,
         x, y, z,
         rot_x, rot_y, rot_z, scene_idx) = struct.unpack_from('<3i3f4h', data, off)
        entries.append(ObjInstance(
            name_offset=name_offset,
            object_type=obj_type,
            mesh_handle=mesh_handle,
            x=x, y=y, z=z,
            rot_x=rot_x, rot_y=rot_y, rot_z=rot_z,
            scene_entry_idx=scene_idx,
        ))
    return entries

def parse_mapi(data: bytes) -> list[MatEntry]:
    if len(data) % 16 != 0:
        print(
            f"Warning: MAPI payload size {len(data)} is not a multiple of 16.",
            file=sys.stderr,
        )
    entries = []
    for i in range(len(data) // 16):
        off = i * 16
        mat_idx, flags, tex_handle, reserved = struct.unpack_from('<4i', data, off)
        entries.append(MatEntry(mat_idx, flags, tex_handle, reserved))
    return entries

def parse_cols(data: bytes) -> list[int]:
    count = len(data) // 4
    return list(struct.unpack_from(f'<{count}I', data))

def parse_mesh_header(data: bytes) -> MeshHeader:
    """Parse the header of a MESH payload blob.

    The blob is an engine mesh-object descriptor.  The first 32 bytes form a
    fixed header; the lod descriptor lives at the offset stored in lod_offset.
    """
    if len(data) < 28:
        return MeshHeader(
            flags=0, unknown1=0, num_extra_sub=0,
            sub_transform_offset=0, sub_material_offset=0,
            lod_offset=0, mesh_handle=0,
            lod_max_draw_dist=None, lod_poly_count=None,
            raw_size=len(data),
        )
    flags, unknown1 = struct.unpack_from('<II', data, 0)
    num_extra_sub = data[8]
    sub_transform_offset, sub_material_offset, lod_offset, mesh_handle = \
        struct.unpack_from('<IIII', data, 12)

    lod_max_draw_dist: Optional[int] = None
    lod_poly_count: Optional[int] = None
    if lod_offset + 8 <= len(data):
        lod_max_draw_dist, lod_poly_count = struct.unpack_from('<II', data, lod_offset)

    return MeshHeader(
        flags=flags,
        unknown1=unknown1,
        num_extra_sub=num_extra_sub,
        sub_transform_offset=sub_transform_offset,
        sub_material_offset=sub_material_offset,
        lod_offset=lod_offset,
        mesh_handle=mesh_handle,
        lod_max_draw_dist=lod_max_draw_dist,
        lod_poly_count=lod_poly_count,
        raw_size=len(data),
    )


def parse_tani(data: bytes) -> list[TaniEntry]:
    """Parse a TANI payload into a list of animated-UV records.

    Each record is variable-length (10 + 2 * num_textures bytes).
    The loop mirrors sub_434B00 in the decompiled binary: it walks the buffer
    until fewer than 5 bytes remain (the minimum non-empty record needs at
    least the 10-byte fixed header).
    """
    entries: list[TaniEntry] = []
    pos = 0
    while pos + 10 <= len(data):
        major_u = struct.unpack_from('b', data, pos)[0]
        major_v = struct.unpack_from('b', data, pos + 1)[0]
        minor_u = struct.unpack_from('b', data, pos + 2)[0]
        minor_v = struct.unpack_from('b', data, pos + 3)[0]
        tick_period    = data[pos + 4]
        v_wrap_count   = data[pos + 5]
        tick_counter   = data[pos + 6]
        subtick_counter = data[pos + 7]
        num_tex = struct.unpack_from('<H', data, pos + 8)[0]
        pos += 10
        tex_ids: list[int] = []
        for _ in range(num_tex):
            if pos + 2 > len(data):
                break
            tex_ids.append(struct.unpack_from('<H', data, pos)[0])
            pos += 2
        entries.append(TaniEntry(
            major_u_step=major_u,
            major_v_step=major_v,
            minor_u_step=minor_u,
            minor_v_step=minor_v,
            tick_period=tick_period,
            v_wrap_count=v_wrap_count,
            tick_counter=tick_counter,
            subtick_counter=subtick_counter,
            texture_indices=tex_ids,
        ))
    return entries


# ---------------------------------------------------------------------------
# Inner KEEP/TEMP chunk walker (mirrors sub_432C00)
# ---------------------------------------------------------------------------

def walk_inner_chunks(data: bytes) -> dict:
    """
    Walk the inner chunk stream inside a KEEP or TEMP blob.
    Returns a dict of tag -> payload bytes for recognised tags.
    """
    result: dict[str, list[bytes]] = {}
    pos = 0
    while pos + 8 <= len(data):
        raw_tag = data[pos:pos+4]
        size = struct.unpack_from('<I', data, pos+4)[0]
        pos += 8
        if pos + size > len(data):
            print(
                f"Warning: inner chunk '{raw_tag}' at offset {pos-8:#x} "
                f"claims {size} bytes but only {len(data)-pos} remain; truncating.",
                file=sys.stderr,
            )
            size = len(data) - pos
        payload = data[pos:pos+size]
        tag = FOURCC.get(raw_tag, raw_tag.decode('latin-1', errors='replace'))
        result.setdefault(tag, []).append(payload)
        pos += size
    return result

# ---------------------------------------------------------------------------
# Main parser
# ---------------------------------------------------------------------------

def parse_sen(data: bytes) -> SenFile:
    r = Reader(data)

    # --- File header ---
    # REV2 layout: [magic 4 bytes][total_remaining_bytes uint32]
    # The size field in the chunk header IS the total_data_size (bytes that follow).
    # There is no separate payload to consume — sub-chunks begin immediately after
    # the 8-byte REV2 header.  The engine reads the 8-byte header and uses the
    # size value as the loop bound (for i=0; i < total; i += chunk_size + 8).
    tag, size, _ = r.read_chunk_header()
    if tag != 'REV2':
        raise ValueError(f"Not a SEN file: expected 'REV2' magic, got {tag!r}")
    total_data_size = size   # size field = byte count of everything that follows

    object_names: list[str] = []
    texture_names: list[str] = []
    object_instances: list[ObjInstance] = []
    material_map: list[MatEntry] = []
    colour_table: list[int] = []
    mesh_entries: list[bytes] = []
    tani_entries: list[bytes] = []
    subo_entries: list[bytes] = []
    keep_blob: Optional[bytes] = None
    temp_blob: Optional[bytes] = None
    unknown_chunks: list[Chunk] = []

    while r.remaining() >= 8:
        tag, size, offset = r.read_chunk_header()
        if r.remaining() < size:
            print(
                f"Warning: chunk '{tag}' at {offset:#x} claims {size} bytes "
                f"but only {r.remaining()} remain; reading what's available.",
                file=sys.stderr,
            )
            size = r.remaining()

        payload = r.read(size)

        if tag == 'ONAM':
            object_names = parse_string_table(payload)
        elif tag == 'TNAM':
            texture_names = parse_string_table(payload)
        elif tag == 'OBJI':
            object_instances = parse_obji(payload)
        elif tag == 'MAPI':
            material_map = parse_mapi(payload)
        elif tag == 'COLS':
            colour_table = parse_cols(payload)
        elif tag == 'NAME':
            # Inline name: null-terminated string for the last MESH entry
            name = payload.split(b'\x00', 1)[0].decode('latin-1')
            if mesh_entries:
                # annotate as a tuple; the raw bytes are still in mesh_entries
                pass  # name is appended below alongside mesh
            mesh_entries.append(('name', name))
        elif tag == 'MESH':
            mesh_entries.append(('mesh', parse_mesh_header(payload)))
        elif tag == 'TANI':
            tani_entries.append(parse_tani(payload))
        elif tag == 'SUBO':
            subo_entries.append(payload)
        elif tag == 'KEEP':
            keep_blob = payload
        elif tag == 'TEMP':
            temp_blob = payload
        else:
            unknown_chunks.append(Chunk(fourcc=tag, offset=offset, size=size))

    return SenFile(
        total_data_size=total_data_size,
        object_names=object_names,
        texture_names=texture_names,
        object_instances=object_instances,
        material_map=material_map,
        colour_table=colour_table,
        mesh_entries=mesh_entries,
        tani_entries=tani_entries,
        subo_entries=subo_entries,
        keep_blob=keep_blob,
        temp_blob=temp_blob,
        unknown_chunks=unknown_chunks,
    )

# ---------------------------------------------------------------------------
# Pretty printer
# ---------------------------------------------------------------------------

def print_summary(sen: SenFile, max_lines: int = 10) -> None:
    def truncated(seq, label: str = ""):
        """Yield items from seq up to max_lines; print a truncation notice if cut."""
        for i, item in enumerate(seq):
            if i >= max_lines:
                remaining = len(seq) - max_lines
                print(f"  ... ({remaining} more{(' ' + label) if label else ''} not shown; use -N to raise limit)")
                return
            yield i, item

    print(f"SEN file")
    print(f"  Total data size : {sen.total_data_size} bytes")
    print()

    print(f"Object names ({len(sen.object_names)})")
    for i, name in truncated(sen.object_names, "names"):
        print(f"  [{i:3d}] {name!r}")

    print()
    print(f"Texture names ({len(sen.texture_names)})")
    for i, name in truncated(sen.texture_names, "names"):
        print(f"  [{i:3d}] {name!r}")

    # ONAM stores names as a flat null-terminated byte buffer;
    # OBJI name_off is a byte offset into that buffer, not a list index.
    onam_raw = b'\x00'.join(n.encode('latin-1') for n in sen.object_names)
    if onam_raw:
        onam_raw += b'\x00'

    def resolve_name(byte_offset: int) -> str:
        try:
            end = onam_raw.index(b'\x00', byte_offset)
            return onam_raw[byte_offset:end].decode('latin-1')
        except (ValueError, IndexError):
            return f'<off={byte_offset}>'

    print()
    print(f"Object instances / OBJI ({len(sen.object_instances)})")
    for i, obj in truncated(sen.object_instances, "instances"):
        name = resolve_name(obj.name_offset) if onam_raw else f'<off={obj.name_offset}>'
        print(
            f"  [{i:3d}] {name:<14s} type={obj.type_name:<8s} "
            f"mesh={obj.mesh_handle:3d}  "
            f"pos=({obj.x:9.1f}, {obj.y:8.1f}, {obj.z:8.1f})  "
            f"rot=({obj.rot_x:6d}, {obj.rot_y:6d}, {obj.rot_z:6d})  "
            f"scene_idx={obj.scene_entry_idx}"
        )

    print()
    print(f"Material map / MAPI ({len(sen.material_map)})")
    for i, m in truncated(sen.material_map, "entries"):
        print(f"  [{i:3d}] mat_idx={m.material_index}  flags=0x{m.flags:08X}  tex_handle={m.texture_handle}")

    print()
    print(f"Colour table / COLS ({len(sen.colour_table)})")
    for i, c in truncated(sen.colour_table, "colours"):
        r = (c >> 0) & 0xFF
        g = (c >> 8) & 0xFF
        b = (c >> 16) & 0xFF
        a = (c >> 24) & 0xFF
        print(f"  [{i:3d}] 0x{c:08X}  R={r} G={g} B={b} A={a}")

    print()
    print(f"MESH/NAME entries ({len(sen.mesh_entries)})")
    for i, entry in truncated(sen.mesh_entries, "entries"):
        kind, val = entry
        if kind == 'name':
            print(f"  [{i:3d}] NAME: {val!r}")
        else:
            mh: MeshHeader = val
            lod_info = ""
            if mh.lod_max_draw_dist is not None:
                lod_info = f"  lod_dist={mh.lod_max_draw_dist} poly_count={mh.lod_poly_count}"
            print(
                f"  [{i:3d}] MESH: {mh.raw_size} bytes  "
                f"flags=0x{mh.flags:08X}  num_extra_sub={mh.num_extra_sub}  "
                f"mesh_handle=0x{mh.mesh_handle:04X}{lod_info}"
            )

    print()
    print(f"TANI chunks ({len(sen.tani_entries)})")
    for i, entries in truncated(sen.tani_entries, "chunks"):
        print(f"  [{i:3d}] {len(entries)} animated-UV record(s):")
        for j, te in enumerate(entries):
            if j >= max_lines:
                print(f"    ... ({len(entries) - max_lines} more records not shown)")
                break
            print(
                f"    [{j}] tick_period={te.tick_period} v_wrap={te.v_wrap_count}  "
                f"minor_uv=({te.minor_u_step},{te.minor_v_step})  "
                f"major_uv=({te.major_u_step},{te.major_v_step})  "
                f"tex_indices={te.texture_indices}"
            )

    print()
    print(f"SUBO entries ({len(sen.subo_entries)})")
    for i, s in truncated(sen.subo_entries, "entries"):
        print(f"  [{i:3d}] {len(s)} bytes (opaque sub-object registration table)")

    if sen.keep_blob is not None:
        inner = walk_inner_chunks(sen.keep_blob)
        print()
        print(f"KEEP blob ({len(sen.keep_blob)} bytes) — inner chunks:")
        for tag, payloads in inner.items():
            for j, p in enumerate(payloads):
                if j >= max_lines:
                    print(f"  ... ({len(payloads) - max_lines} more not shown)")
                    break
                print(f"  {tag}[{j}]: {len(p)} bytes")

    if sen.temp_blob is not None:
        inner = walk_inner_chunks(sen.temp_blob)
        print()
        print(f"TEMP blob ({len(sen.temp_blob)} bytes) — inner chunks:")
        for tag, payloads in inner.items():
            for j, p in enumerate(payloads):
                if j >= max_lines:
                    print(f"  ... ({len(payloads) - max_lines} more not shown)")
                    break
                print(f"  {tag}[{j}]: {len(p)} bytes")

    if sen.unknown_chunks:
        print()
        print(f"Unknown chunks ({len(sen.unknown_chunks)})")
        for i, ch in truncated(sen.unknown_chunks, "chunks"):
            print(f"  {ch.fourcc!r} @ {ch.offset:#x}  {ch.size} bytes")


def to_dict(sen: SenFile) -> dict:
    return {
        'total_data_size': sen.total_data_size,
        'object_names': sen.object_names,
        'texture_names': sen.texture_names,
        'object_instances': [
            {
                'name_offset': o.name_offset,
                'object_type': o.object_type,
                'type_name': o.type_name,
                'mesh_handle': o.mesh_handle,
                'position': {'x': o.x, 'y': o.y, 'z': o.z},
                'rotation': {'x': o.rot_x, 'y': o.rot_y, 'z': o.rot_z},
                'scene_entry_idx': o.scene_entry_idx,
            }
            for o in sen.object_instances
        ],
        'material_map': [asdict(m) for m in sen.material_map],
        'colour_table': [f'0x{c:08X}' for c in sen.colour_table],
        'mesh_entry_count': len(sen.mesh_entries),
        'tani_entry_count': len(sen.tani_entries),
        'subo_entry_count': len(sen.subo_entries),
        'keep_blob_size': len(sen.keep_blob) if sen.keep_blob else None,
        'temp_blob_size': len(sen.temp_blob) if sen.temp_blob else None,
        'unknown_chunks': [asdict(ch) for ch in sen.unknown_chunks],
    }

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Parse a game SEN scene file.")
    parser.add_argument("file", help="Path to the .sen file")
    parser.add_argument(
        "--json", action="store_true",
        help="Output as JSON instead of the human-readable summary"
    )
    parser.add_argument(
        "-N", "--max-lines", metavar="N", type=int, default=10,
        help="Maximum lines printed per section in the summary (default: 10; 0 = unlimited)"
    )
    parser.add_argument(
        "--out", metavar="FILE",
        help="Write output to FILE instead of stdout"
    )
    args = parser.parse_args()

    path = Path(args.file)
    if not path.exists():
        print(f"Error: file not found: {path}", file=sys.stderr)
        sys.exit(1)

    data = path.read_bytes()

    try:
        sen = parse_sen(data)
    except ValueError as exc:
        print(f"Parse error: {exc}", file=sys.stderr)
        sys.exit(1)

    if args.json:
        output = json.dumps(to_dict(sen), indent=2)
    else:
        max_lines = args.max_lines if args.max_lines > 0 else float('inf')
        import io
        buf = io.StringIO()
        old = sys.stdout
        sys.stdout = buf
        print_summary(sen, max_lines=max_lines)
        sys.stdout = old
        output = buf.getvalue()

    if args.out:
        Path(args.out).write_text(output, encoding='utf-8')
        print(f"Written to {args.out}")
    else:
        print(output, end="")


if __name__ == "__main__":
    main()