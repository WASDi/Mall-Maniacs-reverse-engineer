package render;

import org.joml.Vector3f;
import senfile.parts.mesh.Vertex;

public class VertexTranslator {

    private static final float SCALE = 1 / 1000f;
    private static final float ROTATION_FACTOR = 180f / Short.MAX_VALUE;

    public static float translateX(float x) {
        return x * -SCALE;
    }

    public static float translateY(float y) {
        return y * -SCALE;
    }

    public static float translateZ(float z) {
        return z * SCALE;
    }

    public static Vector3f translate(Vertex vertex) {
        return new Vector3f(
                VertexTranslator.translateX(vertex.x),
                VertexTranslator.translateY(vertex.y),
                VertexTranslator.translateZ(vertex.z)
        );
    }

    public static float translateRotation(short rotation) {
        return rotation * ROTATION_FACTOR;
    }
}
