package render;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import senfile.SenFile;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.ObjiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.Mesh;
import senfile.parts.mesh.Vertex;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SenFileRenderer {
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWFramebufferSizeCallback fbCallback;

    private long window;
    private int width = 1600;
    private int height = 600;

    // JOML matrices
    private Matrix4f projMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f modelMatrix = new Matrix4f();
    private Matrix4f modelViewMatrix = new Matrix4f();

    // FloatBuffer for transferring matrices to OpenGL
    private FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    private int texture_random;
    private static final int NUM_MERGED_TPG_FILES = 32;
    private int[] texture_MERGED = new int[NUM_MERGED_TPG_FILES];

    private final SenFile senFile;
    private int meshIdx = 0;

    private final Movement movement = new Movement();

    public SenFileRenderer(SenFile senFile) {
        this.senFile = senFile;
    }

    private void run() {
        try {
            init();
            try {
                loop();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }

            glfwDestroyWindow(window);
            keyCallback.free();
        } finally {
            glfwTerminate();
            errorCallback.free();
        }
    }

    private void init() {
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key,
                               int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, true);
                } else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
                    meshIdx--;
                    glfwSetWindowTitle(window, "Mesh: " + meshIdx + " (" + senFile.getMeshes().get(meshIdx).name + ")");
                } else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
                    meshIdx++;
                    glfwSetWindowTitle(window, "Mesh: " + meshIdx + " (" + senFile.getMeshes().get(meshIdx).name + ")");
                } else if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                    movement.handleMovement(key, action == GLFW_PRESS);
                }
            }
        });
        glfwSetFramebufferSizeCallback(window,
                                       fbCallback = new GLFWFramebufferSizeCallback() {
                                           @Override
                                           public void invoke(long window, int w, int h) {
                                               if (w > 0 && h > 0) {
                                                   width = w;
                                                   height = h;
                                               }
                                           }
                                       });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }

    private void initMyRandomTexture() {
        texture_random = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture_random);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        int height = 64;
        int width = 64;

        Random r = new Random();
        float[] pixels = new float[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = r.nextFloat();
        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_FLOAT, pixels);
    }

    private void initMergedTpgTexture(int idx) {
        String filePath = String.format("/home/wasd/Downloads/Mall Maniacs/scene_ica/MERGED%02d.TPG", idx);
        TpgImage tpgImage = TpgImageFactory.fromFile(filePath);
        ByteBuffer pixels = tpgImage.toByteBuffer();

        texture_MERGED[idx] = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture_MERGED[idx]);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TpgImage.SIZE, TpgImage.SIZE, 0,
                     GL_RGB, GL_UNSIGNED_BYTE, pixels);
    }

    private void renderFromSenFile(int meshIdx) {
        Mesh mesh = senFile.getMeshes().get(meshIdx);
        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

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

                glBindTexture(GL_TEXTURE_2D, texture_MERGED[mergedTpgFileIndex]);
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
    }

    private static final float SCALE = .002f;

    private void putVertex(Vertex vertex) {
        glVertex3f(vertex.x * SCALE, -vertex.y * SCALE, vertex.z * SCALE);
    }

    private void loop() {
        GL.createCapabilities();

        initMyRandomTexture();
        for (int i = 0; i < NUM_MERGED_TPG_FILES; i++) {
            initMergedTpgTexture(i);
        }

        // Set the clear color
        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
//        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);

        // Build the projection matrix. Watch out here for integer division
        // when computing the aspect ratio!
        projMatrix.setPerspective((float) Math.toRadians(40),
                                  (float) width / height, 0.01f, 100.0f);

        // Remember the current time.
        long firstTime = System.nanoTime();
        while (!glfwWindowShouldClose(window)) {
            // Build time difference between this and first time.
            long thisTime = System.nanoTime();
            float totalRuntime = (thisTime - firstTime) / 1E9f;
            // Compute some rotation angle.
            float angle = totalRuntime * .1f;

            movement.step(.5f); //TODO base on time

            // Make the viewport always fill the whole window.
            glViewport(0, 0, width, height);

            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(projMatrix.get(fb));

            // Set lookat view matrix
            viewMatrix.setLookAt(0.0f, 10.0f, 30.0f,
                                 0, 0, 0,
                                 0.0f, 1.0f, 0.0f);

            viewMatrix.translate(movement.getX(), movement.getY(), movement.getZ());


            glMatrixMode(GL_MODELVIEW);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


            renderOne(angle);
//            renderMany(angle);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderOne(float angle) {
        modelMatrix.translation(0, 0, 0)
                .rotateY(angle * (float) Math.toRadians(90));
        glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
        renderFromSenFile(meshIdx);
    }

    private void renderMany(float angle) {
        List<Mesh> meshes = senFile.getMeshes();
        for (int i = 0; i < meshes.size(); i++) {
            String meshName = meshes.get(i).name;
            if (meshName.charAt(0) == '_' || !meshName.contains("LAMP")) {
                continue;
            }

            ObjiElement obji = senFile.getObji().elements[meshIdx];
            modelMatrix.translation(SCALE * obji.xRight,
                                    i * .3f,
                                    SCALE * obji.zRight)
                    .rotateY(angle * (float) Math.toRadians(90));
            glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
            renderFromSenFile(i);
        }
    }

    public static void main(String[] args) {
//        SenFile senFile = SenFileFactory.fromFile("/home/wasd/Downloads/Mall Maniacs/scene_aqua/OBJECTS.SEN");
        SenFile senFile = SenFileFactory.fromFile("/home/wasd/Downloads/Mall Maniacs/scene_ica/MALL1_ICA.SEN");
//        SenFile senFile = SenFileFactory.fromFile("/home/wasd/Downloads/Mall Maniacs/scene_ica/CHARACTERS.SEN");
        new SenFileRenderer(senFile).run();
    }
}