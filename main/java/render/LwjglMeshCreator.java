package render;

import org.lwjglb.engine.graph.MeshTri;
import org.lwjglb.engine.graph.Texture;
import senfile.SenFile;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.Vertex;

import java.io.IOException;
import java.util.Random;

public class LwjglMeshCreator {

    private static final float SCALE = 1 / 1000f;

    public static MeshTri makeMeAMesh(int meshIdx) {
        SenFile senFile = SenFileFactory.fromFile("/home/wasd/Downloads/Mall Maniacs/scene_ica/MALL1_ICA.SEN");

        senfile.parts.mesh.Mesh mesh = senFile.getMeshes().get(meshIdx);
        Vertex[] vertices = mesh.getVertices();

        float[] positions = new float[vertices.length * 3]; // alla vertexars xyz
        float[] textCoords = new float[vertices.length * 2]; // alla vertexars texturkoordinater

        for (int i = 0; i < vertices.length; i++) {
            Vertex vertex = vertices[i];
            int offset = i * 3;
            positions[offset] = vertex.x * SCALE;
            positions[offset + 1] = vertex.y * -SCALE; // invert y
            positions[offset + 2] = vertex.z * SCALE;
        }

        Random r = new Random();
        for (int i = 0; i < textCoords.length; i++) {
            textCoords[i] = r.nextFloat();
        }


        int[] suboOffsets = mesh.getSuboOffsets();
        if (suboOffsets.length != 1) {
            System.out.println("Bad support for >1 subos: " + suboOffsets.length);
            throw new UnsupportedOperationException("not yet impl");
        }
        int suboOffset = suboOffsets[0];
        SuboElement.FaceInfo[] faceInfos = senFile.getSubo().elementByOffset(suboOffset).faceInfos;


        int[] indices = new int[faceInfos.length * 6];

        for (int i = 0; i < faceInfos.length; i++) {
            SuboElement.FaceInfo faceInfo = faceInfos[i];
            byte[] vertexIndices = faceInfo.vertexIndices;
            int v0 = vertexIndices[0] & 0xFF;
            int v1 = vertexIndices[1] & 0xFF;
            int v2 = vertexIndices[2] & 0xFF;
            int v3 = vertexIndices[3] & 0xFF;

            // Convert to triangles, should be quads!
            int idx = i * 6;
            indices[idx] = v0;
            indices[idx + 1] = v1;
            indices[idx + 2] = v2;
            indices[idx + 3] = v3;
            indices[idx + 4] = v0;
            indices[idx + 5] = v2;
        }

        int textureIndexForFace = faceInfos[0].getMapiIndex();
        MapiElement mapiElement = senFile.getMapi().elements[textureIndexForFace];
        int mergedTpgFileIndex = mapiElement.mergedTpgFileIndex;

        Texture texture = getRealTexture(mergedTpgFileIndex);

        return new MeshTri(positions, textCoords, indices, texture);
    }

    private static Texture getRealTexture(int mergedTpgFileIndex) {
        int textureGlId = MergedTpgTextureRepo.REPO.textureGlIdForMergedId(mergedTpgFileIndex);
        return new Texture(textureGlId);
    }

    public static Texture getDummyTexture() {
        try {
            return new Texture("/textures/grassblock.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }
}
