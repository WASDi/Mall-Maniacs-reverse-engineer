"""
ANM animation file parser
Reverse engineered from decompiled game code (Visual C++, x86).

════════════════════════════════════════════════════════════════════════════════
FILE FORMAT
════════════════════════════════════════════════════════════════════════════════

  [0..2]   Magic: "ANM"
  [3]      Version: 1 or 2  (checked in sub_433A90)
   [4..5]   uint16 bone_count
            bone_count × Pascal strings  { uint8 len, char[len] }
            uint16 mesh_count
            mesh_count × { uint8 len, char[len], uint16 sub_obj_idx }
            uint16 track_count
            track_count × { uint16 key_count, keyframe[key_count] }

All integers are little-endian (sub_433EF0 = read-u16, sub_433F10 = read-u32).

════════════════════════════════════════════════════════════════════════════════
KEYFRAME OPCODES  (sub_433A90, switch at ~0x433A90+offset)
════════════════════════════════════════════════════════════════════════════════

Each keyframe starts with a u8 opcode byte, followed by a fixed payload.

  Opcode 1 – set_channel_A  (sub_434090 case 1, sub_434290 case 1)
    Payload: 3 × float32  (x, y, z)   = 12 bytes
    Sets the "channel A" position (a2[8..10]) on the animation state.
    At apply time: sub_430660(root_bone, x, -y, -z, 2)
    The Y and Z components are negated by the engine (coordinate-system flip).
    In-memory layout (after loader): [u8=1][f32 x][f32 y][f32 z]  → 13 bytes;
    but the loader packs it as 16 bytes (LABEL_24 → v4 += 16).

  Opcode 2 – set_channel_B  (sub_434090 case 2, sub_434290 case 2)
    Payload: 3 × float32  (x, y, z)   = 12 bytes
    Same structure as opcode 1 but targets "channel B" (a2[11..13]).
    Used together with opcode 1 to express a two-point aim constraint or
    a root + tip pair; channel B is applied via sub_431030 (look-at solver).

  Opcode 3 – move_named_bone  (sub_434090 case 3, sub_434290 case 3)
    Payload: u16 bone_idx + 3 × float32  (x, y, z)   = 14 bytes
    Calls sub_430660(bone_handle, x, -y, -z, 2) to set the absolute
    world-space position of the named bone.  The bone_idx is resolved to a
    scene-object pointer at load time via sub_431E20 (name→handle lookup);
    if a2[6] (the bound scene object) is also set, that overrides bone_idx.
    In-memory layout: [u8=3][ptr bone_handle 4B][f32 x][f32 y][f32 z]  20 bytes
    Blending variant (sub_434290): reads current position via sub_430E80,
    interpolates 50/50, then calls sub_430660.

  Opcode 4 – set_bone_orientation  (sub_434090 case 4, sub_434290 case 4)
    Payload: u16 bone_idx + 3 × int16  (rot_x, rot_y, rot_z)  = 8 bytes
    Calls sub_4307D0(bone_handle, rot_x, -rot_y, -rot_z, 2).
    Angles are engine fixed-point: 65536 units = 360°  (10430.378… units/radian,
    matching the atan2 constant in sub_431030).
    The bone_idx resolves the same way as opcode 3 (sub_431E20 at load time).
    In-memory layout: [u8=4][ptr bone_handle 4B][i16 rx][i16 ry][i16 rz]  13 B
    packed to 16 bytes (LABEL_24 → v4 += 16).

  Opcode 5 – set_mesh_subobj_orientation  (sub_434090 case 5, sub_434290 case 5)
    Payload in file: u16 mesh_idx + 3 × int16  (rot_x, rot_y, rot_z)  = 8 bytes total
    (including opcode byte = 8; no padding — v4 += 8 at LABEL_21).

    This opcode drives the rotation of a named *sub-object* (mesh bone) within
    a scene object.  The three int16 values are compact Euler rotation angles
    in the same fixed-point encoding as opcode 4 (65536 units = 360°).

    Load-time resolution (sub_433A90, case 5 ~line 42399):
      mesh_idx indexes into the ANM mesh table built during loading.
      Two parallel arrays are allocated (mesh_count DWORDs each):
        v60[i] = sub_431E20(mesh_name)   ← scene-object pointer (name→handle lookup)
        v61[i] = sub_obj_idx_uint16      ← MeshEntry.sub_obj_idx from ANM file header
      The in-memory keyframe byte at +1 is set to:
        v25[1] = ((byte*)v61)[4 * mesh_idx]   ← LOW BYTE of v61[mesh_idx] = sub_obj_idx
      Both temporary arrays are freed after packing (sub_419A60 calls).

      CRITICAL: The second field of each ANM MeshEntry is the sub-object index
      (0-based slot into the scene object's 112-byte transform block array),
      NOT a keyframe or frame count.  The engine stores it as uint16 but only
      uses the low byte.

    In-memory keyframe layout (8 bytes, no opcode-byte padding):
      [u8=5][u8 sub_obj_idx][i16 rot_x][i16 rot_y][i16 rot_z]
      sub_obj_idx: the sub-object slot index within the bound scene object
                   (= MeshEntry.sub_obj_idx for the ANM mesh entry at mesh_idx).

    Apply-time (sub_434090 case 5):
      v11 = a2[6]   ← the scene object this animation is bound to (set in sub_433A90)
      sub_430A90(v11, sub_obj_idx, rot_x, rot_y, rot_z, flags=2)
      sub_430A90 mode 2 (absolute set) writes into the per-sub-object transform block:
        base_ptr = *(scene_obj + 20)            ← sub-object block array pointer
        block    = base_ptr + 112 * sub_obj_idx ← 112-byte block for this sub-object
        block[+0]  = rot_x  (int16)
        block[+2]  = rot_y  (int16)
        block[+4]  = rot_z  (int16)
        block[+10] = 0      (dirty flag cleared)
        block[+11] = 0      (dirty flag cleared)
      Y/Z negation does NOT happen for opcode 5 (unlike opcodes 1–4).

    The 112-byte sub-object transform block layout:
        [+0]   int16  rot_x        (Euler angle, engine units)
        [+2]   int16  rot_y
        [+4]   int16  rot_z
        [+6]   float  scale        (initialised to 1.0)
        [+10]  byte   dirty_flag_0 (cleared to 0 on absolute set)
        [+11]  byte   dirty_flag_1 (cleared to 0 on absolute set; set to 1 on blend)
        [+12]  int32  mat_idx      (material index from SEN MESH sub_material table)
        [+16]  int32  offset_x     (sign-extended from int16 in SEN sub_transform table)
        [+20]  int32  offset_y
        [+24]  int32  offset_z
        [+28..+111] renderer-specific data (matrix, bounding sphere, etc.)

    The sub_obj_idx == 0 refers to the ROOT sub-object (always present).
    Sub-objects 1..N refer to extra sub-objects from the SEN MESH header's
    sub_transform and sub_material tables (N = num_extra_sub).

    Blending variant (sub_434290 case 5):
      Calls sub_431110(scene_obj, sub_obj_idx, rot_x, rot_y, rot_z):
        Builds a full 3×3 rotation matrix from the three angles.
        Blends 50/50 with the current matrix already in the transform block.
        Sets dirty flag block[+10] = 1.
      Used when seeking to a non-sequential frame (interpolation path).

  Opcode 6 – move_implicit_bone  (sub_434090 case 6, sub_434290 case 6)
    Payload: u16 (discarded / unused index) + 3 × float32  (x, y, z)  = 14 bytes
    The u16 field is read and thrown away (sub_433EF0 called, result ignored).
    Calls sub_430660(a2[6], x, -y, -z, 2) — moves the *bound* scene object
    itself (a2[6]) rather than a named bone.
    In-memory layout: [u8=6][f32 x][f32 y][f32 z]  → 13 bytes, padded to 16
    (shared LABEL_23 → v4 += 16).

════════════════════════════════════════════════════════════════════════════════
ANIMATION STATE OBJECT  (allocated by sub_433F40, sub_419870)
════════════════════════════════════════════════════════════════════════════════

The loader returns a pointer to an animation-state block (v56) with this layout
(all DWORDs unless noted):

  [+0]   uint32  current_frame       (initialised to 0)
  [+1]   uint32  total_frames        (= track_count; set at v56[1] = v20)
  [+2]   ptr     keyframe_buf_start  (= v58 initially; used for loop rewind)
  [+3]   ptr     current_kf_ptr      (advances by 8 per frame)
  [+4]   uint32  (reserved / 0)
  [+5]   ptr     pool_handle         (= dword_45EBC8, the "Anim" pool)
  [+6]   ptr     bound_scene_object  (a3 passed to sub_433A50)
  [+7]   ptr     root_bone_handle    (a2 passed to sub_433A50; may be 0)
  [+8]   float   channel_A_x
  [+9]   float   channel_A_y        (negated)
  [+10]  float   channel_A_z        (negated)
  [+11]  float   channel_B_x
  [+12]  float   channel_B_y        (negated)
  [+13]  float   channel_B_z        (negated)

  current_kf_ptr advances by 8 bytes per frame (v4[3] += 8 in sub_434090)
  after all keyframes in that frame have been applied.  Looping resets
  current_kf_ptr to keyframe_buf_start and current_frame to 0.

════════════════════════════════════════════════════════════════════════════════
COORDINATE SYSTEM
════════════════════════════════════════════════════════════════════════════════

Opcodes 1, 2, 3, 6  negate Y and Z on apply (engine is Y-up, file is Z-up or
vice versa).  Opcode 5 (sub-mesh rotation) does NOT negate any axis.
Angle units: int16, 65536 = 360°  (= 10430.378… per radian).

════════════════════════════════════════════════════════════════════════════════
TWO ANIMATION SYSTEMS — DO NOT CONFUSE
════════════════════════════════════════════════════════════════════════════════

  ANM opcode 5   = rotates named sub-object mesh bones to discrete orientations.
  SEN TANI chunk = scrolls UV texture coordinates on static mesh materials
                   (sub_434B00 / sub_434A90).  Completely separate system.
"""

