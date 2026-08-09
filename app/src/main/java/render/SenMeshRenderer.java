package render;

import org.joml.Vector3f;
import render.texture.TextureAtlas;
import senfile.SenFile;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;

import static org.lwjgl.opengl.GL11.*;

/**
 * Draws the faces of a SenMesh using the fixed-function pipeline. Shared between the
 * standalone GLFW renderer and the Swing-embedded AWTGLCanvas viewer.
 */
public class SenMeshRenderer {

    public static final float SCALE = .002f;

    private final SenFile senFile;
    private final AnimationController animationController;
    private int textureId;

    private int debug = -1;

    public SenMeshRenderer(SenFile senFile, AnimationController animationController) {
        this.senFile = senFile;
        this.animationController = animationController;
    }

    public void setDebugGroup(int debug) {
        this.debug = debug;
    }

    public void initTextures() {
        textureId = TextureAtlas.atlasFromTnam(senFile.tnam, senFile.directory).toLwjglTexture().getId();
    }

    public void disposeTextures() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
    }

    public void drawMesh(SenMesh mesh) {
        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

        Vector3f[] animatedVertices = null;
        if (mesh instanceof MeshCharacter character && animationController != null && animationController.hasAnimation()) {
            animatedVertices = animationController.buildAnimatedVertices(character);
        }

        renderSubo(mesh, suboOffsets, vertices, animatedVertices, false);
        renderSubo(mesh, suboOffsets, vertices, animatedVertices, true);
    }

    private void renderSubo(SenMesh mesh, int[] suboOffsets, Vertex[] vertices, Vector3f[] animatedVertices, boolean renderTransparent) {
        glBindTexture(GL_TEXTURE_2D, textureId);

        for (int suboOffset : suboOffsets) {
            SuboElement suboElement = senFile.subo.elementByOffset(suboOffset);
            if (suboElement.getTransparencyMode().isTransparent != renderTransparent) {
                continue;
            }

            SuboElement.FaceInfo[] faceInfos = suboElement.faceInfos;

            for (SuboElement.FaceInfo faceInfo : faceInfos) {
                byte[] vertexIndices = faceInfo.vertexIndices;
                int v0 = vertexIndices[0] & 0xFF;
                int v1 = vertexIndices[1] & 0xFF;
                int v2 = vertexIndices[2] & 0xFF;
                int v3 = vertexIndices[3] & 0xFF;

                if (mesh instanceof MeshCharacter character) {
                    int g0 = character.vertexId2Group[v0];
                    int g1 = character.vertexId2Group[v1];
                    int g2 = character.vertexId2Group[v2];
                    int g3 = character.vertexId2Group[v3];
                    if (debug < 0) {
                        // noop
                    } else if (!(g0 == debug || g1 == debug || g2 == debug)) {
                        continue;
                    }
                }

                int textureIndexForFace = faceInfo.getMapiIndex();
                MapiElement mapiElement = senFile.mapi.elements[textureIndexForFace];
                int materialIndex = mapiElement.materialIndex;
                byte[] coords = mapiElement.textureCoordBytes;

                float xOffset = TextureAtlas.xOffsetFromIndex(materialIndex);
                float yOffset = TextureAtlas.yOffsetFromIndex(materialIndex);
                float divX = 256f * TextureAtlas.SIZE_X;
                float divY = 256f * TextureAtlas.SIZE_Y;

                // Vertex N, texture x y
                float v0tx = xOffset + (coords[0] & 0xFF) / divX;
                float v0ty = yOffset + (coords[1] & 0xFF) / divY;
                float v1tx = xOffset + (coords[2] & 0xFF) / divX;
                float v1ty = yOffset + (coords[3] & 0xFF) / divY;
                float v2tx = xOffset + (coords[4] & 0xFF) / divX;
                float v2ty = yOffset + (coords[5] & 0xFF) / divY;
                float v3tx = xOffset + (coords[6] & 0xFF) / divX;
                float v3ty = yOffset + (coords[7] & 0xFF) / divY;

                glBegin(GL_TRIANGLES);

                glTexCoord2f(v0tx, v0ty);
                putAnimatedVertex(vertices[v0], animatedVertices, v0);

                glTexCoord2f(v1tx, v1ty);
                putAnimatedVertex(vertices[v1], animatedVertices, v1);

                glTexCoord2f(v2tx, v2ty);
                putAnimatedVertex(vertices[v2], animatedVertices, v2);

                if (suboElement.isQuad()) {
                    glTexCoord2f(v3tx, v3ty);
                    putAnimatedVertex(vertices[v3], animatedVertices, v3);

                    glTexCoord2f(v0tx, v0ty);
                    putAnimatedVertex(vertices[v0], animatedVertices, v0);

                    glTexCoord2f(v2tx, v2ty);
                    putAnimatedVertex(vertices[v2], animatedVertices, v2);
                }

                glEnd();
            }
        }
    }

    private void putVertex(Vertex vertex) {
        glVertex3f(vertex.x * SCALE, -vertex.y * SCALE, vertex.z * SCALE);
    }

    /**
     * Emit a vertex, using the animated position if available, otherwise the static one.
     * Y is negated to match the engine coordinate-system convention used in putVertex.
     */
    private void putAnimatedVertex(Vertex vertex, Vector3f[] animatedVertices, int vertexIdx) {
        if (animatedVertices != null) {
            Vector3f av = animatedVertices[vertexIdx];
            glVertex3f(av.x * SCALE, -av.y * SCALE, av.z * SCALE);
        } else {
            putVertex(vertex);
        }
    }
}
