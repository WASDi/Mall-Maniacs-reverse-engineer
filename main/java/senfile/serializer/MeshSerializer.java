package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.factories.VerticesFactory;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.MeshObject;
import senfile.parts.mesh.MeshVisitor;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;

import java.nio.ByteBuffer;

import static senfile.serializer.SenFileSerializer.roundTo4WithMargin;

public class MeshSerializer {

    public static void serializeMeshes(SenFile senFile, ByteBuffer buffer) {
        SubMeshSerializer subMeshSerializer = new SubMeshSerializer(buffer);
        for (SenMesh mesh : senFile.getMeshes()) {
            buffer.putInt(HeaderTexts.MESH);
            buffer.putInt(mesh.bytesLeftUntilName);
            buffer.putInt(mesh.constant1);
            buffer.putInt(mesh.constant2);
            buffer.putInt(mesh.isCharacter1);
            buffer.putInt(mesh.constant3);
            buffer.putInt(mesh.isCharacter2);
            buffer.putInt(mesh.isCharacter3);
            buffer.putInt(mesh._7);
            buffer.putInt(mesh.constant4);
            buffer.putInt(mesh.constant5);
            buffer.putInt(mesh.constant6);
            buffer.putInt(mesh._11);
            buffer.putInt(mesh.constant7);
            mesh.visit(subMeshSerializer);
            buffer.putInt(HeaderTexts.NAME);

            int serializedNameLength = roundTo4WithMargin(mesh.name.length());
            buffer.putInt(serializedNameLength);
            for (char c : mesh.name.toCharArray()) {
                buffer.put((byte) c);
            }
            int padding = serializedNameLength - mesh.name.length();
            for (int i = 0; i < padding; i++) {
                buffer.put((byte) 0);
            }
        }
    }

    private static class SubMeshSerializer implements MeshVisitor {

        private final ByteBuffer buffer;

        private SubMeshSerializer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void visit(MeshObject mesh) {
            buffer.putInt(mesh.constant8);
            buffer.putInt(mesh.constant9);
            buffer.putInt(mesh.numVertices);
            buffer.putInt(mesh.constant10);
            buffer.putInt(mesh.constant11);
            buffer.putInt(mesh.ok_18);
            buffer.putInt(mesh.numSuboReferences);
            buffer.putInt(mesh.ok_20);
            buffer.putInt(mesh.constant12);
            buffer.putInt(mesh._22);
            buffer.putInt(mesh.constant13);
            buffer.putInt(mesh.constant14);
            buffer.putInt(mesh.ok_25);
            for (Vertex vertex : mesh.vertices) {
                int part1 = VerticesFactory.reversePart1(vertex);
                int part2 = VerticesFactory.reversePart2(vertex);
//                part1 = -1431655766; // AA AA AA AA
//                part2 = -1431655766;
                buffer.putInt(part1);
                buffer.putInt(part2);
            }
            for (int suboReference : mesh.suboReferences) {
                buffer.putInt(suboReference);
            }
            buffer.putInt(mesh.vertexGroup.size);
            buffer.putInt(mesh.vertexGroup.constant0);
            buffer.putInt(mesh.vertexGroup.index);
        }

        @Override
        public void visit(MeshCharacter mesh) {
            throw new UnsupportedOperationException("not impl");
        }
    }
}