import math
import struct
import sys
import json
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Optional


# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

# Engine angle encoding: int16 units per full revolution.
# Derived from the atan2 constant 10430.37834910853 in sub_431030.
ANGLE_UNITS_PER_REV: int = 65536
ANGLE_UNITS_PER_RAD: float = 10430.37834910853


def angle_to_deg(raw: int) -> float:
    """Convert an engine int16 angle to degrees.  Range: (-180, +180]."""
    return raw * 360.0 / ANGLE_UNITS_PER_REV


def angle_to_rad(raw: int) -> float:
    """Convert an engine int16 angle to radians."""
    return raw / ANGLE_UNITS_PER_RAD


def deg_to_angle(degrees: float) -> int:
    """Convert degrees to engine int16 angle units."""
    return round(degrees * ANGLE_UNITS_PER_REV / 360.0) & 0xFFFF


def rad_to_angle(radians: float) -> int:
    """Convert radians to engine int16 angle units."""
    return round(radians * ANGLE_UNITS_PER_RAD) & 0xFFFF


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class MeshEntry:
    """One entry from the ANM mesh table.

    'name' is the scene-object name (uppercased at load time via _strupr,
    then resolved to a handle by sub_431E20 / sub_433A90 in the engine).

    'sub_obj_idx' is the sub-object slot index within the bound scene object.
    Despite being stored as a uint16 field in the ANM file, only the low byte
    is used at load time:

        v61 array: each entry DWORD gets `*v15 = keyframe_count_uint16` written
                   by the mesh-table parsing loop (sub_433A90 line 42328).
        At pack time (case 5): v25[1] = v50[4 * mesh_idx]  where v50 = (byte*)v61.
                   This reads the LOW BYTE of v61[mesh_idx], which is the
                   uint16 keyframe_count value narrowed to uint8.

    So what the file calls the "keyframe_count" is really the sub-object index
    (0-based slot into the scene object's sub-object transform block array)
    that opcode-5 keyframes referencing this mesh entry will target.

    In typical ANM files, all mesh entries share the same scene-object name
    (e.g. "roland") with consecutive sub_obj_idx values (0, 1, 2, ...), one
    entry per animated sub-object on that character mesh.

    Also used in sub_433F40 (buffer-size pass): case 5 adds 8 bytes per
    opcode-5 keyframe found during the pre-scan, but this is driven by the
    ANM track data, not this field.
    """
    name: str
    sub_obj_idx: int   # sub-object slot index within the bound scene object (low byte of stored uint16)


