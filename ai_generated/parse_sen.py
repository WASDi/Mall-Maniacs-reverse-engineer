"""
SEN scene file parser
Reverse engineered from sub_432320 in the decompiled game binary (Visual C++, x86).

════════════════════════════════════════════════════════════════════════════════
FILE FORMAT OVERVIEW
════════════════════════════════════════════════════════════════════════════════

SEN is a chunked binary format (similar in spirit to IFF/RIFF).
Every chunk header is 8 bytes:

    [0..3]  FourCC tag  (4 ASCII bytes, little-endian as uint32)
    [4..7]  uint32      payload byte length (not including the 8-byte header)

The file starts with a mandatory REV2 header chunk, then an arbitrary
sequence of data chunks.  The chunks are read in one sequential pass;
order matters for cross-chunk references but is otherwise free.

════════════════════════════════════════════════════════════════════════════════
CHUNK TYPES
════════════════════════════════════════════════════════════════════════════════

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
          [+00]  int32   name_offset   BYTE OFFSET into the ONAM string buffer
                                       (not an array index; the engine walks the
                                       buffer with a pointer, ~line 41263 of
                                       sub_432320 in decompile.c)
          [+04]  int32   object type  (1 = standard mesh object → sub_430200,
                                       3 = sentinel / end marker → loop break,
                                       other = billboard/sprite type →
                                         sub_4319E0 or sub_431B00 selected by
                                         strstr() against SubStr[16] table)
          [+08]  int32   mesh handle  (index into KEEP mesh table)
          [+12]  float32 x position
          [+16]  float32 y position
          [+20]  float32 z position
          [+24]  int16   rotation x  (fixed-point degrees or engine units)
          [+26]  int16   rotation y
          [+28]  int16   rotation z  (also used as sprite-frame index
                                       for billboard objects)
          [+30]  int16   mesh entry index (index into the array of MESH chunks
                                       loaded for this scene)

NAME  Inline name string.  Payload = null-terminated name for the
      preceding MESH entry.  Written into the scenery name pool.

MESH  Mesh reference.  Payload = opaque mesh-pointer / index data loaded
      into the scenery object table as a (flags=0, ptr) pair.

      Each MESH blob describes one mesh object in the scene.  See MeshHeader
      for the full layout.  Key fields:
          lod_level_count  (at +4)  = number of LOD levels (always 1 observed)
          num_extra_sub    (at +8)  = extra sub-objects beyond root (low byte only)
          sub_transform_offset      = table of per-sub position offsets (8 bytes/entry)
          sub_material_offset       = table of per-sub material indices (4 bytes/entry)
          lod_offset                = LOD descriptor table (48 bytes/level):
              [+0] max_draw_dist, [+4] vertex_count

      ── HOW ANM ANIMATIONS ARE APPLIED TO MESH SUB-OBJECTS ───────────────
      ANM files drive character animation via opcode 5 keyframes.  The
      connection between ANM mesh entries and SEN MESH sub-objects works as:

        1. ANM file contains a mesh table: each entry has a scene-object NAME
           and a sub_obj_idx (the sub-object slot 0-N within that scene object).

        2. At ANM load time (sub_433A90):
             - sub_431E20(mesh_name) resolves the name → runtime scene-object ptr
             - sub_obj_idx is stored as-is into a temporary array (v61)
             - For each opcode-5 keyframe in the ANM:
                 in-mem[+1] = v61[mesh_idx] (low byte) = sub_obj_idx

        3. At playback time (sub_434090, each game tick):
             - sub_430A90(scene_obj, sub_obj_idx, rot_x, rot_y, rot_z, mode=2)
             - Writes int16 Euler angles into:
                 *(scene_obj+20) + 112 * sub_obj_idx + [+0..+4]
             - Clears dirty flags at [+10], [+11]

        4. The scene-object pointer (scene_obj) is bound at ANM load time via
           sub_433A50(filename, root_bone_handle, scene_object_ptr).
           a3 = the scene-object ptr (stored at v56[6]).

        5. sub-objects 0..num_extra_sub correspond to body segments in order:
             sub[0] = root (body, no transform-table entry)
             sub[1..N] = limbs, head, etc. from sub_transform/sub_material tables

TANI  Texture animation reference.  Payload = texture-animation info
      block fed to sub_434A90 / sub_434B00.

      ── TANI / UV animation detail ──────────────────────────────────────
      This is a SEPARATE system from ANM opcode 5 (sub-mesh rotation).
      TANI drives UV texture scrolling on static mesh materials;
      ANM opcode 5 drives 3-D bone rotation via sub_430A90.

      Each TANI payload is a stream of variable-length records (sub_434B00):
          [+00]  int8    major_u_step    added to U when v_counter wraps
          [+01]  int8    major_v_step    added to V when v_counter wraps
          [+02]  int8    minor_u_step    added to U each sub-advance
          [+03]  int8    minor_v_step    added to V each sub-advance
          [+04]  uint8   tick_period     game-ticks between sub-advances
          [+05]  uint8   v_wrap_count    sub-advances before major step
          [+06]  uint8   tick_counter    runtime state (0 in file)
          [+07]  uint8   subtick_counter runtime state (0 in file)
          [+08]  uint16  num_textures
          [+10]  uint16[num_textures]  texture_indices (MAPI row indices)

      The TANI update loop (sub_434A90, called once per game tick):
          tick_counter++
          if tick_counter >= tick_period:
              tick_counter = 0
              subtick_counter++
              for each texture_index:
                  apply minor_u_step, minor_v_step to UV offset
              if subtick_counter >= v_wrap_count:
                  subtick_counter = 0
                  for each texture_index:
                      apply major_u_step, major_v_step to UV offset

MAPI  Material mapping table.  Payload = array of 16-byte records.
      Entry count = payload_size / 16.
      Each record (all little-endian):
          [+00]  int32   material index (max index + 1 = texture-name
                                        lookup count in sub_432260)
          [+04]  int32   flags
          [+08]  int32   texture handle placeholder (filled at load time)
          [+12]  int32   reserved / padding

SUBO  Sub-object data block.  *** DEAD DATA — stored but never read. ***
      The pointer is saved to dword_45EB30 (sub_432320 ~line 41089;
      sub_432C00 ~line 41438) and never dereferenced in decompile.c or
      gxSoft.c.  In CHARACTERS.SEN: 36 instances × 948 bytes each.
      Internal structure of the 948-byte per-instance blocks is unknown.

Unknown FourCCs are silently skipped (seeked past) by the engine.

════════════════════════════════════════════════════════════════════════════════
KEEP inner-chunk parsing (sub_432C00)
════════════════════════════════════════════════════════════════════════════════

The KEEP / TEMP payload is itself walked as a stream of the same
chunk-header records.  The inner parser handles:
    ONAM (0x4F4E414D)  -> dword_45E934 = start of name buffer
    TNAM (0x544E414D)  -> dword_45E990 = texture name table ptr
    ONAM (second)      -> same as first
    COLS (0x434F4C53)  -> dword_45EB28 = colour table ptr,
                          dword_45EB2C = count (size >> 2)
    MAPI (0x4D415049)  -> dword_45EB20 = material map ptr,
                          dword_45EB24 = count (size >> 4)
    SUBO (0x5355424F)  -> dword_45EB30 = ptr (SET BUT NEVER READ — dead data)

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
    """32-byte OBJI record.

    Parsed from sub_432320 (the main SEN loader).

    'name_offset' is a BYTE OFFSET into the flat ONAM string buffer (not an
    array index).  In the decompile, sub_432320 stores a raw pointer derived
    by walking the ONAM buffer byte-by-byte (v37/v39 loop at ~line 41263) and
    later passes it as a C-string pointer.  Use SenFile.onam_buffer and
    name_at_offset() to resolve the actual name string.

    'object_type' dispatch (sub_432320, ~line 41286):
        1  -> standard mesh object  -> sub_430200()
        3  -> end sentinel          -> loop break
        other -> billboard/sprite   -> sub_4319E0() or sub_431B00()
                 (selection by strstr() against SubStr[] table of 16 entries)
    """
    name_offset: int      # byte offset into flat ONAM buffer (NOT an array index)
    object_type: int
    mesh_handle: int
    x: float
    y: float
    z: float
    rot_x: int
    rot_y: int
    rot_z: int           # also sprite-frame index for billboard objects
    mesh_entry_idx: int

    @property
    def type_name(self) -> str:
        return {1: 'mesh', 3: 'end_sentinel'}.get(self.object_type, f'billboard_type_{self.object_type}')


@dataclass
class MatEntry:
    """16-byte MAPI record.

    'material_index' is the primary lookup key used by sub_432260 to find the
    corresponding texture-name entry.  'texture_handle' is a runtime placeholder
    filled at load time and meaningless in the raw file.
    """
    material_index: int
    flags: int
    texture_handle: int   # placeholder; resolved at runtime
    reserved: int


@dataclass
class SubObjectEntry:
    """Sub-object position offset and material entry (extra sub-objects beyond root).

    Each entry is read from the tables at sub_transform_offset and
    sub_material_offset inside a MESH payload blob when num_extra_sub > 0.
    The root sub-object (index 0) does NOT have a table entry — it uses
    identity transform implicitly (zero offset, identity rotation/scale).

    This entry represents sub-object slot i+1 (0-indexed), where i is its
    position in the list.  ANM opcode-5 keyframes target sub-objects by their
    slot index (0 = root, 1..N = extra subs from this table).

    Position offset table entry (8 bytes per entry at sub_transform_offset):
        [+0]  int16  offset_x   sub-object centre relative to root origin
        [+2]  int16  offset_y
        [+4]  int16  offset_z
        [+6]  int16  (not read by sub_430200; may be renderer metadata)

    Material table entry (4 bytes per entry at sub_material_offset):
        [+0]  uint32  material_index

    Consumption in sub_430200 (decompile.c lines 39359-39362):
      - The three int16s are sign-extended to int32 and stored at offsets
        +16, +20, +24 within the 112-byte runtime sub-object transform block:
            block[+16] = offset_x  (int32)
            block[+20] = offset_y  (int32)
            block[+24] = offset_z  (int32)
      - sub_4303C0 reads these for bounding-sphere calculations:
          radius = sqrt(offset_x² + offset_y² + offset_z²) + base_radius
      - The material_index is stored at block[+12] (int32).
      - Rotation words at block[+0, +2, +4] are initialised to 0 (identity)
        and are later written by ANM opcode-5 animation via sub_430A90.
      - Scale at block[+6] (float) is initialised to 1.0.
    """
    offset_x: int
    offset_y: int
    offset_z: int
    material_index: int


@dataclass
class MeshHeader:
    """Parsed header from a MESH payload blob.

    The blob is loaded wholesale into memory and then relocated in-place by
    sub_4320F0 (all offset fields become absolute pointers).  sub_430200
    allocates a runtime scene-object block of size:
        112 * num_extra_sub + 168   (= 56 + 112 * total_sub_count)

    Fixed header layout (all little-endian, offsets relative to blob start):
        [+00]  uint32  flags             (always 1 in observed data)
        [+04]  uint32  lod_level_count   number of LOD descriptor entries in the
                                         LOD table (always 1 in observed data).
                                         Used as the loop bound in sub_4320F0
                                         (~line 40823: result < *(_DWORD *)(a1+4))
        [+08]  uint32  num_extra_sub     extra sub-objects beyond the root.
                                         Only the LOW BYTE is used by sub_430200
                                         (~line 39331: v14 = *((_BYTE*)v9+8) + 1).
                                         Total sub-object count = num_extra_sub + 1.
                                         Full uint32 used only in malloc call.
        [+12]  uint32  sub_transform_offset  byte offset into blob of sub-object
                                             transform table (v9[3] in sub_430200)
        [+16]  uint32  sub_material_offset   byte offset into blob of per-sub
                                             material index table (v9[4])
        [+20]  uint32  lod_offset        byte offset into blob of the LOD descriptor
                                         table (v9[5]); each entry is 48 bytes.
        [+24]  uint32  mesh_handle       display-list handle / render-object index
                                         (v9[6], runtime value patched at load time)
        [+28]  uint32  zero              (unused / reserved)
        [+32]  uint32  0 (file) / ptr    relocated by sub_4320F0 (+32): MAPI base ptr
        [+36]  uint32  0 (file)
        [+40]  uint32  0 (file) / ptr    relocated by sub_4320F0 (+40)
        [+44]  uint32  0 (file)
        [+48]  uint32  offset / ptr      relocated by sub_4320F0 (+48); points into
                                         some sub-structure within the blob

    Sub-object transform table (at sub_transform_offset):
        One 8-byte entry per EXTRA sub-object (i.e. num_extra_sub entries).
        The ROOT sub-object (index 0) has no entry.
        Per entry:
            [+0]  int16  offset_x   position offset from root origin
            [+2]  int16  offset_y
            [+4]  int16  offset_z
            [+6]  int16  (not read by engine — may be renderer metadata)
        sub_430200 reads only the first three int16s (offsets 0, 2, 4).

    Sub-object material table (at sub_material_offset):
        One 4-byte entry per EXTRA sub-object (num_extra_sub entries).
        Per entry:
            [+0]  uint32  mat_idx   material / MAPI index for this sub-object

    LOD descriptor table (at lod_offset):
        lod_level_count × 48-byte entries.  First entry (LOD 0):
            [+00]  uint32  max_draw_dist  maximum draw distance for this LOD level
                                          (checked against dword_45E634)
            [+04]  uint32  vertex_count   number of vertices in this LOD mesh.
                                          NOTE: previously misnamed "poly_count".
                                          This is the vertex count, compared to 256
                                          in sub_4320F0 (~line 40872) and to
                                          dword_45EB14 for stats tracking.
            [+08..+47]  additional LOD data (vertex/face table offsets and counts;
                                            not yet fully parsed — see below)

    Sub-object transform block layout in the RUNTIME scene-object:
        Allocated by sub_430200 at: base_ptr = v10 + 14*4 (= v10 + 56 bytes)
        One 112-byte block per sub-object (0..num_extra_sub):
            [+0]   int16  rot_x        (engine Euler angle, 65536 = 360°)
            [+2]   int16  rot_y
            [+4]   int16  rot_z
            [+6]   float  scale        (initialised to 1.0 = 0x3F800000)
            [+10]  byte   dirty_flag_0 (0 = clean after absolute set by ANM op5)
            [+11]  byte   dirty_flag_1 (1 = needs rebuild after blend)
            [+12]  int32  mat_idx      (from sub_material_offset table)
            [+16]  int32  offset_x     (sign-extended from int16 sub_transform entry)
            [+20]  int32  offset_y
            [+24]  int32  offset_z
            [+28..+111]  renderer matrix + bounding-sphere data (gxDLL managed)

        ANM opcode 5 writes to this block via sub_430A90(scene_obj, sub_obj_idx,
        rot_x, rot_y, rot_z, flags=2), updating rot_x/y/z and clearing dirty flags.
    """
    flags: int
    lod_level_count: int     # number of LOD descriptor entries (was: unknown1)
    num_extra_sub: int
    sub_transform_offset: int
    sub_material_offset: int
    lod_offset: int
    mesh_handle: int
    lod_max_draw_dist: Optional[int]
    lod_vertex_count: Optional[int]  # previously misnamed lod_poly_count; this is vertex count
    sub_objects: list[SubObjectEntry]
    raw_size: int


@dataclass
class TaniEntry:
    """One animated-UV record within a TANI payload.

    Parsed from sub_434B00 in the decompiled binary.

    This drives UV texture scrolling on static mesh materials — a completely
    separate system from ANM opcode 5 (which rotates sub-mesh bones via
    sub_430A90).

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
    tick_counter: int       # runtime state — 0 in file, mutable during play
    subtick_counter: int    # runtime state — 0 in file, mutable during play
    texture_indices: list[int]


