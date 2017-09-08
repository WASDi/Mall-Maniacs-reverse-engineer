package org.lwjglb.game;

import org.lwjglb.engine.graph.IMesh;
import render.MergedTpgTextureRepo;
import senfile.SenFile;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.Mesh;
import senfile.parts.mesh.Vertex;

import static org.lwjgl.opengl.GL11.*;

public class NaiveSenfileMesh implements IMesh {

    private final SenFile senFile;
    private final Vertex[] vertices;
    private final SuboElement.FaceInfo[] faceInfos;

    public NaiveSenfileMesh(int meshIdx) {
        senFile = SenFileFactory.fromFile("/home/wasd/Downloads/Mall Maniacs/scene_ica/MALL1_ICA.SEN");

        Mesh mesh = senFile.getMeshes().get(meshIdx);
        int[] suboOffsets = mesh.getSuboOffsets();
        if (suboOffsets.length != 1) {
            throw new UnsupportedOperationException("shit...");
        }
        vertices = mesh.getVertices();
        faceInfos = senFile.getSubo().elementByOffset(suboOffsets[0]).faceInfos;
    }

    @Override
    public void render() {

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

            glBindTexture(GL_TEXTURE_2D, MergedTpgTextureRepo.REPO.textureGlIdForMergedId(mergedTpgFileIndex));
            glBegin(GL_QUADS);

            float div = 256f;
            glTexCoord2f((coords[0] & 0xFF) / div, (coords[1] & 0xFF) / div);
            putVertex(vertices[v0]);

            glTexCoord2f((coords[2] & 0xFF) / div, (coords[3] & 0xFF) / div);
            putVertex(vertices[v1]);

            glTexCoord2f((coords[4] & 0xFF) / div, (coords[5] & 0xFF) / div);
            putVertex(vertices[v2]);

            glTexCoord2f((coords[6] & 0xFF) / div, (coords[7] & 0xFF) / div);
            putVertex(vertices[v3]);

            glEnd();
        }
    }

    private static final float SCALE = 1 / 1000f;

    private void putVertex(Vertex vertex) {
        glVertex3f(vertex.x * SCALE, -vertex.y * SCALE, vertex.z * SCALE);
    }

    @Override
    public void cleanUp() {

    }
}
