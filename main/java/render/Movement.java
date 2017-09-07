package render;

import static org.lwjgl.glfw.GLFW.*;

// TODO https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter8/chapter8.html
public class Movement {

    private float x;
    private float y;
    private float z;

    private boolean wDown;
    private boolean aDown;
    private boolean sDown;
    private boolean dDown;
    private boolean spaceDown;
    private boolean ctrlDown;


    public void handleMovement(int key, boolean press) {
        switch (key) {
            case GLFW_KEY_W:
                wDown = press;
                break;
            case GLFW_KEY_A:
                aDown = press;
                break;
            case GLFW_KEY_S:
                sDown = press;
                break;
            case GLFW_KEY_D:
                dDown = press;
                break;
            case GLFW_KEY_SPACE:
                spaceDown = press;
                break;
            case GLFW_KEY_LEFT_CONTROL:
                ctrlDown = press;
                break;
        }
    }

    public void step(float step) {
        if (wDown) {
            z -= step;
        }
        if (aDown) {
            x -= step;
        }
        if (sDown) {
            z += step;
        }
        if (dDown) {
            x += step;
        }
        if (spaceDown) {
            y += step;
        }
        if (ctrlDown) {
            y -= step;
        }

    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