@dataclass
class SuboChunk:
    raw: bytes          # full uninterpreted payload
    num_instances: int  # len(raw) // bytes_per_instance (if evenly divisible by OBJI count)
    bytes_per_instance: int  # raw size / OBJI count (0 if not evenly divisible)


@dataclass
class SenFile:
    total_data_size: int
    onam_buffer: bytes               # raw flat ONAM payload (null-separated names)
    object_names: list[str]          # convenience list; use name_at_offset() for OBJI lookups
    texture_names: list[str]
    object_instances: list[ObjInstance]
    material_map: list[MatEntry]
    colour_table: list[int]              # raw uint32 RGBA values
    mesh_entries: list                   # list of ('name', str) | ('mesh', MeshHeader)
    tani_entries: list[list[TaniEntry]]  # each TANI chunk = list of TaniEntry records
    subo_entries: list[SuboChunk]        # DEAD DATA — stored to dword_45EB30, never read back
    keep_blob: Optional[bytes]           # raw KEEP payload
    temp_blob: Optional[bytes]           # raw TEMP payload
    unknown_chunks: list[Chunk]

    def name_at_offset(self, byte_offset: int) -> str:
        """Resolve a name from a byte offset into the ONAM buffer.

        OBJI records store byte offsets into the flat null-terminated ONAM
        string buffer (not array indices).  This mirrors how sub_432320 walks
        the buffer with a pointer (v39) in the OBJI processing loop
        (~line 41263 of decompile.c).
        """
        if not self.onam_buffer or byte_offset >= len(self.onam_buffer):
            return f'<onam+{byte_offset}>'
        end = self.onam_buffer.find(b'\x00', byte_offset)
        if end == -1:
            end = len(self.onam_buffer)
        return self.onam_buffer[byte_offset:end].decode('latin-1')


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


