package org.lwjglb.game;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjglb.engine.GameItem;
import org.lwjglb.engine.IGameLogic;
import org.lwjglb.engine.MouseInput;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.Camera;
import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.graph.Texture;
import render.LwjglMeshCreator;
import render.VertexTranslator;
import render.texture.TextureAtlas;
import senfile.GameMap;
import senfile.SenFile;
import senfile.Util;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.ObjiElement;
import senfile.parts.mesh.SenMesh;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class DummyGame implements IGameLogic {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final Camera camera;

    private GameItemContainer gameItemContainer = new GameItemContainer();
    private List<SenMesh> meshesToRender;
    private List<Vector3f> meshesToRenderPos;

    private static final float CAMERA_POS_STEP = 0.2f;
    private static final boolean ADD_DEBUG_CUBES = false;
    private static final boolean RENDER_UNDERSCORES = false;

    public DummyGame() {
        renderer = new Renderer();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
    }

    @Override
    public void init(Window window) throws Exception {
        renderer.init(window);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        SenFile senFile = SenFileFactory.getMap(GameMap.SELECTED_MAP);
        addAllTheThings(senFile);
    }

    private void addAllTheThings(SenFile senFile) {

        if (RENDER_UNDERSCORES) {
            meshesToRender = senFile.meshes;
        } else {
            meshesToRender = new ArrayList<>();
            for (SenMesh mesh : senFile.meshes) {
                if (!mesh.ignoreBecauseUnderline()) {
                    meshesToRender.add(mesh);
                }
            }
        }

        meshesToRenderPos = new ArrayList<>();
        Texture textureAtlas = TextureAtlas.atlasFromTnam(senFile.tnam).toLwjglTexture();
        for (SenMesh mesh : meshesToRender) {
            ObjiElement obji = senFile.obji.elements[mesh.meshIdx];

            GameItem item;
            try {
                item = new GameItem(LwjglMeshCreator.crateMeshFromSenMesh(senFile, mesh, textureAtlas), mesh.meshIdx);
            } catch (RuntimeException ex) {
                meshesToRenderPos.add(new Vector3f(9999));
                System.err.println("Erroneous mesh: " + mesh.name);
                ex.printStackTrace();
                continue;
            }

            float x = VertexTranslator.translateX(obji.x);
            float y = VertexTranslator.translateY(obji.y);
            float z = VertexTranslator.translateZ(obji.z);

            item.setPosition(x, y, z);
            item.setRotation(VertexTranslator.translateRotation(obji.rotX),
                             VertexTranslator.translateRotation(obji.rotY),
                             VertexTranslator.translateRotation(obji.rotZ));

            boolean hasTransparency = Util.hasTransparency(mesh, senFile);
            gameItemContainer.addGameItem(item, hasTransparency);
            meshesToRenderPos.add(new Vector3f(x, y, z));

            if (ADD_DEBUG_CUBES) {
                addCube(x, y, z, TriCube.getMesh());
            }
        }
    }

    private void addCube(float x, float y, float z, Mesh mesh) {
        GameItem item = new GameItem(mesh, -1);
        item.setScale(0.1f);
        item.setPosition(x, y, z);
        gameItemContainer.addGameItem(item, false);
    }

    @Override
    public void input(Window window, MouseInput mouseInput) {
        cameraInc.set(0, 0, 0);

        if (window.isKeyPressed(GLFW_KEY_W)) {
            cameraInc.z = -1;
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            cameraInc.z = 1;
        }

        if (window.isKeyPressed(GLFW_KEY_A)) {
            cameraInc.x = -1;
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            cameraInc.x = 1;
        }

        if (window.isKeyPressed(GLFW_KEY_Z)) {
            cameraInc.y = -1;
        } else if (window.isKeyPressed(GLFW_KEY_X)) {
            cameraInc.y = 1;
        }


        if (window.isKeyPressed(GLFW_KEY_Y)) {
            if (noPressY) {
                noPressY = false;
                //debugClosestItem();
                lightPosition.set(camera.getPosition());
            }
        } else {
            noPressY = true;
        }

    }

    boolean noPressY = true;

    public static Vector3f lightPosition = new Vector3f();

    private void debugClosestItem() {
        Vector3f myPos = camera.getPosition();
        float minDistance = Float.MAX_VALUE;
        SenMesh closestMesh = null;

        for (int i = 0; i < meshesToRender.size(); i++) {
            SenMesh mesh = meshesToRender.get(i);
            Vector3f pos = meshesToRenderPos.get(i);
            float distance = myPos.distance(pos);
            if (distance < minDistance) {
                minDistance = distance;
                closestMesh = mesh;
            }
        }

        System.out.println(closestMesh.name);
    }

    @Override
    public void update(float interval, MouseInput mouseInput) {
        // Update camera position
        camera.movePosition(cameraInc.x * CAMERA_POS_STEP, cameraInc.y * CAMERA_POS_STEP, cameraInc.z * CAMERA_POS_STEP);

        // Update camera based on mouse            
        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);
        }
    }

    @Override
    public void render(Window window) {
        gameItemContainer.preRender(camera.getPosition());
        renderer.render(window, camera, gameItemContainer);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
        gameItemContainer.cleanup();
    }

}
