package org.lwjglb.game;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjglb.engine.GameItem;
import org.lwjglb.engine.IGameLogic;
import org.lwjglb.engine.MouseInput;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.Camera;
import org.lwjglb.engine.graph.Mesh;
import render.LwjglMeshCreator;
import render.VertexTranslator;
import senfile.SenFile;
import senfile.factories.SenFileFactory;
import senfile.parts.elements.ObjiElement;
import senfile.parts.mesh.SenMesh;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class DummyGame implements IGameLogic {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final Camera camera;

    private List<GameItem> gameItems = new ArrayList<>();
    private List<SenMesh> meshesToRender;
    private List<Vector3f> meshesToRenderPos;

    private static final float CAMERA_POS_STEP = 0.1f;

    public DummyGame() {
        renderer = new Renderer();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
    }

    @Override
    public void init(Window window) throws Exception {
        renderer.init(window);
//        addCube(0, 0, -2);
//        addCube(0.5f, 0.5f, -2);
//        addCube(0, 0, -2.5f);
//        addCube(0.5f, 0, -2.5f);


//        GL11.glEnable(GL11.GL_CULL_FACE);

//        addCube(0, 0, -2, TriCube.MESH);
//
//        addCube(0, 0, 1, TriCube.MESH);
//
//        addCube(-1, 0, 0, TriCube.MESH);
//        addCube(0, 1, 0, TriCube.MESH);
//        addCube(1, 0, 0, TriCube.MESH);
//        addCube(2, 0, 0, TriCube.MESH);


        addAllTheThings();
    }

    private void addAllTheThings() {

        SenFile senFile = SenFileFactory.icaSingleton();
//        SenFile senFile = SenFileFactory.aquaSingleton();

        List<SenMesh> meshes = senFile.getMeshes();

        meshesToRender = new ArrayList<>();
        meshesToRenderPos = new ArrayList<>();
        for (SenMesh mesh : meshes) {
            if (!mesh.isUnderscore()) {
                meshesToRender.add(mesh);
            }
        }

        for (SenMesh mesh : meshesToRender) {
            ObjiElement obji = senFile.getObji().elements[mesh.meshIdx];

            GameItem item = new GameItem(LwjglMeshCreator.crateMeshFromSenMesh(senFile, mesh));

            float x = VertexTranslator.translateX(obji.x);
            float y = VertexTranslator.translateY(obji.y);
            float z = VertexTranslator.translateZ(obji.z);

            item.setPosition(x, y, z);
            gameItems.add(item);
            meshesToRenderPos.add(new Vector3f(x, y, z));

            GameItem smallCube = new GameItem(TriCube.MESH);
            smallCube.setScale(0.1f);
            smallCube.setPosition(x, y, z);
            gameItems.add(smallCube);
        }
    }

    private void addCube(float x, float y, float z, Mesh mesh) {
        GameItem item = new GameItem(mesh);
        item.setScale(0.5f);
        item.setPosition(x, y, z);
        gameItems.add(item);
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
                debugClosestItem();
            }
        } else {
            noPressY = true;
        }

    }

    boolean noPressY = true;

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
        renderer.render(window, camera, gameItems);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
        for (GameItem gameItem : gameItems) {
            gameItem.getMesh().cleanUp();
        }
    }

}
