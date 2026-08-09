package render;

import anmfile.AnmFile;
import anmfile.factories.AnmFileFactory;
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
import senfile.parts.elements.ObjiElement;
import senfile.parts.mesh.SenMesh;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

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
    private final Matrix4f projMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f modelViewMatrix = new Matrix4f();

    // FloatBuffer for transferring matrices to OpenGL
    private final FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    private final SenFile senFile;
    private final AnimationController animationController;
    private final SenMeshRenderer meshRenderer;
    private final File[] animFiles;
    private int currentAnimIndex;
    private int meshIdx = 29;
    private int debug = -1;

    private final Movement movement = new Movement(0f, 7f, 22f);

    public SenFileRenderer(SenFile senFile, File[] animFiles, int currentAnimIndex) {
        this.senFile = senFile;
        this.animFiles = animFiles;
        this.currentAnimIndex = currentAnimIndex;

        AnmFile initialAnm = animFiles.length > 0 ? loadAnimation(animFiles[currentAnimIndex]) : null;
        this.animationController = new AnimationController(initialAnm);
        this.meshRenderer = new SenMeshRenderer(senFile, animationController);
    }

    private static AnmFile loadAnimation(File file) {
        try {
            return AnmFileFactory.fromFile(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void run() {
        try {
            init();
            try {
                loop();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return;
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
                    meshRenderer.setDebugGroup(debug);
                    glfwSetWindowTitle(window, "debug: " + debug);
                } else if (key == GLFW_KEY_DOWN && action == GLFW_PRESS) {
                    debug--;
                    meshRenderer.setDebugGroup(debug);
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
                } else if (key == GLFW_KEY_O && action == GLFW_PRESS) {
                    if (animFiles.length > 0) {
                        currentAnimIndex = (currentAnimIndex + 1) % animFiles.length;
                        AnmFile newAnm = loadAnimation(animFiles[currentAnimIndex]);
                        if (newAnm != null) {
                            animationController.switchAnimation(newAnm);
                            glfwSetWindowTitle(window, animFiles[currentAnimIndex].getName());
                        }
                    }
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

        GL.createCapabilities();

        meshRenderer.initTextures();
    }

    private void loop() {
        // Set the clear color
        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_TEXTURE_2D);

        // Transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Build the projection matrix. Watch out here for integer division
        // when computing the aspect ratio!
        projMatrix.setPerspective((float) Math.toRadians(40), (float) width / height, 0.01f, 100.0f);

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
            viewMatrix.setLookAt(
                    0.0f, 10.0f, 30.0f,
                    0, 0, 0,
                    0.0f, 1.0f, 0.0f
            );

            viewMatrix.translate(movement.getX(), movement.getY(), movement.getZ());

            glMatrixMode(GL_MODELVIEW);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            animationController.update(totalRuntime);

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
        meshRenderer.drawMesh(senFile.meshes.get(meshIdx));
    }

    private void renderMany(float angle) {
        List<SenMesh> meshes = senFile.meshes;
        for (int i = 0; i < meshes.size(); i++) {
            String meshName = meshes.get(i).name;
            if (meshName.charAt(0) == '_' || !meshName.contains("LAMP")) {
                continue;
            }

            ObjiElement obji = senFile.obji.elements[meshIdx];
            modelMatrix.translation(
                    SenMeshRenderer.SCALE * obji.x,
                    i * .3f,
                    SenMeshRenderer.SCALE * obji.z
            ).rotateY(angle * (float) Math.toRadians(90));
            glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
            meshRenderer.drawMesh(meshes.get(i));
        }
    }

    public static void main(String[] args) throws IOException {
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/OBJECTS.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/AQUAMALL.SEN");
        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/MALL1_ICA.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/CHARACTERS.SEN");

        File animDir = new File(Util.ROOT_DIR + "anim/");
        File[] animFiles = animDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".anm"));
        if (animFiles == null) {
            animFiles = new File[0];
        }
        Arrays.sort(animFiles);

        int animIdx = 0;
        for (int i = 0; i < animFiles.length; i++) {
            File animFile = animFiles[i];
            if (animFile.getName().equals("s_winner.anm")) {
                animIdx = i;
                break;
            }
        }


        new SenFileRenderer(senFile, animFiles, animIdx).run();
    }
}
