"""
ANM animation file parser
Reverse engineered from decompiled game code.

File layout:
  [0..2]  Magic: "ANM"
  [3]     Version: 1 or 2
  [4..5]  uint16 bone_count
  [6..]   bone_count x Pascal strings  { uint8 len, char[len] }
          uint16 mesh_count
          mesh_count x { uint8 len, char[len], uint16 frame_count }
          uint16 track_count
          track_count x { uint16 key_count, keyframe[key_count] }

Keyframe opcodes:
  1  set transform channel A  : 12 bytes  (3 x float32 x,y,z)
  2  set transform channel B  : 12 bytes  (3 x float32 x,y,z)
  3  move named bone by index : 2 bytes bone_idx + 12 bytes (3 x float32)
  4  sprite/UV frame on bone  : 2 bytes bone_idx +  6 bytes (3 x int16)
  5  mesh animation frame     : 2 bytes mesh_idx +  6 bytes (3 x int16)
  6  move implicit bone       : 14 bytes (skip 2, then 3 x float32)

All integers are little-endian.
Y and Z are negated by the engine at runtime (coordinate system flip).
"""

import struct
import sys
import json
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Optional


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class MeshEntry:
    name: str
    frame_count: int


@dataclass
class Keyframe:
    opcode: int
    # For opcodes 3, 4, 5 – resolved name (bone or mesh)
    target_name: Optional[str] = None
    target_index: Optional[int] = None
    # Float payload (opcodes 1, 2, 3, 6)
    x: Optional[float] = None
    y: Optional[float] = None
    z: Optional[float] = None
    # Int16 payload (opcodes 4, 5)
    u: Optional[int] = None
    v: Optional[int] = None
    w: Optional[int] = None

    @property
    def opcode_name(self) -> str:
        return {
            1: "set_pos_A",
            2: "set_pos_B",
            3: "move_bone",
            4: "sprite_frame",
            5: "mesh_frame",
            6: "move_implicit_bone",
        }.get(self.opcode, f"unknown_{self.opcode}")


@dataclass
class Track:
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
        return struct.unpack_from("<H", self.read(2))[0]

    def i16(self) -> int:
        return struct.unpack_from("<h", self.read(2))[0]

    def f32(self) -> float:
        return struct.unpack_from("<f", self.read(4))[0]

    def pascal_string(self) -> str:
        length = self.u8()
        return self.read(length).decode("latin-1")


# ---------------------------------------------------------------------------
# Parser
# ---------------------------------------------------------------------------

def parse_anm(data: bytes) -> AnmFile:
    r = Reader(data)

    # --- Magic ---
    magic = r.read(3)
    if magic != b"ANM":
        raise ValueError(f"Not an ANM file (magic={magic!r})")

    version = r.u8()
    if version not in (1, 2):
        raise ValueError(f"Unsupported ANM version {version} (expected 1 or 2)")

    # --- Bone names ---
    bone_count = r.u16()
    bone_names: list[str] = [r.pascal_string() for _ in range(bone_count)]

    # --- Mesh entries ---
    mesh_count = r.u16()
    meshes: list[MeshEntry] = []
    for _ in range(mesh_count):
        name = r.pascal_string()
        frame_count = r.u16()
        meshes.append(MeshEntry(name=name, frame_count=frame_count))

    # --- Tracks ---
    track_count = r.u16()
    tracks: list[Track] = []

    for _ in range(track_count):
        key_count = r.u16()
        keyframes: list[Keyframe] = []

        for _ in range(key_count):
            opcode = r.u8()

            if opcode == 1:
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            elif opcode == 2:
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            elif opcode == 3:
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
                r.u16()  # skip 2 bytes (unused index field per decompiled code)
                x, y, z = r.f32(), r.f32(), r.f32()
                kf = Keyframe(opcode=opcode, x=x, y=y, z=z)

            else:
                # Unknown opcode – can't determine payload size, stop parsing track
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
# Pretty printer
# ---------------------------------------------------------------------------

def print_summary(anm: AnmFile) -> None:
    print(f"ANM version : {anm.version}")
    print(f"Bones       : {len(anm.bone_names)}")
    for i, name in enumerate(anm.bone_names):
        print(f"  [{i:3d}] {name}")

    print(f"Meshes      : {len(anm.meshes)}")
    for i, m in enumerate(anm.meshes):
        print(f"  [{i:3d}] {m.name}  (frame_count={m.frame_count})")

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
                parts.append(f"uvw=({kf.u}, {kf.v}, {kf.w})")
            print(f"    [{k_idx:4d}] {', '.join(parts)}")


def to_dict(anm: AnmFile) -> dict:
    return {
        "version": anm.version,
        "bone_names": anm.bone_names,
        "meshes": [asdict(m) for m in anm.meshes],
        "tracks": [
            {
                "keyframes": [
                    {
                        "opcode": kf.opcode,
                        "opcode_name": kf.opcode_name,
                        **({} if kf.target_name is None else {"target_name": kf.target_name, "target_index": kf.target_index}),
                        **({} if kf.x is None else {"x": kf.x, "y": kf.y, "z": kf.z}),
                        **({} if kf.u is None else {"u": kf.u, "v": kf.v, "w": kf.w}),
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
        description="Parse a game ANM animation file."
    )
    parser.add_argument("file", help="Path to the .anm file")
    parser.add_argument(
        "--json", action="store_true",
        help="Output as JSON instead of the human-readable summary"
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
        anm = parse_anm(data)
    except ValueError as exc:
        print(f"Parse error: {exc}", file=sys.stderr)
        sys.exit(1)

    if args.json:
        output = json.dumps(to_dict(anm), indent=2)
    else:
        # Redirect stdout capture trick for --out
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
