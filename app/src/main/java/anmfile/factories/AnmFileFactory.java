package anmfile.factories;

import anmfile.AnmFile;
import anmfile.parts.Keyframe;
import anmfile.parts.MeshEntry;
import anmfile.parts.Track;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AnmFileFactory {

    public static AnmFile parseFromBufferPosition(ByteBuffer buffer) {
        byte[] magic = new byte[3];
        buffer.get(magic);
        if (magic[0] != 'A' || magic[1] != 'N' || magic[2] != 'M') {
            throw new IllegalArgumentException("Not an ANM file (magic=" + new String(magic, StandardCharsets.ISO_8859_1) + ")");
        }

        int version = buffer.get() & 0xFF;
        if (version != 1 && version != 2) {
            throw new IllegalArgumentException("Unsupported ANM version " + version + " (expected 1 or 2)");
        }

        int boneCount = buffer.getShort() & 0xFFFF;
        List<String> boneNames = new ArrayList<>();
        for (int i = 0; i < boneCount; i++) {
            boneNames.add(readPascalString(buffer));
        }

        int meshCount = buffer.getShort() & 0xFFFF;
        List<MeshEntry> meshes = new ArrayList<>();
        for (int i = 0; i < meshCount; i++) {
            String name = readPascalString(buffer);
            int frameCount = buffer.getShort() & 0xFFFF;
            meshes.add(new MeshEntry(name, frameCount));
        }

        int trackCount = buffer.getShort() & 0xFFFF;
        List<Track> tracks = new ArrayList<>();

        for (int i = 0; i < trackCount; i++) {
            int keyCount = buffer.getShort() & 0xFFFF;
            List<Keyframe> keyframes = new ArrayList<>();

            for (int j = 0; j < keyCount; j++) {
                int opcode = buffer.get() & 0xFF;

                if (opcode == 1) {
                    float x = buffer.getFloat();
                    float y = buffer.getFloat();
                    float z = buffer.getFloat();
                    keyframes.add(new Keyframe(opcode, null, null, x, y, z, null, null, null));

                } else if (opcode == 2) {
                    float x = buffer.getFloat();
                    float y = buffer.getFloat();
                    float z = buffer.getFloat();
                    keyframes.add(new Keyframe(opcode, null, null, x, y, z, null, null, null));

                } else if (opcode == 3) {
                    int boneIdx = buffer.getShort() & 0xFFFF;
                    String boneName = boneIdx < boneNames.size() ? boneNames.get(boneIdx) : null;
                    float x = buffer.getFloat();
                    float y = buffer.getFloat();
                    float z = buffer.getFloat();
                    keyframes.add(new Keyframe(opcode, boneName, boneIdx, x, y, z, null, null, null));

                } else if (opcode == 4) {
                    int boneIdx = buffer.getShort() & 0xFFFF;
                    String boneName = boneIdx < boneNames.size() ? boneNames.get(boneIdx) : null;
                    int u = buffer.getShort();
                    int v = buffer.getShort();
                    int w = buffer.getShort();
                    keyframes.add(new Keyframe(opcode, boneName, boneIdx, null, null, null, u, v, w));

                } else if (opcode == 5) {
                    int meshIdx = buffer.getShort() & 0xFFFF;
                    String meshName = meshIdx < meshes.size() ? meshes.get(meshIdx).name() : null;
                    int u = buffer.getShort();
                    int v = buffer.getShort();
                    int w = buffer.getShort();
                    keyframes.add(new Keyframe(opcode, meshName, meshIdx, null, null, null, u, v, w));

                } else if (opcode == 6) {
                    buffer.getShort(); // skip 2 bytes (unused index field per decompiled code)
                    float x = buffer.getFloat();
                    float y = buffer.getFloat();
                    float z = buffer.getFloat();
                    keyframes.add(new Keyframe(opcode, null, null, x, y, z, null, null, null));

                } else {
                    System.err.println(
                            "Warning: unknown opcode " + opcode + " at offset " + (buffer.position() - 1)
                                    + "; stopping track parse early."
                    );
                    break;
                }
            }

            tracks.add(new Track(keyframes));
        }

        return new AnmFile(version, boneNames, meshes, tracks);
    }

    private static String readPascalString(ByteBuffer buffer) {
        int length = buffer.get() & 0xFF;
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}
