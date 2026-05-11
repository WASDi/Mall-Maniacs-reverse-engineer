package render;

import anmfile.AnmFile;
import anmfile.parts.Keyframe;
import anmfile.parts.Track;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.Vertex;
import senfile.parts.mesh.VertexGroupDefinition;

public class AnimationController {

    private static final float ANGLE_UNITS_TO_RAD = (float) (2.0 * Math.PI / 65536.0);
    private static final int TRACKS_PER_SECOND = 10;

    private AnmFile anmFile;
    private int trackCounter = 0;

    public AnimationController(AnmFile anmFile) {
        this.anmFile = anmFile;
    }

    public void switchAnimation(AnmFile anmFile) {
        this.anmFile = anmFile;
    }

    public void update(float totalRuntime) {
        trackCounter = (int) (totalRuntime * TRACKS_PER_SECOND);
    }

    public boolean hasAnimation() {
        return anmFile != null;
    }

    public Vector3f[] buildAnimatedVertices(MeshCharacter character) {
        Vertex[] vertices = character.getVertices();
        Vector3f[] result = new Vector3f[vertices.length];

        int trackCount = anmFile.tracks().size();
        if (trackCount == 0) {
            for (int i = 0; i < vertices.length; i++) {
                result[i] = new Vector3f(vertices[i].x, vertices[i].y, vertices[i].z);
            }
            return result;
        }
        int trackIdx = Math.floorMod(trackCounter, trackCount);

        Matrix4f[] subObjRotations = new Matrix4f[16];
        for (int i = 0; i < 16; i++) {
            subObjRotations[i] = new Matrix4f();
        }

        for (int subObjIdx = 0; subObjIdx < 16; subObjIdx++) {
            Matrix4f rot = null;
            for (int t = trackIdx; t >= 0 && rot == null; t--) {
                Track tTrack = anmFile.tracks().get(t);
                for (Keyframe kf : tTrack.keyframes()) {
                    if (kf.opcode() != 5) continue;
                    if (kf.targetIndex() == null || kf.u() == null) continue;

                    int meshEntryIdx = kf.targetIndex();
                    if (meshEntryIdx < 0 || meshEntryIdx >= anmFile.meshes().size()) continue;

                    int kfSubObjIdx = anmFile.meshes().get(meshEntryIdx).frameCount();
                    if (kfSubObjIdx < 0 || kfSubObjIdx >= 16) continue;

                    if (kfSubObjIdx == subObjIdx) {
                        float rx = kf.u() * ANGLE_UNITS_TO_RAD;
                        float ry = kf.v() * ANGLE_UNITS_TO_RAD;
                        float rz = kf.w() * ANGLE_UNITS_TO_RAD;

                        rot = new Matrix4f()
                                .rotateZ(rz)
                                .rotateX(rx)
                                .rotateY(ry);
                        break;
                    }
                }
            }
            if (rot != null) {
                subObjRotations[subObjIdx] = rot;
            }
        }

        for (int i = 0; i < vertices.length; i++) {
            int group = character.vertexId2Group[i];
            VertexGroupDefinition vgd = character.vertexGroupDefinitions[group];
            int subObjIdx = vgd.index;

            Vertex v = vertices[i];
            float vx = v.x, vy = v.y, vz = v.z;

            Matrix4f rot = subObjRotations[subObjIdx < 16 ? subObjIdx : 0];

            Vector3f translation = getSubObjectTranslation(character, subObjIdx);
            Vector3f rotated = new Vector3f(vx, vy, vz).mulDirection(rot);

            vx = rotated.x + translation.x;
            vy = rotated.y + translation.y;
            vz = rotated.z + translation.z;

            result[i] = new Vector3f(vx, vy, vz);
        }

        return result;
    }

    private static Vector3f getSubObjectTranslation(MeshCharacter character, int subObjIdx) {
        if (subObjIdx == 0) {
            return new Vector3f(0, 0, 0);
        }
        int i = subObjIdx - 1;
        return new Vector3f(character.subTransformX[i], character.subTransformY[i], character.subTransformZ[i]);
    }
}
