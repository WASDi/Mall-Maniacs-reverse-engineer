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
        for (SenMesh mesh : senFile.meshes) {
            buffer.putInt(HeaderTexts.MESH);
            buffer.putInt(mesh.bytesLeftUntilName);
            buffer.putInt(mesh.flags);
            buffer.putInt(mesh.unknown1);
            buffer.putInt(mesh.numExtraSub);
            buffer.putInt(mesh.subTransformOffset);
            buffer.putInt(mesh.subMaterialOffset);
            buffer.putInt(mesh.lodOffset);
            buffer.putInt(mesh.meshHandle);
            buffer.putInt(mesh.reserved0);
            buffer.putInt(mesh.reserved1);
            buffer.putInt(mesh.reserved2);
            buffer.putInt(mesh.unknown4);
            buffer.putInt(mesh.reserved4);
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
            buffer.putInt(mesh.meshDataBaseSize);
            buffer.putInt(mesh.maxDrawDistance);
            buffer.putInt(mesh.numVertices);
            buffer.putInt(mesh.vertexDataOffset);
            buffer.putInt(mesh.reserved0);
            buffer.putInt(mesh.vertexDataEndOffset);
            buffer.putInt(mesh.numSuboReferences);
            buffer.putInt(mesh.suboRefsTableOffset);
            buffer.putInt(mesh.numMeshParts);
            buffer.putInt(mesh.faceGroupsCount);
            buffer.putInt(mesh.reserved1);
            buffer.putInt(mesh.reserved2);
            buffer.putInt(mesh.vertexGroupDefOffset);
            for (Vertex vertex : mesh.vertices) {
                int part1 = VerticesFactory.reversePart1(vertex);
                int part2 = VerticesFactory.reversePart2(vertex);
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
