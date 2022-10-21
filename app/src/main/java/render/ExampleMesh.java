package render;

import org.joml.Vector3f;

public class ExampleMesh {

    public static final Vector3f[] VERTICES = {
            getScaledVertex(4, 5, 300),
            getScaledVertex(4, 5, -225),
            getScaledVertex(-31, 5, 300),
            getScaledVertex(-31, 5, -225),
            getScaledVertex(4, -220, 300),
            getScaledVertex(4, -220, -225),
            getScaledVertex(-31, -220, 300),
            getScaledVertex(-31, -220, -225),
    };

    public static final float VERTEX_SCALE = .01f;

    private static Vector3f getScaledVertex(int x, int y, int z) {
        return new Vector3f(x * VERTEX_SCALE,
                            y * VERTEX_SCALE,
                            z * VERTEX_SCALE);
    }

    public static final int[] FACES = {
            2, 3, 1, 0,
            3, 7, 5, 1,
            7, 6, 4, 5,
            6, 2, 0, 4,
            0, 1, 5, 4,
            6, 7, 3, 2
    };

    public static final int[] TEXTURE_INDICES = {
            0, 1, 1, 2, 3, 4
    };

    public static final int[] TEXTURE_COORDS = {
            192, 161, 255, 161, 255, 166, 192, 165,
            244, 188, 236, 188, 236, 181, 244, 181,
            236, 181, 244, 181, 244, 188, 236, 188,
            192, 165, 255, 166, 255, 191, 192, 191,
            192, 128, 255, 128, 255, 161, 192, 161
    };


}