def parse_subo(data: bytes, obji_count: int) -> SuboChunk:
    """Parse the SUBO payload into a SuboChunk.

    The SUBO pointer is stored in dword_45EB30 but never read back — the
    payload is dead data in this engine version.  We preserve the raw bytes
    and document the per-instance block size for forensic purposes.

    References: sub_432320 case 0x4F425553 (~line 41089 of decompile.c);
                sub_432C00 case 1329747283 (~line 41438 of decompile.c).
    """
    bpi = 0
    ni = 0
    if obji_count > 0 and len(data) % obji_count == 0:
        bpi = len(data) // obji_count
        ni = obji_count
    return SuboChunk(raw=data, num_instances=ni, bytes_per_instance=bpi)


def parse_obji(data: bytes) -> list[ObjInstance]:
    """Parse the OBJI payload into ObjInstance records.

    Mirrors the OBJI branch in sub_432320 (~line 41171 of decompile.c).
    The first int32 field is a BYTE OFFSET into the ONAM string buffer,
    not an array index.  The engine resolves it by pointer arithmetic
    (v37/v39 loop at ~line 41263).
    """
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
            mesh_entry_idx=scene_idx,
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


def parse_sub_object_table(data: bytes,
                           num_extra_sub: int,
                           sub_transform_offset: int,
                           sub_material_offset: int) -> list[SubObjectEntry]:
    """Parse the sub-object position-offset and material tables.

    Each extra sub-object (beyond the root) has one entry in each table:

        Position offset table — 8 bytes per entry:
            [+0]  int16  offset_x
            [+2]  int16  offset_y
            [+4]  int16  offset_z
            [+6]  int16  padding

        Material table — 4 bytes per entry:
            [+0]  int32  material_index

    Mirrors sub_430200 loop (v13 = 2 .. count) in decompile.c.
    The three int16s are sign-extended to dwords and stored at offsets
    +16, +20, +24 per sub-object's 112-byte block, used as a position
    vector in bounding-sphere computation (sub_4303C0).
    """
    entries: list[SubObjectEntry] = []
    for i in range(num_extra_sub):
        t_off = sub_transform_offset + i * 8
        m_off = sub_material_offset + i * 4
        if t_off + 6 > len(data) or m_off + 4 > len(data):
            break
        off_x, off_y, off_z = struct.unpack_from('<hhh', data, t_off)
        mat_idx = struct.unpack_from('<I', data, m_off)[0]
        entries.append(SubObjectEntry(
            offset_x=off_x, offset_y=off_y, offset_z=off_z,
            material_index=mat_idx,
        ))
    return entries