@dataclass
class Keyframe:
    """One decoded keyframe from an ANM track.

    Opcode summary (see module docstring for full details):
      1  set_channel_A            – float xyz sets channel-A position
      2  set_channel_B            – float xyz sets channel-B position (aim target)
      3  move_named_bone          – float xyz moves a named bone (sub_430660)
      4  set_bone_orientation     – int16 rot_xyz sets bone Euler angles
      5  set_mesh_subobj_orient   – int16 rot_xyz sets sub-object Euler angles
                                    via sub_430A90 (NO axis negation)
                                    target_index = mesh table index (from file)
                                    target_name  = mesh entry name (scene object)
                                    The actual sub-object slot = MeshEntry.sub_obj_idx
      6  move_implicit_bone       – float xyz moves the bound scene object itself

    For opcode 5 the uvw fields hold raw Euler angles (engine int16 units,
    65536 = 360°).  Use angle_to_deg() / angle_to_rad() for conversion.
    """
    opcode: int

    # Opcodes 3, 4, 5: resolved name and original index
    target_name: Optional[str] = None
    target_index: Optional[int] = None

    # Float payload: opcodes 1, 2, 3, 6  (engine coords — Y/Z negated at apply)
    x: Optional[float] = None
    y: Optional[float] = None
    z: Optional[float] = None

    # Int16 payload: opcodes 4, 5
    # Opcode 4: bone Euler angles in engine units (65536 = 360°)
    # Opcode 5: sub-mesh Euler angles in engine units (65536 = 360°);
    #           stored as rot_x/rot_y/rot_z — no axis negation on apply.
    u: Optional[int] = None   # rot_x  (engine int16 angle units)
    v: Optional[int] = None   # rot_y
    w: Optional[int] = None   # rot_z

    @property
    def opcode_name(self) -> str:
        return {
            1: "set_channel_A",
            2: "set_channel_B",
            3: "move_named_bone",
            4: "set_bone_orientation",
            5: "set_mesh_subobj_orient",
            6: "move_implicit_bone",
        }.get(self.opcode, f"unknown_{self.opcode}")

    # ------------------------------------------------------------------ #
    # Convenience helpers for opcode-5 rotation angles                    #
    # ------------------------------------------------------------------ #

    def rot_deg(self) -> Optional[tuple[float, float, float]]:
        """Return (rot_x, rot_y, rot_z) in degrees for opcodes 4 and 5.

        Returns None if this keyframe has no int16 rotation payload.
        """
        if self.u is None:
            return None
        return angle_to_deg(self.u), angle_to_deg(self.v), angle_to_deg(self.w)

    def rot_matrix(self) -> Optional[list[list[float]]]:
        """Return the 3×3 rotation matrix for opcodes 4 and 5.

        Mirrors the matrix construction in sub_431110 (the blending path
        called by sub_434290 for opcode 5):

            R = Rz(rot_z) · Rx(rot_x) · Ry(rot_y)   (engine convention)

        The matrix rows are [right, up, forward] in engine space.
        Returns None if this keyframe carries no rotation payload.
        """
        if self.u is None:
            return None
        rx = angle_to_rad(self.u)
        ry = angle_to_rad(self.v)
        rz = angle_to_rad(self.w)

        cx, sx = math.cos(rx), math.sin(rx)
        cy, sy = math.cos(ry), math.sin(ry)
        cz, sz = math.cos(rz), math.sin(rz)

        # sub_431110 builds v18[0..8] as:
        #   v17 = cz * cy   v16 = sz * cx   v15 = sz * cy   v23 = cz * cx
        #   [0] = v17*cx + v16          (row 0, col 0)
        #   [1] = v15*cx - v23          (row 0, col 1)
        #   [2] = cy * sx               (row 0, col 2)
        #   [3] = cz * sx               (row 1, col 0)
        #   [4] = sz * sx               (row 1, col 1)
        #   [5] = -cx                   (row 1, col 2)
        #   [6] = v23*cx - v15          (row 2, col 0)
        #   [7] = v16*cx + v17          (row 2, col 1)   ← note: same as [0]
        #   [8] = cx * sx               (row 2, col 2)
        v17 = cz * cy
        v16 = sz * cx
        v15 = sz * cy
        v23 = cz * cx
        return [
            [v17 * cx + v16,  v15 * cx - v23,  cy * sx],
            [cz * sx,         sz * sx,          -cx    ],
            [v23 * cx - v15,  v16 * cx + v17,   cx * sx],
        ]


