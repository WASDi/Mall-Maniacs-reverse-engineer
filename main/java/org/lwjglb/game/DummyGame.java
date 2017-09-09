package org.lwjglb.game;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjglb.engine.GameItem;
import org.lwjglb.engine.IGameLogic;
import org.lwjglb.engine.MouseInput;
import org.lwjglb.engine.Window;
import org.lwjglb.engine.graph.Camera;
import org.lwjglb.engine.graph.IMesh;
import render.LwjglMeshCreator;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class DummyGame implements IGameLogic {

    private static final float MOUSE_SENSITIVITY = 0.2f;

    private final Vector3f cameraInc;

    private final Renderer renderer;

    private final Camera camera;

    private List<GameItem> gameItems = new ArrayList<>();

    private static final float CAMERA_POS_STEP = 0.05f;

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

        addCube(0, 0, -2, TriCube.MESH);

        addCube(0, 0, -1, QuadCube.MY_MESH);
        addCube(0, -1, -1, TriCube.MY_MESH);

        addCube(0, 0, 1, TriCube.MESH);

        addCube(-1, 0, 0, TriCube.MESH);
        addCube(0, 1, 0, TriCube.MESH);
        addCube(1, 0, 0, TriCube.MESH);
        addCube(2, 0, 0, TriCube.MESH);

        meshFromTranslatedSen();
    }

    private void meshFromTranslatedSen() {
        GameItem item = new GameItem(LwjglMeshCreator.makeMeAMesh(44));
        item.setPosition(2, 1, 0);
        gameItems.add(item);
    }

    private void addCube(float x, float y, float z, IMesh mesh) {
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