def parse_mesh_header(data: bytes) -> MeshHeader:
    """Parse the header of a MESH payload blob.

    The blob is an engine mesh-object descriptor.  The first 52 bytes form the
    fixed header (up to and including the padding zeroes); the sub-object tables
    begin at sub_transform_offset and sub_material_offset; the LOD descriptor
    table begins at lod_offset (48 bytes per LOD level).

    When num_extra_sub > 0 the sub_transform_offset and sub_material_offset
    point to tables consumed by sub_430200 to initialise the extra child
    sub-objects in the runtime scene-object block.

    Mirrors the load path in sub_430200 / sub_432320 / sub_4320F0.
    """
    if len(data) < 28:
        return MeshHeader(
            flags=0, lod_level_count=0, num_extra_sub=0,
            sub_transform_offset=0, sub_material_offset=0,
            lod_offset=0, mesh_handle=0,
            lod_max_draw_dist=None, lod_vertex_count=None,
            sub_objects=[],
            raw_size=len(data),
        )
    flags, lod_level_count, num_extra_sub = struct.unpack_from('<III', data, 0)
    num_extra_sub &= 0xFF  # only lowest byte used for loop count in sub_430200; full uint32 for malloc
    sub_transform_offset, sub_material_offset, lod_offset, mesh_handle = \
        struct.unpack_from('<IIII', data, 12)

    lod_max_draw_dist: Optional[int] = None
    lod_vertex_count: Optional[int] = None
    if lod_offset + 8 <= len(data):
        lod_max_draw_dist, lod_vertex_count = struct.unpack_from('<II', data, lod_offset)

    sub_objects = parse_sub_object_table(
        data, num_extra_sub, sub_transform_offset, sub_material_offset,
    )

    return MeshHeader(
        flags=flags,
        lod_level_count=lod_level_count,
        num_extra_sub=num_extra_sub,
        sub_transform_offset=sub_transform_offset,
        sub_material_offset=sub_material_offset,
        lod_offset=lod_offset,
        mesh_handle=mesh_handle,
        lod_max_draw_dist=lod_max_draw_dist,
        lod_vertex_count=lod_vertex_count,
        sub_objects=sub_objects,
        raw_size=len(data),
    )