@dataclass
class Track:
    """One animation track: an ordered sequence of keyframes.

    The engine plays tracks sequentially, one keyframe bundle per frame
    (sub_434090 / sub_434290).  'current_kf_ptr' advances by 8 bytes per
    game frame, covering every keyframe in the track once per cycle.
    """
    keyframes: list[Keyframe] = field(default_factory=list)


@dataclass
class AnmFile:
    version: int
    bone_names: list[str]
    meshes: list[MeshEntry]
    tracks: list[Track]


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
                f"Unexpected end of file: need {n} bytes at offset {self.pos:#x}, "
                f"only {self.remaining()} available"
            )
        chunk = self._data[self.pos : self.pos + n]
        self.pos += n
        return chunk

    def u8(self) -> int:
        return struct.unpack_from("<B", self.read(1))[0]

    def u16(self) -> int:
        # Mirrors sub_433EF0: return *a1 + (a1[1] << 8)
        return struct.unpack_from("<H", self.read(2))[0]

    def i16(self) -> int:
        return struct.unpack_from("<h", self.read(2))[0]

    def f32(self) -> float:
        # Mirrors sub_433F10 (four-byte LE integer reinterpreted as float)
        return struct.unpack_from("<f", self.read(4))[0]

    def pascal_string(self) -> str:
        length = self.u8()
        return self.read(length).decode("latin-1")


# ---------------------------------------------------------------------------
# Parser
# ---------------------------------------------------------------------------

