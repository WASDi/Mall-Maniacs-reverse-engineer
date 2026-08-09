package senviewer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import render.SenMeshRenderer;
import senfile.SenFile;
import senfile.parts.mesh.SenMesh;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadMatrixf;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

/**
 * Swing panel embedding an OpenGL canvas that renders the mesh selected in the tree.
 */
public class MeshViewPanel extends JPanel {

    private final AWTGLCanvas canvas;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    private SenMeshRenderer meshRenderer;
    private SenMeshRenderer lastInitializedRenderer;
    private SenMesh currentMesh;
    private float rotationAngle;

    public MeshViewPanel() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(400, 300));

        canvas = new AWTGLCanvas(new GLData()) {
            @Override
            public void initGL() {
                GL.createCapabilities();
                glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
                glEnable(GL_DEPTH_TEST);
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
                glEnable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }

            @Override
            public void paintGL() {
                paintScene();
            }
        };
        add(canvas, BorderLayout.CENTER);

        new Timer(33, e -> {
            rotationAngle += 0.03f;
            if (meshRenderer != null && canvas.isDisplayable() && canvas.isShowing()) {
                try {
                    canvas.render();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public void setSenFile(SenFile senFile) {
        this.meshRenderer = senFile == null ? null : new SenMeshRenderer(senFile, null);
        this.currentMesh = null;
        this.rotationAngle = 0;
    }

    public void showMesh(SenMesh mesh) {
        this.currentMesh = mesh;
        if (meshRenderer != null && canvas.isDisplayable() && canvas.isShowing()) {
            try {
                canvas.render();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void paintScene() {
        int width = canvas.getFramebufferWidth();
        int height = canvas.getFramebufferHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (meshRenderer == null || currentMesh == null) {
            canvas.swapBuffers();
            return;
        }

        if (lastInitializedRenderer != meshRenderer) {
            if (lastInitializedRenderer != null) {
                lastInitializedRenderer.disposeTextures();
            }
            meshRenderer.initTextures();
            lastInitializedRenderer = meshRenderer;
        }

        Matrix4f proj = new Matrix4f().setPerspective((float) Math.toRadians(40), (float) width / height, 0.01f, 100.0f);
        Matrix4f view = new Matrix4f().setLookAt(0f, 5f, 20f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix4f model = new Matrix4f().translation(0, 0, 0).rotateY(rotationAngle);

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(proj.get(matrixBuffer));

        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(view.mul(model, new Matrix4f()).get(matrixBuffer));

        meshRenderer.drawMesh(currentMesh);
        canvas.swapBuffers();
    }
}
