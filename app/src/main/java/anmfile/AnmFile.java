package anmfile;

import anmfile.parts.Keyframe;
import anmfile.parts.MeshEntry;
import anmfile.parts.Track;

import java.util.List;

public record AnmFile(int version, List<String> boneNames, List<MeshEntry> meshes, List<Track> tracks) {

    public void dumpInfo() {
        System.out.println("ANM version : " + version);
        System.out.println("Bones       : " + boneNames.size());
        for (int i = 0; i < boneNames.size(); i++) {
            System.out.println("  [" + i + "] " + boneNames.get(i));
        }

        System.out.println("Meshes      : " + meshes.size());
        for (int i = 0; i < meshes.size(); i++) {
            MeshEntry m = meshes.get(i);
            System.out.println("  [" + i + "] " + m.name() + "  (frame_count=" + m.frameCount() + ")");
        }

        System.out.println("Tracks      : " + tracks.size());
        for (int tIdx = 0; tIdx < tracks.size(); tIdx++) {
            Track track = tracks.get(tIdx);
            System.out.println("  Track " + tIdx + ": " + track.keyframes().size() + " keyframes");
            for (int kIdx = 0; kIdx < track.keyframes().size(); kIdx++) {
                Keyframe kf = track.keyframes().get(kIdx);
                StringBuilder sb = new StringBuilder();
                sb.append("op=").append(opcodeName(kf.opcode()));
                if (kf.targetName() != null) {
                    sb.append(", target='").append(kf.targetName()).append("'[").append(kf.targetIndex()).append("]");
                }
                if (kf.x() != null) {
                    sb.append(", xyz=(").append(String.format("%.4f", kf.x())).append(", ").append(String.format("%.4f", kf.y())).append(", ").append(String.format("%.4f", kf.z())).append(")");
                }
                if (kf.u() != null) {
                    sb.append(", uvw=(").append(kf.u()).append(", ").append(kf.v()).append(", ").append(kf.w()).append(")");
                }
                System.out.println("    [" + kIdx + "] " + sb);
            }
        }
    }

    public static String opcodeName(int opcode) {
        return switch (opcode) {
            case 1 -> "set_pos_A";
            case 2 -> "set_pos_B";
            case 3 -> "move_bone";
            case 4 -> "sprite_frame";
            case 5 -> "mesh_frame";
            case 6 -> "move_implicit_bone";
            default -> "unknown_" + opcode;
        };
    }
}