def parse_anm(data: bytes) -> AnmFile:
    """Parse an ANM file.  Mirrors sub_433A90 in the decompiled binary."""
    r = Reader(data)

    # --- Magic + version  (sub_433A90: memcmp(a1, "ANM", 3), then a1[3]) ---
    magic = r.read(3)
    if magic != b"ANM":
        raise ValueError(f"Not an ANM file (magic={magic!r})")

    version = r.u8()
    if version not in (1, 2):
        raise ValueError(f"Unsupported ANM version {version} (expected 1 or 2)")

    # --- Bone names (sub_433A90: v6 = sub_433EF0(a1+4), loop v5 Pascal strs) ---
    bone_count = r.u16()
    bone_names: list[str] = [r.pascal_string() for _ in range(bone_count)]

    # --- Mesh entries (sub_433A90: v11 = sub_433EF0(v5), then loop) ---
    # Each entry: Pascal string name + uint16 sub_obj_idx.
    # sub_obj_idx is the sub-object slot index within the named scene object.
    # At pack time the low byte of this value becomes the in-memory opcode-5
    # keyframe byte at [+1] (sub_433A90 line 42413: v25[1] = v50[4 * v46]).
    mesh_count = r.u16()
    meshes: list[MeshEntry] = []
    for _ in range(mesh_count):
        name = r.pascal_string()
        sub_obj_idx = r.u16()
        meshes.append(MeshEntry(name=name, sub_obj_idx=sub_obj_idx))

    # --- Tracks (sub_433A90: v20 = sub_433EF0(v12), then outer/inner loops) ---
    track_count = r.u16()
    tracks: list[Track] = []

    for _ in range(track_count):
        key_count = r.u16()
        keyframes: list[Keyframe] = []

        for _ in range(key_count):
            opcode = r.u8()

            if opcode == 1:
                # set_channel_A — sub_433A90 case 1/2 (shared with opcode 2):
                #   sub_433F10 × 3 → LABEL_23 → v4 += 16 (13 bytes data, 3 pad)
                # Apply: a2[8] = x, a2[9] = -y, a2[10] = -z  (sub_434090 case 1)
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            elif opcode == 2:
                # set_channel_B — identical layout to opcode 1.
                # Apply: a2[11] = x, a2[12] = -y, a2[13] = -z  (sub_434090 case 2)
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            elif opcode == 3:
                # move_named_bone — sub_433A90 case 3:
                #   v31 = sub_433EF0(v21)  → bone_idx (u16)
                #   then 3 × sub_433F10   → xyz floats
                #   *(v25+1) = v59[bone_idx]  ← resolved bone handle (4 bytes)
                # Apply (sub_434090 case 3): sub_430660(bone_handle, x, -y, -z, 2)
                bone_idx = r.u16()
                bone_name = bone_names[bone_idx] if bone_idx < len(bone_names) else None
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(
                    opcode=opcode,
                    target_index=bone_idx,
                    target_name=bone_name,
                    x=x, y=y, z=z,
                )

            elif opcode == 4:
                # set_bone_orientation — sub_433A90 case 4:
                #   v37 = sub_433EF0(v21)  → bone_idx (u16)
                #   3 × sub_433EF0         → rot_x, rot_y, rot_z (int16 angles)
                #   *(v25+1) = v59[bone_idx]  ← resolved bone handle
                # Apply (sub_434090 case 4): sub_4307D0(bone, rx, -ry, -rz, 2)
                # Angles: int16, 65536 units = 360°
                bone_idx = r.u16()
                bone_name = bone_names[bone_idx] if bone_idx < len(bone_names) else None
                u, v, w = r.i16(), r.i16(), r.i16()
                kf = Keyframe(
                    opcode=opcode,
                    target_index=bone_idx,
                    target_name=bone_name,
                    u=u, v=v, w=w,
                )

            elif opcode == 5:
                # set_mesh_subobj_orient — sub_433A90 case 5:
                #   v44 = sub_433EF0(v21)     → mesh_idx (u16)
                #   3 × sub_433EF0            → rot_x, rot_y, rot_z (int16 angles)
                #   v25[1] = v61[4*mesh_idx]  ← sub-obj index (byte 0 of handle)
                #
                # Apply (sub_434090 case 5):
                #   sub_430A90(scene_obj, sub_obj_idx, rot_x, rot_y, rot_z, 2)
                #   → writes int16 Euler angles into transform block at:
                #       *(scene_obj+20) + 112*sub_obj_idx + [0..5]
                #   → clears dirty flags at [+10], [+11]
                #   NO axis negation (unlike opcodes 1–4).
                #
                # Blending path (sub_434290 case 5):
                #   sub_431110(bone_ptr, sub_obj_idx, rot_x, rot_y, rot_z)
                #   → builds full 3×3 rotation matrix, blends 50/50 with current.
                #   → sets dirty flag [+10] = 1.
                mesh_idx = r.u16()
                mesh_name = meshes[mesh_idx].name if mesh_idx < len(meshes) else None
                u, v, w = r.i16(), r.i16(), r.i16()
                kf = Keyframe(
                    opcode=opcode,
                    target_index=mesh_idx,
                    target_name=mesh_name,
                    u=u, v=v, w=w,
                )

            elif opcode == 6:
                # move_implicit_bone — sub_433A90 case 6:
                #   sub_433EF0(v21) result discarded (unused index field)
                #   3 × sub_433F10  → xyz floats
                # Apply (sub_434090 case 6):
                #   sub_430660(a2[6], x, -y, -z, 2)  — moves the bound object
                r.u16()   # discard — unused index field (sub_433EF0, result ignored)
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            else:
                # Unknown opcode — payload size is indeterminate; bail out.
                print(
                    f"Warning: unknown opcode {opcode} at offset {r.pos - 1:#x}; "
                    "stopping track parse early.",
                    file=sys.stderr,
                )
                break

            keyframes.append(kf)

        tracks.append(Track(keyframes=keyframes))

    return AnmFile(
        version=version,
        bone_names=bone_names,
        meshes=meshes,
        tracks=tracks,
    )


# ---------------------------------------------------------------------------
# Animation playback engine
# ---------------------------------------------------------------------------