def parse_tani(data: bytes) -> list[TaniEntry]:
    """Parse a TANI payload into a list of animated-UV records.

    Each record is variable-length (10 + 2 * num_textures bytes).
    The loop mirrors sub_434B00 in the decompiled binary: it walks the buffer
    until fewer than 10 bytes remain (the minimum non-empty record needs at
    least the 10-byte fixed header).

    These records are consumed by sub_434A90 (the per-tick TANI update),
    which is completely separate from ANM playback (sub_434090 / sub_434290).
    """
    entries: list[TaniEntry] = []
    pos = 0
    while pos + 10 <= len(data):
        major_u = struct.unpack_from('b', data, pos)[0]
        major_v = struct.unpack_from('b', data, pos + 1)[0]
        minor_u = struct.unpack_from('b', data, pos + 2)[0]
        minor_v = struct.unpack_from('b', data, pos + 3)[0]
        tick_period     = data[pos + 4]
        v_wrap_count    = data[pos + 5]
        tick_counter    = data[pos + 6]
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
    """Walk the inner chunk stream inside a KEEP or TEMP blob.

    Mirrors sub_432C00 in the decompiled binary.
    Returns a dict of tag -> list[payload_bytes] for all recognised tags.
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
    """Parse a SEN scene file.

    Mirrors sub_432320 in decompile.c (line 40953).

    The outer loop reads 8-byte chunk headers and dispatches by FourCC tag.
    Recognised tags and their decompile equivalents:

      REV2  – file header validation and loop bound          (sub_432320, ~line 41058)
      ONAM  – object name buffer (flat null-term strings)    (sub_432320, ~line 41135 / case 0x4D414E4F)
      TNAM  – texture name table                             (sub_432320, ~line 41080 / case 0x4D414E54)
                                                               -> allocated with sub_419870, ptr → dword_45E990
      KEEP  – persistent mesh blob, inner-parsed by          (sub_432320, ~line 41071 / case 0x5045454B)
                sub_432C00 (line 41410); must pass validation
      TEMP  – temporary mesh blob (same inner format)        (sub_432320, ~line 41108 / case 0x504D4554)
                validated by sub_432C00 then freed after use
      COLS  – colour table (uint32 RGBA array)               (sub_432320, ~line 41121 / case 0x534C4F43)
                                                               -> dword_45EB28 = ptr, dword_45EB2C = count
      OBJI  – object instance table (32 bytes/entry)         (sub_432320, ~line 41171 / case 0x494A424F)
                                                               -> dword_45EAA4 = ptr, dword_45EAAC = count
      NAME  – inline mesh name string                        (sub_432320, ~line 41183 / case 0x454D414E)
                                                               -> strcpy into name pool at dword_45EAB4
      MESH  – mesh/display-list blob                         (sub_432320, ~line 41193 / case 0x4853454D)
                                                               -> sub_419870 alloc, stored as (flags=0, ptr)
                                                               -> relocation via sub_4320F0 (line 40780)
      TANI  – UV texture animation records                   (sub_432320, ~line 41149 / case 0x494E4154)
                                                               -> sub_434A90 (ticker, line 43223)
                                                               -> sub_434B00 (record parser, line 43260)
      MAPI  – material mapping table (16 bytes/entry)        (sub_432320, ~line 41157 / case 0x4950414D)
                                                               -> dword_45EB20 = ptr, dword_45EB24 = count
                                                               -> used by sub_432260 (line 40880) for
                                                                  texture-name resolution
      SUBO  – sub-object registration blob (opaque)          (sub_432320, ~line 41089 / case 0x4F425553)
                                                               -> dword_45EB30 = ptr

    Unknown FourCCs are silently skipped (sub_408D30 seek-forward, ~line 41105).
    """
    r = Reader(data)

    # --- File header ---
    # REV2 layout: [magic 4 bytes][total_remaining_bytes uint32]
    # The size field in the chunk header IS the total_data_size (bytes that follow).
    # There is no separate payload to consume — sub-chunks begin immediately after
    # the 8-byte REV2 header.  The engine reads the 8-byte header and uses the
    # size value as the loop bound (for i=0; i < total; i += chunk_size + 8).
    # Validated in sub_432320 line 41058: sub_408D10(v2, &v64, 8u)==8 && v64==844514642
    tag, size, _ = r.read_chunk_header()
    if tag != 'REV2':
        raise ValueError(f"Not a SEN file: expected 'REV2' magic, got {tag!r}")
    total_data_size = size   # size field = byte count of everything that follows

    onam_buffer: bytes = b''
    object_names: list[str] = []
    texture_names: list[str] = []
    object_instances: list[ObjInstance] = []
    material_map: list[MatEntry] = []
    colour_table: list[int] = []
    mesh_entries: list = []
    tani_entries: list[list[TaniEntry]] = []
    subo_entries: list[SuboChunk] = []
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
            # sub_432320 case 0x4D414E4F (~line 41135):
            #   dword_45E934 = dword_45E94C  (start of name pool write cursor)
            #   sub_408D10(v2, dword_45E94C, Offset)  (read into scenery pool)
            #   dword_45E94C += Offset
            # The raw buffer is a flat sequence of null-terminated strings.
            # OBJI records reference names by BYTE OFFSET into this buffer.
            onam_buffer = payload
            object_names = parse_string_table(payload)
        elif tag == 'TNAM':
            # sub_432320 case 0x4D414E54 (~line 41080):
            #   sub_419870(0, Offset) -> heap alloc (not scenery-pool)
            #   dword_45E990 = ptr, dword_45EAA8 = Offset (byte count)
            # Used by sub_432260 (line 40880) to load texture handles by index.
            texture_names = parse_string_table(payload)
        elif tag == 'OBJI':
            # sub_432320 case 0x494A424F (~line 41171):
            #   sub_419870(0, Offset) -> heap alloc
            #   dword_45EAA4 = ptr, dword_45EAAC = Offset >> 5  (count = size/32)
            # Records are 32 bytes each.  First field is a BYTE OFFSET into the
            # ONAM buffer (not an array index); resolved by pointer walk at ~line 41263.
            object_instances = parse_obji(payload)
        elif tag == 'MAPI':
            # sub_432320 case 0x4950414D (~line 41157):
            #   sub_419870(v4, Offset) -> scenery-pool alloc
            #   dword_45EB20 = ptr, dword_45EB24 = Offset >> 4  (count = size/16)
            # Records are 16 bytes each; used by sub_432260 to resolve texture
            # handles (replaces material_index field with runtime handle).
            material_map = parse_mapi(payload)
        elif tag == 'COLS':
            # sub_432320 case 0x534C4F43 (~line 41121):
            #   sub_419870(v4, Offset) -> scenery-pool alloc
            #   dword_45EB28 = ptr, dword_45EB2C = Offset >> 2  (count = size/4)
            colour_table = parse_cols(payload)
        elif tag == 'NAME':
            # sub_432320 case 0x454D414E (~line 41183):
            #   sub_408D10(v2, Buffer, Offset) -> read into local 256-byte Buffer
            #   strcpy((char *)dword_45EAB4, Buffer) -> copy into name pool
            #   dword_45EAB4 += strlen(...) + 1   -> advance name pool write ptr
            # The preceding MESH entry's slot (dword_45E948-8) is updated to
            # point at this name.
            name = payload.split(b'\x00', 1)[0].decode('latin-1')
            mesh_entries.append(('name', name))
        elif tag == 'MESH':
            # sub_432320 case 0x4853454D (~line 41193):
            #   sub_419870(v4, Offset) -> scenery-pool alloc
            #   Stored as (flags=0, ptr) pair in scene object table:
            #     *(_DWORD *)(dword_45E948 + 4) = ptr
            #     *(_DWORD *)dword_45E948 = 0
            #     dword_45E948 += 8
            #   Blob header relocated by sub_4320F0 (line 40780) after all
            #   MESH/NAME pairs are loaded.
            mesh_entries.append(('mesh', parse_mesh_header(payload)))
        elif tag == 'TANI':
            # sub_432320 case 0x494E4154 (~line 41149):
            #   sub_419870(v4, Offset) -> scenery-pool alloc  (dword_45E940)
            #   dword_45EAB0 = Offset  (byte count)
            #   After loading, passed to sub_434A90 (UV anim ticker, line 43223)
            #   which calls sub_434B00 (record walker, line 43260) per tick.
            # UV texture scrolling — separate from ANM opcode 5 (sub_430A90).
            tani_entries.append(parse_tani(payload))
        elif tag == 'SUBO':
            # sub_432320 case 0x4F425553 (~line 41089 of decompile.c):
            #   sub_419870(v4, Offset) -> scenery-pool alloc
            #   dword_45EB30 = (int)v16  (ptr stored, NEVER READ BACK)
            # sub_432C00 case 1329747283 (~line 41438): same — sets dword_45EB30.
            #
            # *** DEAD DATA *** — the payload pointer is stored in dword_45EB30
            # but is never dereferenced anywhere in decompile.c or gxSoft.c.
            # gxDLLInit (gxSoft.c line 454) fills unk_45EB40 = a1[0..30] with
            # function pointers; dword_45EB30 = a1[-4] is never touched by the DLL.
            # dword_45EB08 / dword_45EB0C (which feed a2 in the OBJI→sub_430200
            # call, ~line 41281) are both permanently 0 (sub_431CB0 line 40566).
            # See SuboChunk docstring for the full investigation.
            #
            # In CHARACTERS.SEN: 34128 bytes = 36 OBJI instances × 948 bytes each.
            # Internal structure of each 948-byte block is unknown — no code parses it.
            subo_entries.append(parse_subo(payload, len(object_instances)))
        elif tag == 'KEEP':
            # sub_432320 case 0x5045454B (~line 41071):
            #   sub_419870(v4, Offset) -> scenery-pool alloc (kept alive)
            #   sub_432C00((int *)dword_45EAA0, dword_45EAA0 + Offset) must return 1
            #   Inner chunks parsed by sub_432C00 (line 41410).
            keep_blob = payload
        elif tag == 'TEMP':
            # sub_432320 case 0x504D4554 (~line 41108):
            #   sub_419870(0, Offset) -> heap alloc (freed after processing)
            #   sub_432C00(v27, (unsigned int)v27 + Offset) must return 1
            #   Inner chunks parsed by sub_432C00 (line 41410).
            temp_blob = payload
        else:
            # Unknown FourCC: sub_408D30(v2, Offset, 1) — seek forward (~line 41105)
            unknown_chunks.append(Chunk(fourcc=tag, offset=offset, size=size))

    return SenFile(
        total_data_size=total_data_size,
        onam_buffer=onam_buffer,
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
# TANI animation playback engine
# ---------------------------------------------------------------------------

@dataclass
class TextureUVState:
    """Runtime UV offset for one MAPI-indexed texture slot.

    The engine stores UV offsets as integer scroll values applied to the
    material's UV coordinates.  Signs and scaling are renderer-dependent;
    these values mirror the counters tracked per-entry in sub_434A90.
    """
    texture_index: int   # index into the SEN MAPI table
    u_offset: int = 0
    v_offset: int = 0


@dataclass
class TaniAnimationState:
    """Runtime state for one TaniEntry during UV animation playback.

    Mirrors the mutable fields of TaniEntry as they are updated by
    sub_434A90 each game tick.  The file-resident TaniEntry is left
    unmodified; this object holds the live counters.

    Update logic (sub_434A90, one call per game tick per TANI chunk):
        tick_counter++
        if tick_counter >= tick_period:
            tick_counter = 0
            subtick_counter++
            for each texture_index in texture_indices:
                u_offset += minor_u_step
                v_offset += minor_v_step
            if subtick_counter >= v_wrap_count:
                subtick_counter = 0
                for each texture_index in texture_indices:
                    u_offset += major_u_step
                    v_offset += major_v_step
    """
    entry: TaniEntry
    tick_counter: int = 0
    subtick_counter: int = 0
    # Per-texture UV offsets, indexed by position in entry.texture_indices.
    uv_states: list[TextureUVState] = field(default_factory=list)

    def __post_init__(self) -> None:
        if not self.uv_states:
            self.uv_states = [
                TextureUVState(texture_index=idx)
                for idx in self.entry.texture_indices
            ]

    def step(self) -> None:
        """Advance one game tick.  Mirrors sub_434A90."""
        self.tick_counter += 1
        if self.tick_counter < self.entry.tick_period:
            return

        # Sub-advance fires
        self.tick_counter = 0
        self.subtick_counter += 1

        for uv in self.uv_states:
            uv.u_offset += self.entry.minor_u_step
            uv.v_offset += self.entry.minor_v_step

        if self.subtick_counter >= self.entry.v_wrap_count:
            self.subtick_counter = 0
            for uv in self.uv_states:
                uv.u_offset += self.entry.major_u_step
                uv.v_offset += self.entry.major_v_step

    def reset(self) -> None:
        """Reset to initial state (tick_counter = subtick_counter = 0, all UVs zero)."""
        self.tick_counter = 0
        self.subtick_counter = 0
        for uv in self.uv_states:
            uv.u_offset = 0
            uv.v_offset = 0

    def summary(self) -> str:
        lines = [
            f"  tick={self.tick_counter}/{self.entry.tick_period}"
            f"  sub={self.subtick_counter}/{self.entry.v_wrap_count}"
            f"  minor=({self.entry.minor_u_step},{self.entry.minor_v_step})"
            f"  major=({self.entry.major_u_step},{self.entry.major_v_step})"
        ]
        for uv in self.uv_states:
            lines.append(f"    tex[{uv.texture_index}]: u={uv.u_offset}  v={uv.v_offset}")
        return "\n".join(lines)


@dataclass
class SceneTaniState:
    """Playback state for all TANI chunks in a loaded SEN scene.

    Each TANI chunk in the SEN file becomes one list of TaniAnimationState
    objects (one per record in that chunk).

    Usage:
        sen = parse_sen(Path("scene.sen").read_bytes())
        scene_tani = make_tani_state(sen)
        for tick in range(300):          # simulate 300 game ticks
            scene_tani.step()
        scene_tani.print_summary()

    To read current UV offsets for a specific MAPI material index:
        for chunk_states in scene_tani.chunks:
            for anim in chunk_states:
                for uv in anim.uv_states:
                    if uv.texture_index == my_mat_idx:
                        print(uv.u_offset, uv.v_offset)
    """
    chunks: list[list[TaniAnimationState]]
    tick: int = 0

    def step(self) -> None:
        """Advance all TANI animations by one game tick."""
        for chunk in self.chunks:
            for anim in chunk:
                anim.step()
        self.tick += 1

    def reset(self) -> None:
        """Reset all TANI animations to their initial state."""
        self.tick = 0
        for chunk in self.chunks:
            for anim in chunk:
                anim.reset()

    def uv_offset_for(self, mapi_index: int) -> Optional[tuple[int, int]]:
        """Return the current (u, v) UV offset for a given MAPI material index.

        Returns the offset from the first TaniAnimationState that targets
        that index, or None if no TANI record references it.
        """
        for chunk in self.chunks:
            for anim in chunk:
                for uv in anim.uv_states:
                    if uv.texture_index == mapi_index:
                        return (uv.u_offset, uv.v_offset)
        return None

    def print_summary(self) -> None:
        print(f"TANI state at tick {self.tick}:")
        for ci, chunk in enumerate(self.chunks):
            print(f"  Chunk {ci} ({len(chunk)} record(s)):")
            for ai, anim in enumerate(chunk):
                print(f"    Record {ai}:")
                print(anim.summary())


def make_tani_state(sen: SenFile) -> SceneTaniState:
    """Construct a fresh SceneTaniState for all TANI chunks in a SEN file.

    Usage:
        sen = parse_sen(data)
        state = make_tani_state(sen)
        for _ in range(60):
            state.step()
        state.print_summary()
    """
    chunks = [
        [TaniAnimationState(entry=entry) for entry in chunk]
        for chunk in sen.tani_entries
    ]
    return SceneTaniState(chunks=chunks)


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

    print()
    print(f"Object instances / OBJI ({len(sen.object_instances)})")
    for i, obj in truncated(sen.object_instances, "instances"):
        name = sen.name_at_offset(obj.name_offset)
        print(
            f"  [{i:3d}] {name:<14s} type={obj.type_name:<16s} "
            f"mesh={obj.mesh_handle:3d}  "
            f"pos=({obj.x:9.1f}, {obj.y:8.1f}, {obj.z:8.1f})  "
            f"rot=({obj.rot_x:6d}, {obj.rot_y:6d}, {obj.rot_z:6d})  "
            f"mesh_entry_idx={obj.mesh_entry_idx}"
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
                lod_info = f"  lod_dist={mh.lod_max_draw_dist} vertex_count={mh.lod_vertex_count}"
            print(
                f"  [{i:3d}] MESH: {mh.raw_size} bytes  "
                f"flags=0x{mh.flags:08X}  lod_levels={mh.lod_level_count}  "
                f"num_extra_sub={mh.num_extra_sub}  "
                f"mesh_handle=0x{mh.mesh_handle:04X}{lod_info}"
            )
            if mh.sub_objects:
                for si, sub in enumerate(mh.sub_objects):
                    print(
                        f"        sub[{si}]: offset=({sub.offset_x},{sub.offset_y},{sub.offset_z})  "
                        f"mat_idx={sub.material_index}"
                    )

    print()
    print(f"TANI chunks ({len(sen.tani_entries)})")
    print(f"  (UV texture scroll animation — sub_434A90/sub_434B00)")
    print(f"  (Separate from ANM opcode-5 sub-mesh rotation — sub_430A90)")
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
    print(f"SUBO entries ({len(sen.subo_entries)}) — DEAD DATA (pointer stored, never read by engine)")
    for i, s in truncated(sen.subo_entries, "entries"):
        bpi_note = f", {s.bytes_per_instance} bytes/instance" if s.bytes_per_instance else ""
        print(f"  [{i:3d}] {len(s.raw)} bytes raw{bpi_note}")

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
                'name': sen.name_at_offset(o.name_offset),
                'object_type': o.object_type,
                'type_name': o.type_name,
                'mesh_handle': o.mesh_handle,
                'position': {'x': o.x, 'y': o.y, 'z': o.z},
                'rotation': {'x': o.rot_x, 'y': o.rot_y, 'z': o.rot_z},
                'mesh_entry_idx': o.mesh_entry_idx,
            }
            for o in sen.object_instances
        ],
        'material_map': [asdict(m) for m in sen.material_map],
        'colour_table': [f'0x{c:08X}' for c in sen.colour_table],
        'mesh_entries': [
            (
                {'type': 'name', 'name': val}
                if kind == 'name'
                else {
                    'type': 'mesh',
                    'flags': val.flags,
                    'lod_level_count': val.lod_level_count,
                    'num_extra_sub': val.num_extra_sub,
                    'sub_transform_offset': val.sub_transform_offset,
                    'sub_material_offset': val.sub_material_offset,
                    'lod_offset': val.lod_offset,
                    'mesh_handle': val.mesh_handle,
                    'lod_max_draw_dist': val.lod_max_draw_dist,
                    'lod_vertex_count': val.lod_vertex_count,
                    'sub_objects': [asdict(s) for s in val.sub_objects],
                    'raw_size': val.raw_size,
                }
            )
            for kind, val in sen.mesh_entries
        ],
        'tani_entries': [
            [
                {
                    'major_u_step': e.major_u_step,
                    'major_v_step': e.major_v_step,
                    'minor_u_step': e.minor_u_step,
                    'minor_v_step': e.minor_v_step,
                    'tick_period': e.tick_period,
                    'v_wrap_count': e.v_wrap_count,
                    'texture_indices': e.texture_indices,
                }
                for e in chunk
            ]
            for chunk in sen.tani_entries
        ],
        'subo_entries': [
            {
                'raw_size': len(s.raw),
                'num_instances': s.num_instances,
                'bytes_per_instance': s.bytes_per_instance,
                'note': 'DEAD DATA — dword_45EB30 is set but never read back',
            }
            for s in sen.subo_entries
        ],
        'keep_blob_size': len(sen.keep_blob) if sen.keep_blob else None,
        'temp_blob_size': len(sen.temp_blob) if sen.temp_blob else None,
        'unknown_chunks': [asdict(ch) for ch in sen.unknown_chunks],
    }


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(
        description="Parse and optionally simulate a game SEN scene file."
    )
    parser.add_argument("file", help="Path to the .sen file")
    parser.add_argument(
        "--json", action="store_true",
        help="Output as JSON instead of the human-readable summary",
    )
    parser.add_argument(
        "-N", "--max-lines", metavar="N", type=int, default=10,
        help="Maximum lines printed per section in the summary (default: 10; 0 = unlimited)",
    )
    parser.add_argument(
        "--tani-play", metavar="TICKS", type=int, default=0,
        help="Simulate TICKS game ticks of TANI UV animation and print the result",
    )
    parser.add_argument(
        "--out", metavar="FILE",
        help="Write output to FILE instead of stdout",
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

    if args.tani_play > 0:
        state = make_tani_state(sen)
        for _ in range(args.tani_play):
            state.step()
        import io
        buf = io.StringIO()
        old = sys.stdout
        sys.stdout = buf
        state.print_summary()
        sys.stdout = old
        output = buf.getvalue()
    elif args.json:
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
