package render;

import anmfile.AnmFile;
import anmfile.factories.AnmFileFactory;
import anmfile.parts.Keyframe;
import anmfile.parts.MeshEntry;
import anmfile.parts.Track;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
import senfile.parts.mesh.VertexGroupDefinition;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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

    private static final int NUM_MERGED_TPG_FILES = 32;
    private final int[] texture_MERGED = new int[NUM_MERGED_TPG_FILES];

    private final SenFile senFile;
    private final AnmFile anmFile;
    private int meshIdx = 29;
    private int debug = 0;

    private final Movement movement = new Movement(0f, 7f, 22f);

    public SenFileRenderer(SenFile senFile, AnmFile anmFile) {
        this.senFile = senFile;
        this.anmFile = anmFile;
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

        GL.createCapabilities();

        for (int i = 0; i < NUM_MERGED_TPG_FILES; i++) {
            initMergedTpgTexture(i);
        }
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

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TpgImage.SIZE, TpgImage.SIZE, 0, GL_RGB, GL_UNSIGNED_BYTE, pixels);
    }

    // Engine angle units: 65536 = 360 degrees
    private static final float ANGLE_UNITS_TO_RAD = (float) (2.0 * Math.PI / 65536.0);

    /**
     * Extract the (offset_x, offset_y, offset_z) rest-pose pivot for sub-object at
     * subObjIdx (1-based; root=0 has pivot (0,0,0)).
     *
     * MeshCharacter.subTransformX/Y/Z are arrays of 15 shorts indexed by (subObjIdx-1),
     * matching parse_sen.py parse_sub_object_table: struct.unpack_from('<hhh', data,
     * sub_transform_offset + i*8) for i in range(num_extra_sub).
     */
    private static Vector3f getSubObjectPivot(MeshCharacter character, int subObjIdx) {
        if (subObjIdx <= 0 || subObjIdx > 15) {
            return new Vector3f(0, 0, 0);
        }
        int i = subObjIdx - 1;
        return new Vector3f(character.subTransformX[i], character.subTransformY[i], character.subTransformZ[i]);
    }

    /**
     * Build a per-vertex animated position array for a character mesh, driven by the
     * ANM track selected by 'debug'.
     *
     * Algorithm:
     *  1. Each ANM MeshEntry has a name (e.g. "roland") and a frameCount which is
     *     actually the sub_obj_idx — the sub-object slot this entry controls.
     *  2. Opcode-5 keyframes in the selected track each have:
     *       targetIndex = index into anmFile.meshes list
     *       u/v/w = rot_x, rot_y, rot_z in engine units (65536 = 360°)
     *  3. For each vertex, look up its controlling sub_obj_idx via:
     *       group = vertexId2Group[vertexId]
     *       subObjIdx = vertexGroupDefinitions[group].index
     *  4. Look up the rotation for that sub_obj_idx from the selected track.
     *  5. Rotate the vertex around the sub-object's pivot point.
     *
     * Rotation order mirrors sub_431110: Rz * Rx * Ry (ZXY Euler).
     * No axis negation (opcode 5 does not negate any axis).
     */
    private Vector3f[] buildAnimatedVertices(MeshCharacter character) {
        Vertex[] vertices = character.getVertices();
        Vector3f[] result = new Vector3f[vertices.length];

        // Select track: debug selects a track index (clamped to valid range)
        int trackCount = anmFile.tracks().size();
        if (trackCount == 0) {
            for (int i = 0; i < vertices.length; i++) {
                result[i] = new Vector3f(vertices[i].x, vertices[i].y, vertices[i].z);
            }
            return result;
        }
        int trackIdx = Math.floorMod(debug, trackCount);
        Track track = anmFile.tracks().get(trackIdx);

        // Build a lookup: sub_obj_idx → rotation matrix (from keyframe u/v/w)
        // We have up to 16 sub-objects (0-15)
        Matrix4f[] subObjRotations = new Matrix4f[16];
        for (int i = 0; i < 16; i++) {
            subObjRotations[i] = new Matrix4f(); // identity = no rotation
        }

        // Scan all opcode-5 keyframes in this track
        for (Keyframe kf : track.keyframes()) {
            if (kf.opcode() != 5) continue;
            if (kf.targetIndex() == null || kf.u() == null) continue;

            int meshEntryIdx = kf.targetIndex();
            if (meshEntryIdx < 0 || meshEntryIdx >= anmFile.meshes().size()) continue;

            // frameCount is actually the sub_obj_idx
            int subObjIdx = anmFile.meshes().get(meshEntryIdx).frameCount();
            if (subObjIdx < 0 || subObjIdx >= 16) continue;

            // Convert engine angle units → radians
            float rx = kf.u() * ANGLE_UNITS_TO_RAD;
            float ry = kf.v() * ANGLE_UNITS_TO_RAD;
            float rz = kf.w() * ANGLE_UNITS_TO_RAD;

            // Rotation order: Rz * Rx * Ry  (ZXY Euler, as in sub_431110)
            // No axis negation for opcode 5
            Matrix4f rot = new Matrix4f()
                    .rotateZ(rz)
                    .rotateX(rx)
                    .rotateY(ry);
            subObjRotations[subObjIdx] = rot;
        }

        // Apply rotations to each vertex
        for (int i = 0; i < vertices.length; i++) {
            int group = character.vertexId2Group[i];
            VertexGroupDefinition vgd = character.vertexGroupDefinitions[group];
            int subObjIdx = vgd.index;

            Vertex v = vertices[i];
            float vx = v.x, vy = v.y, vz = v.z;

            Matrix4f rot = subObjRotations[subObjIdx < 16 ? subObjIdx : 0];

            // Check if this rotation is non-identity by seeing if it differs from default
            boolean isIdentity = (subObjIdx >= 16 || subObjRotations[subObjIdx] == null
                    || subObjRotations[subObjIdx].equals(new Matrix4f()));
            if (!isIdentity) {
                // Get pivot = rest-pose offset of this sub-object
                Vector3f pivot = getSubObjectPivot(character, subObjIdx);

                // Translate to pivot-local space
                float lx = vx - pivot.x;
                float ly = vy - pivot.y;
                float lz = vz - pivot.z;

                // Apply rotation
                Vector3f rotated = new Vector3f(lx, ly, lz).mulDirection(rot);

                // Translate back
                vx = rotated.x + pivot.x;
                vy = rotated.y + pivot.y;
                vz = rotated.z + pivot.z;
            }

            result[i] = new Vector3f(vx, vy, vz);
        }

        return result;
    }

    private void renderFromSenFile(int meshIdx) {
        SenMesh mesh = senFile.meshes.get(meshIdx);
        int[] suboOffsets = mesh.getSuboOffsets();
        Vertex[] vertices = mesh.getVertices();

        // For character meshes, build animated vertex positions driven by the ANM file
        Vector3f[] animatedVertices = null;
        if (mesh instanceof MeshCharacter character && anmFile != null) {
            animatedVertices = buildAnimatedVertices(character);
        }

        for (int suboOffset : suboOffsets) {
            SuboElement.FaceInfo[] faceInfos = senFile.subo.elementByOffset(suboOffset).faceInfos;

            for (SuboElement.FaceInfo faceInfo : faceInfos) {
                byte[] vertexIndices = faceInfo.vertexIndices;
                int v0 = vertexIndices[0] & 0xFF;
                int v1 = vertexIndices[1] & 0xFF;
                int v2 = vertexIndices[2] & 0xFF;
                int v3 = vertexIndices[3] & 0xFF;

                int textureIndexForFace = faceInfo.getMapiIndex();
                MapiElement mapiElement = senFile.mapi.elements[textureIndexForFace];
                int mergedTpgFileIndex = mapiElement.materialIndex;
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
                putAnimatedVertex(vertices[v0], animatedVertices, v0);

                glTexCoord2f(v1tx, v1ty);
                putAnimatedVertex(vertices[v1], animatedVertices, v1);

                glTexCoord2f(v2tx, v2ty);
                putAnimatedVertex(vertices[v2], animatedVertices, v2);

                // --- //

                glTexCoord2f(v3tx, v3ty);
                putAnimatedVertex(vertices[v3], animatedVertices, v3);

                glTexCoord2f(v0tx, v0ty);
                putAnimatedVertex(vertices[v0], animatedVertices, v0);

                glTexCoord2f(v2tx, v2ty);
                putAnimatedVertex(vertices[v2], animatedVertices, v2);

                glEnd();
            }
        }
    }

    private static final float SCALE = .002f;

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

    private void loop() {
        // Set the clear color
        glClearColor(0.6f, 0.7f, 0.8f, 1.0f);
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
//        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);

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
            modelMatrix.translation(
                    SCALE * obji.x,
                    i * .3f,
                    SCALE * obji.z
            ).rotateY(angle * (float) Math.toRadians(90));
            glLoadMatrixf(viewMatrix.mul(modelMatrix, modelViewMatrix).get(fb));
            renderFromSenFile(i);
        }
    }

    public static void main(String[] args) throws IOException {
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/OBJECTS.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_aqua/AQUAMALL.SEN");
//        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/MALL1_ICA.SEN");
        SenFile senFile = SenFileFactory.fromFile(Util.ROOT_DIR + "scene_ica/CHARACTERS.SEN");
        AnmFile anmFile = AnmFileFactory.fromFile(Util.ROOT_DIR + "anim/s_run.anm");
        new SenFileRenderer(senFile, anmFile).run();
    }
}