@dataclass
class BoneState:
    """Runtime state for one bone / sub-object slot.

    Mirrors the relevant fields of the per-bone transform block at:
        *(scene_obj + 20) + 112 * bone_index

    Positions are in engine world-space (Y/Z already negated by apply logic).
    Angles are in engine int16 units (65536 = 360°).
    """
    name: str
    pos_x: float = 0.0
    pos_y: float = 0.0
    pos_z: float = 0.0
    rot_x: int = 0    # engine int16 angle units
    rot_y: int = 0
    rot_z: int = 0
    matrix_dirty: bool = False   # mirrors byte at [+10] in the transform block

    def rot_deg(self) -> tuple[float, float, float]:
        """Return current rotation as (rx, ry, rz) in degrees."""
        return angle_to_deg(self.rot_x), angle_to_deg(self.rot_y), angle_to_deg(self.rot_z)


@dataclass
class AnimationState:
    """Python equivalent of the animation-state block returned by sub_433A90.

    Layout of the engine block (all DWORDs):
      [+0]  current_frame
      [+1]  total_frames   (= track_count)
      [+2]  ptr keyframe_buf_start  (loop rewind point)
      [+3]  ptr current_kf_ptr      (advances 8 bytes / frame)
      [+4]  (reserved)
      [+5]  ptr pool_handle         (= dword_45EBC8)
      [+6]  ptr bound_scene_object  (a3 to sub_433A50)
      [+7]  ptr root_bone_handle    (a2 to sub_433A50; may be 0)
      [+8..10]  channel_A xyz (floats)
      [+11..13] channel_B xyz (floats)
    """
    anm: AnmFile
    current_frame: int = 0
    looping: bool = True

    # Channel A / B state — written by opcodes 1 and 2.
    # Channel A = position target (applied via sub_430660 to root bone).
    # Channel B = aim target (applied via sub_431030 look-at solver).
    channel_A: tuple[float, float, float] = field(default_factory=lambda: (0.0, 0.0, 0.0))
    channel_B: tuple[float, float, float] = field(default_factory=lambda: (0.0, 0.0, 0.0))

    # Named bone states, keyed by uppercased bone name.
    # Populated on first write; call reset() to re-initialise.
    bones: dict[str, BoneState] = field(default_factory=dict)

    # Named mesh sub-object states, keyed by uppercased mesh name.
    meshes: dict[str, BoneState] = field(default_factory=dict)

    def _get_bone(self, name: str) -> BoneState:
        key = name.upper()
        if key not in self.bones:
            self.bones[key] = BoneState(name=key)
        return self.bones[key]

    def _get_mesh(self, name: str) -> BoneState:
        key = name.upper()
        if key not in self.meshes:
            self.meshes[key] = BoneState(name=key)
        return self.meshes[key]

    @property
    def total_frames(self) -> int:
        """Total keyframe count across all tracks (= v56[1] in engine block)."""
        if not self.anm.tracks:
            return 0
        return max(len(t.keyframes) for t in self.anm.tracks)

    def is_finished(self) -> bool:
        return not self.looping and self.current_frame >= self.total_frames

    def reset(self) -> None:
        """Rewind to frame 0.  Mirrors sub_434270 / the loop-rewind in sub_434090."""
        self.current_frame = 0
        self.channel_A = (0.0, 0.0, 0.0)
        self.channel_B = (0.0, 0.0, 0.0)
        self.bones.clear()
        self.meshes.clear()

    # ------------------------------------------------------------------ #
    # Opcode apply helpers                                                 #
    # These mirror the sub_434090 case bodies exactly.                    #
    # ------------------------------------------------------------------ #

    def _apply_op1(self, kf: Keyframe) -> None:
        """set_channel_A — sub_434090 case 1.
        a2[8] = x,  a2[9] = -y,  a2[10] = -z
        """
        self.channel_A = (kf.x, -kf.y, -kf.z)

    def _apply_op2(self, kf: Keyframe) -> None:
        """set_channel_B — sub_434090 case 2.
        a2[11] = x,  a2[12] = -y,  a2[13] = -z
        """
        self.channel_B = (kf.x, -kf.y, -kf.z)

    def _apply_op3(self, kf: Keyframe) -> None:
        """move_named_bone — sub_434090 case 3.
        sub_430660(bone_handle, x, -y, -z, 2)
        Uses a2[6] (bound scene object) if set, otherwise kf.target_name.
        """
        if kf.target_name is None:
            return
        bone = self._get_bone(kf.target_name)
        bone.pos_x = kf.x
        bone.pos_y = -kf.y   # engine negates Y
        bone.pos_z = -kf.z   # engine negates Z

    def _apply_op4(self, kf: Keyframe) -> None:
        """set_bone_orientation — sub_434090 case 4.
        sub_4307D0(bone_handle, rot_x, -rot_y, -rot_z, 2)
        """
        if kf.target_name is None:
            return
        bone = self._get_bone(kf.target_name)
        bone.rot_x = kf.u
        bone.rot_y = -kf.v   # engine negates rot_y
        bone.rot_z = -kf.w   # engine negates rot_z
        bone.matrix_dirty = False

    def _apply_op5(self, kf: Keyframe) -> None:
        """set_mesh_subobj_orient — sub_434090 case 5.

        Calls sub_430A90(scene_obj, sub_obj_idx, rot_x, rot_y, rot_z, flags=2).
        sub_430A90 mode 2 (absolute set):
            base = *(scene_obj+20) + 112 * sub_obj_idx
            base[+0] = rot_x  (int16)
            base[+2] = rot_y  (int16)
            base[+4] = rot_z  (int16)
            base[+10] = base[+11] = 0   (dirty flags cleared)

        NO axis negation — unlike opcodes 1–4, the engine does not negate
        any component of the rotation for opcode 5 (sub_434090 case 5 vs
        cases 3/4 which explicitly negate with unary minus).
        """
        if kf.target_name is None:
            return
        mesh = self._get_mesh(kf.target_name)
        mesh.rot_x = kf.u   # no negation
        mesh.rot_y = kf.v
        mesh.rot_z = kf.w
        mesh.matrix_dirty = False

    def _apply_op6(self, kf: Keyframe) -> None:
        """move_implicit_bone — sub_434090 case 6.
        sub_430660(a2[6], x, -y, -z, 2)  — moves the bound scene object itself.
        Stored here as a special bone named '__implicit__'.
        """
        bone = self._get_bone("__implicit__")
        bone.pos_x = kf.x
        bone.pos_y = -kf.y
        bone.pos_z = -kf.z

    # ------------------------------------------------------------------ #
    # Frame advance                                                        #
    # ------------------------------------------------------------------ #

    _APPLY = {
        1: _apply_op1,
        2: _apply_op2,
        3: _apply_op3,
        4: _apply_op4,
        5: _apply_op5,
        6: _apply_op6,
    }

    def step(self) -> bool:
        """Advance one frame and apply all keyframes for that frame.

        Mirrors sub_434090 in the engine (the standard per-track advance):
          • Iterates all keyframes in the current track entry.
          • Increments current_frame.
          • If looping=True and current_frame >= total_frames, rewinds to 0.

        Returns True when the animation has finished (non-looping only).
        """
        if self.is_finished():
            return True

        frame = self.current_frame

        for track in self.anm.tracks:
            if frame < len(track.keyframes):
                kf = track.keyframes[frame]
                fn = self._APPLY.get(kf.opcode)
                if fn is not None:
                    fn(self, kf)

        self.current_frame += 1

        if self.looping and self.current_frame >= self.total_frames:
            # Mirrors: *a2 = 0; *(a2+3) = *(a2+2)  (sub_434090, loop branch)
            self.current_frame = 0

        return self.is_finished()

    def play_all(self) -> list["AnimationState"]:
        """Play every frame and return a snapshot list (non-destructive).

        Useful for offline baking / export.  Does not modify self.
        """
        import copy
        tmp = copy.deepcopy(self)
        tmp.reset()
        tmp.looping = False
        snapshots = []
        while not tmp.is_finished():
            tmp.step()
            snapshots.append(copy.deepcopy(tmp))
        return snapshots

    def seek(self, target_frame: int) -> None:
        """Jump to an arbitrary frame, blending from frame 0.

        Mirrors sub_434290 (the blending/seek variant): resets state then
        applies all frames up to target_frame using the blending path.

        The true engine blending (50/50 matrix interpolation via sub_431110)
        is approximated here by simply replaying from the start, which
        produces the correct result for the absolute-set modes used in
        practice.  Intermediate frames are applied but not exposed.
        """
        self.reset()
        target_frame = max(0, min(target_frame, self.total_frames - 1))
        self.looping = False
        for _ in range(target_frame + 1):
            if self.is_finished():
                break
            self.step()
        # Leave current_frame pointing just past target so the caller can
        # continue stepping forward naturally.

    def summary(self) -> str:
        """Human-readable snapshot of current animation state."""
        lines = [
            f"Frame {self.current_frame}/{self.total_frames}  "
            f"({'looping' if self.looping else 'one-shot'})",
            f"  Channel A: ({self.channel_A[0]:.4f}, {self.channel_A[1]:.4f}, {self.channel_A[2]:.4f})",
            f"  Channel B: ({self.channel_B[0]:.4f}, {self.channel_B[1]:.4f}, {self.channel_B[2]:.4f})",
        ]
        if self.bones:
            lines.append("  Bones:")
            for name, b in sorted(self.bones.items()):
                rx, ry, rz = b.rot_deg()
                lines.append(
                    f"    {name:<20s}  pos=({b.pos_x:8.2f}, {b.pos_y:8.2f}, {b.pos_z:8.2f})"
                    f"  rot=({rx:7.2f}°, {ry:7.2f}°, {rz:7.2f}°)"
                )
        if self.meshes:
            lines.append("  Mesh sub-objects (opcode 5):")
            for name, m in sorted(self.meshes.items()):
                rx, ry, rz = m.rot_deg()
                lines.append(
                    f"    {name:<20s}  rot=({rx:7.2f}°, {ry:7.2f}°, {rz:7.2f}°)"
                    f"  dirty={m.matrix_dirty}"
                )
        return "\n".join(lines)


