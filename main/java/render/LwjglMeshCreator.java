package render;

import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.Texture;
import render.texture.TextureAtlas;
import render.util.Utils;
import senfile.SenFile;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;

import java.util.ArrayList;
import java.util.List;

public class LwjglMeshCreator {

    public static Mesh crateMeshFromSenMesh(SenFile senFile, SenMesh mesh) {

        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

        MeshBuilderFromQuad meshBuilderFromQuad = new MeshBuilderFromQuad();

        for (int suboOffset : suboOffsets) {
            SuboElement.FaceInfo[] faceInfos = senFile.getSubo().elementByOffset(suboOffset).faceInfos;

            for (SuboElement.FaceInfo faceInfo : faceInfos) {
                byte[] vertexIndices = faceInfo.vertexIndices;
                int v0 = vertexIndices[0] & 0xFF;
                int v1 = vertexIndices[1] & 0xFF;
                int v2 = vertexIndices[2] & 0xFF;
                int v3 = vertexIndices[3] & 0xFF;

                int textureIndexForFace = faceInfo.getMapiIndex();
                MapiElement mapiElement = senFile.getMapi().elements[textureIndexForFace];
                int mergedTpgFileIndex = mapiElement.mergedTpgFileIndex;
                byte[] coords = mapiElement.textureCoordBytes;


                float xOffset = TextureAtlas.xOffsetFromIndex(mergedTpgFileIndex);
                float yOffset = TextureAtlas.yOffsetFromIndex(mergedTpgFileIndex);
                float divX = 256f * TextureAtlas.SIZE_X;
                float divY = 256f * TextureAtlas.SIZE_Y;

                // Vertex N, texture xy
                float v0tx = xOffset + (coords[0] & 0xFF) / divX;
                float v0ty = yOffset + (coords[1] & 0xFF) / divY;

                float v1tx = xOffset + (coords[2] & 0xFF) / divX;
                float v1ty = yOffset + (coords[3] & 0xFF) / divY;

                float v2tx = xOffset + (coords[4] & 0xFF) / divX;
                float v2ty = yOffset + (coords[5] & 0xFF) / divY;

                float v3tx = xOffset + (coords[6] & 0xFF) / divX;
                float v3ty = yOffset + (coords[7] & 0xFF) / divY;

                meshBuilderFromQuad.putVertex(vertices[v0], v0tx, v0ty);
                meshBuilderFromQuad.putVertex(vertices[v1], v1tx, v1ty);
                meshBuilderFromQuad.putVertex(vertices[v2], v2tx, v2ty);
                meshBuilderFromQuad.putVertex(vertices[v3], v3tx, v3ty);
                meshBuilderFromQuad.conjureTrianglesAfterQuadPut();
            }
        }


        return meshBuilderFromQuad.build();
    }

    private static final class MeshBuilderFromQuad {

        private final List<Float> vertexFloats = new ArrayList<>();
        private final List<Float> textureFloats = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
        private int indexOffset = 0;

        void putVertex(Vertex vertex, float textureX, float textureY) {
            vertexFloats.add(VertexTranslator.translateX(vertex.x));
            vertexFloats.add(VertexTranslator.translateY(vertex.y));
            vertexFloats.add(VertexTranslator.translateZ(vertex.z));
            textureFloats.add(textureX);
            textureFloats.add(textureY);
        }

        void conjureTrianglesAfterQuadPut() {
            int offset = 4 * indexOffset;

            indices.add(offset);
            indices.add(offset + 1);
            indices.add(offset + 2);

            indices.add(offset + 3);
            indices.add(offset);
            indices.add(offset + 2);

            indexOffset++;
        }

        public Mesh build() {
            float[] positions = Utils.floatListToArray(vertexFloats);
            float[] textCoords = Utils.floatListToArray(textureFloats);
            int[] indices = Utils.intListToArray(this.indices);


            Texture texture = TextureAtlas.getSingletonTexture();
//            Texture texture = MergedTpgTextureRepo.REPO.textureForMergedId(15);
            return new Mesh(positions, textCoords, indices, texture);
        }
    }
}
