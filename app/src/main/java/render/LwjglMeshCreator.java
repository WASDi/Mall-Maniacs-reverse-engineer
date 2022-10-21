package render;

import org.joml.Vector3f;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.Texture;
import render.texture.TextureAtlas;
import render.util.Utils;
import senfile.SenFile;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.elements.SuboElementTransparencyMode;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;

import java.util.ArrayList;
import java.util.List;

public class LwjglMeshCreator {

    public static Mesh crateMeshFromSenMesh(SenFile senFile, SenMesh mesh, Texture textureAtlas) {

        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

        MeshBuilder meshBuilder = new MeshBuilder();

        for (int suboOffset : suboOffsets) {
            SuboElement suboElement = senFile.subo.elementByOffset(suboOffset);
            SuboElement.FaceInfo[] faceInfos = suboElement.faceInfos;
            SuboElementTransparencyMode transparencyMode = suboElement.getTransparencyMode();
            boolean isQuad = suboElement.isQuad();

            for (SuboElement.FaceInfo faceInfo : faceInfos) {
                byte[] vertexIndices = faceInfo.vertexIndices;
                int v0 = vertexIndices[0] & 0xFF;
                int v1 = vertexIndices[1] & 0xFF;
                int v2 = vertexIndices[2] & 0xFF;
                int v3 = vertexIndices[3] & 0xFF;

                int textureIndexForFace = faceInfo.getMapiIndex();
                MapiElement mapiElement = senFile.mapi.elements[textureIndexForFace];
                int tpgFileIndex = mapiElement.tpgFileIndex;
                byte[] coords = mapiElement.textureCoordBytes;


                float xOffset = TextureAtlas.xOffsetFromIndex(tpgFileIndex);
                float yOffset = TextureAtlas.yOffsetFromIndex(tpgFileIndex);
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

                Vector3f normal = NormalCalculator.calculate(vertices[v0], vertices[v1], vertices[v2]);

                meshBuilder.putVertex(vertices[v0], v0tx, v0ty);
                meshBuilder.putVertex(vertices[v1], v1tx, v1ty);
                meshBuilder.putVertex(vertices[v2], v2tx, v2ty);
                meshBuilder.putNormal(normal);
                meshBuilder.putNormal(normal);
                meshBuilder.putNormal(normal);

                if (isQuad) {
                    meshBuilder.putVertex(vertices[v3], v3tx, v3ty);
                    meshBuilder.putNormal(normal);
                }

                meshBuilder.conjureTrianglesAfterVerticesPut(isQuad);
                if (transparencyMode.mustMirrorSurface) {
                    // TODO incorrect normal, need to duplicate vertices to fix
                    meshBuilder.conjureReverseTrianglesAfterVerticesPut(isQuad);
                }

                meshBuilder.incrementIndex(isQuad);
            }
        }

        return meshBuilder.build(textureAtlas);
    }

    private static final class MeshBuilder {

        private final List<Float> vertexFloats = new ArrayList<>();
        private final List<Float> textureFloats = new ArrayList<>();
        private final List<Float> normalFloats = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
        private int offset = 0;

        void putVertex(Vertex vertex, float textureX, float textureY) {
            vertexFloats.add(VertexTranslator.translateX(vertex.x));
            vertexFloats.add(VertexTranslator.translateY(vertex.y));
            vertexFloats.add(VertexTranslator.translateZ(vertex.z));
            textureFloats.add(textureX);
            textureFloats.add(textureY);
        }

        public void putNormal(Vector3f normal) {
            normalFloats.add(normal.x);
            normalFloats.add(normal.y);
            normalFloats.add(normal.z);
        }

        void conjureTrianglesAfterVerticesPut(boolean isQuad) {
            // Always add one triangle
            indices.add(offset);
            indices.add(offset + 1);
            indices.add(offset + 2);

            if (isQuad) {
                // If quad, make two triangles to mimic quad
                indices.add(offset + 3);
                indices.add(offset);
                indices.add(offset + 2);
            }
        }

        void conjureReverseTrianglesAfterVerticesPut(boolean isQuad) {
            indices.add(offset);
            indices.add(offset + 2);
            indices.add(offset + 1);

            if (isQuad) {
                indices.add(offset + 3);
                indices.add(offset + 2);
                indices.add(offset);
            }
        }

        public Mesh build(Texture textureAtlas) {
            float[] positions = Utils.floatListToArray(vertexFloats);
            float[] textCoords = Utils.floatListToArray(textureFloats);
            float[] normals = Utils.floatListToArray(normalFloats);
            int[] indices = Utils.intListToArray(this.indices);

            return new Mesh(positions, textCoords, normals, indices, textureAtlas);
        }

        public void incrementIndex(boolean isQuad) {
            offset += isQuad ? 4 : 3;
        }
    }
}