def make_animation_state(anm: AnmFile, looping: bool = True) -> AnimationState:
    """Construct a fresh AnimationState for playback.

    Usage:
        anm = parse_anm(Path("anim/s_run.anm").read_bytes())
        state = make_animation_state(anm, looping=True)
        for _ in range(60):          # simulate 60 game ticks
            state.step()
            print(state.summary())

    For mesh sub-object animations:
        state.step()
        for name, mesh in state.meshes.items():
            rx, ry, rz = mesh.rot_deg()
            mat = ...  # apply rot_matrix via Keyframe.rot_matrix() if needed
    """
    return AnimationState(anm=anm, looping=looping)


# ---------------------------------------------------------------------------
# Pretty printer
# ---------------------------------------------------------------------------

def print_summary(anm: AnmFile) -> None:
    print(f"ANM version : {anm.version}")
    print(f"Bones       : {len(anm.bone_names)}")
    for i, name in enumerate(anm.bone_names):
        print(f"  [{i:3d}] {name}")

    print(f"Meshes      : {len(anm.meshes)}")
    for i, m in enumerate(anm.meshes):
        # sub_obj_idx = sub-object slot index within the named scene object
        print(f"  [{i:3d}] {m.name}  sub_obj_idx={m.sub_obj_idx}")

    print(f"Tracks      : {len(anm.tracks)}")
    for t_idx, track in enumerate(anm.tracks):
        print(f"  Track {t_idx}: {len(track.keyframes)} keyframes")
        for k_idx, kf in enumerate(track.keyframes):
            parts = [f"op={kf.opcode_name}"]
            if kf.target_name is not None:
                parts.append(f"target={kf.target_name!r}[{kf.target_index}]")
            if kf.x is not None:
                parts.append(f"xyz=({kf.x:.4f}, {kf.y:.4f}, {kf.z:.4f})")
            if kf.u is not None:
                rx, ry, rz = kf.rot_deg()
                parts.append(
                    f"rot=({kf.u}, {kf.v}, {kf.w})"
                    f" = ({rx:.1f}°, {ry:.1f}°, {rz:.1f}°)"
                )
            print(f"    [{k_idx:4d}] {', '.join(parts)}")


