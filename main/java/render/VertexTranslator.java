package render;

public class VertexTranslator {

    // SenFiles use inverted X and Y

    private static final float SCALE = 1 / 1000f;

    public static float translateX(float x) {
        return x * -SCALE;
    }

    public static float translateY(float y) {
        return y * -SCALE;
    }

    public static float translateZ(float z) {
        return z * SCALE;
    }

}
