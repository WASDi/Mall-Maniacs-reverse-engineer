package render;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import senfile.SenFile;
import senfile.Util;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.MapiElement;
import senfile.parts.elements.ObjiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

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
    private int meshIdx = 29;
    private int debug = -1;

    private final Movement movement = new Movement(0f, 7f, 22f);

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
                } else if (key == GLFW_KEY_UP && action == GLFW_PRESS) {
                    debug++;
                    glfwSetWindowTitle(window, "debug: " + debug);
                } else if (key == GLFW_KEY_DOWN && action == GLFW_PRESS) {
                    debug--;
                    glfwSetWindowTitle(window, "debug: " + debug);
                } else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
                    meshIdx--;
                    if (meshIdx < 0) {
                        meshIdx = senFile.meshes.size() - 1;
                    }
                    glfwSetWindowTitle(window, "Mesh: " + meshIdx + " (" + senFile.meshes.get(meshIdx).name + ")");
                } else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
                    meshIdx++;
                    if (meshIdx >= senFile.meshes.size()) {
                        meshIdx = 0;
                    }
                    glfwSetWindowTitle(window, "Mesh: " + meshIdx + " (" + senFile.meshes.get(meshIdx).name + ")");
                } else if (key == GLFW_KEY_P && action == GLFW_PRESS) {
                    glfwSetWindowTitle(window, movement.toString());
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
        String filePath = String.format(Util.ROOT_DIR + "scene_ica/MERGED%02d.TPG", idx);
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
        SenMesh mesh = senFile.meshes.get(meshIdx);
        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

        for (int i = 0; i < suboOffsets.length; i++) {
//            if (debug >= 0 && debug != i) {
//                continue;
//            }
            int suboOffset = suboOffsets[i];
            SuboElement.FaceInfo[] faceInfos = senFile.subo.elementByOffset(suboOffset).faceInfos;

            for (int j = 0; j < faceInfos.length; j++) {
//                if (debug >= 0 && debug == j) {
//                    break;
//                }
                SuboElement.FaceInfo faceInfo = faceInfos[j];
                byte[] vertexIndices = faceInfo.vertexIndices;
                int v0 = vertexIndices[0] & 0xFF;
                int v1 = vertexIndices[1] & 0xFF;
                int v2 = vertexIndices[2] & 0xFF;
                int v3 = vertexIndices[3] & 0xFF;

                boolean[] bumps = new boolean[4];
                if (debug >= 0 && mesh instanceof MeshCharacter) {
                    int[] vertexId2Group = ((MeshCharacter) mesh).vertexId2Group;

                    // Render one vertex group at a time
                    if (IntStream.range(0, vertexIndices.length)
                            .mapToObj(it -> vertexIndices[it])
                            .noneMatch(it -> vertexId2Group[it] == debug)) {
                        //continue;
                    }

                    // offset vertex groups
                    for (int k = 0; k < bumps.length; k++) {
                        if (vertexId2Group[vertexIndices[k]] == debug) {
                            bumps[k] = true;
                        }
                    }
                }


                int textureIndexForFace = faceInfo.getMapiIndex();
                MapiElement mapiElement = senFile.mapi.elements[textureIndexForFace];
                int mergedTpgFileIndex = mapiElement.tpgFileIndex;
                byte[] coords = mapiElement.textureCoordBytes;

                float div = 256f;
                // Vertex N, texture x y
                float v0tx = (coords[0] & 0xFF) / div;
                float v0ty = (coords[1] & 0xFF) / div;
                float v1tx = (coords[2] & 0xFF) / div;
                float v1ty = (coords[3] & 0xFF) / div;
                float v2tx = (coords[4] & 0xFF) / div;
                float v2ty = (coords[5] & 0xFF) / div;
                float v3tx = (coords[6] & 0xFF) / div;
                float v3ty = (coords[7] & 0xFF) / div;

                glBindTexture(GL_TEXTURE_2D, texture_MERGED[mergedTpgFileIndex]);

                glBegin(GL_TRIANGLES);

                glTexCoord2f(v0tx, v0ty);
                putVertex(vertices[v0], bumps[0]);

                glTexCoord2f(v1tx, v1ty);
                putVertex(vertices[v1], bumps[1]);

                glTexCoord2f(v2tx, v2ty);
                putVertex(vertices[v2], bumps[2]);

                // --- //

                glTexCoord2f(v3tx, v3ty);
                putVertex(vertices[v3], bumps[3]);

                glTexCoord2f(v0tx, v0ty);
                putVertex(vertices[v0], bumps[0]);

                glTexCoord2f(v2tx, v2ty);
                putVertex(vertices[v2], bumps[2]);


                glEnd();
            }
        }
    }

    private static final float SCALE = .002f;

    private void putVertex(Vertex vertex, boolean bump) {
        if (bump) {
            int offset = -300;
            vertex = new Vertex(vertex.x + offset, vertex.y + offset, vertex.z + offset);
        }
        glVertex3f(vertex.x * SCALE, -vertex.y * SCALE, vertex.z * SCALE);
    }

    private void loop() {
        GL.createCapabilities();

//        initMyRandomTexture();
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
            float angle = totalRuntime * .5f;

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
        List<SenMesh> meshes = senFile.meshes;
        for (int i = 0; i < meshes.size(); i++) {
            String meshName = meshes.get(i).name;
            if (meshName.charAt(0) == '_' || !meshName.contains("LAMP")) {
                continue;
            }

            ObjiElement obji = senFile.obji.elements[meshIdx];
            modelMatrix.translation(SCALE * obji.x,
                                    i * .3f,
                                    SCALE * obji.z)
                    .rotateY(angle * (float) Math.toRadians(90));
            glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
            renderFromSenFile(i);
        }
    }

    public static void main(String[] args) {
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/OBJECTS.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/AQUAMALL.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/MALL1_ICA.SEN");
        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/CHARACTERS.SEN");
        new SenFileRenderer(senFile).run();
    }
}