def to_dict(anm: AnmFile) -> dict:
    return {
        "version": anm.version,
        "bone_names": anm.bone_names,
        "meshes": [
            {"name": m.name, "sub_obj_idx": m.sub_obj_idx}
            for m in anm.meshes
        ],
        "tracks": [
            {
                "keyframes": [
                    {
                        "opcode": kf.opcode,
                        "opcode_name": kf.opcode_name,
                        **({} if kf.target_name is None else {
                            "target_name": kf.target_name,
                            "target_index": kf.target_index,
                        }),
                        **({} if kf.x is None else {
                            "x": kf.x, "y": kf.y, "z": kf.z,
                        }),
                        **({} if kf.u is None else {
                            "rot_x_raw": kf.u, "rot_y_raw": kf.v, "rot_z_raw": kf.w,
                            "rot_x_deg": angle_to_deg(kf.u),
                            "rot_y_deg": angle_to_deg(kf.v),
                            "rot_z_deg": angle_to_deg(kf.w),
                        }),
                    }
                    for kf in track.keyframes
                ]
            }
            for track in anm.tracks
        ],
    }


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(
        description="Parse and optionally play back a game ANM animation file."
    )
    parser.add_argument("file", help="Path to the .anm file")
    parser.add_argument(
        "--json", action="store_true",
        help="Output file structure as JSON instead of the human-readable summary",
    )
    parser.add_argument(
        "--play", metavar="N", type=int, default=0,
        help="Simulate N game ticks of playback and print the animation state each tick",
    )
    parser.add_argument(
        "--seek", metavar="FRAME", type=int, default=None,
        help="Seek to FRAME and print the resulting animation state",
    )
    parser.add_argument(
        "--loop", action="store_true", default=True,
        help="Loop animation during --play (default: on)",
    )
    parser.add_argument(
        "--no-loop", action="store_false", dest="loop",
        help="Disable looping during --play",
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
        anm = parse_anm(data)
    except ValueError as exc:
        print(f"Parse error: {exc}", file=sys.stderr)
        sys.exit(1)

    if args.seek is not None:
        state = make_animation_state(anm, looping=False)
        state.seek(args.seek)
        output = state.summary()
    elif args.play > 0:
        state = make_animation_state(anm, looping=args.loop)
        lines = []
        for tick in range(args.play):
            done = state.step()
            lines.append(f"=== Tick {tick + 1} ===")
            lines.append(state.summary())
            if done:
                lines.append("(animation finished)")
                break
        output = "\n".join(lines)
    elif args.json:
        output = json.dumps(to_dict(anm), indent=2)
    else:
        import io
        buf = io.StringIO()
        old_stdout = sys.stdout
        sys.stdout = buf
        print_summary(anm)
        sys.stdout = old_stdout
        output = buf.getvalue()

    if args.out:
        Path(args.out).write_text(output, encoding="utf-8")
        print(f"Written to {args.out}")
    else:
        print(output, end="")


if __name__ == "__main__":
    main()
