package render;

import org.joml.Vector3f;
import senfile.parts.mesh.Vertex;

public class NormalCalculator {

    // https://stackoverflow.com/questions/1966587/given-3-pts-how-do-i-calculate-the-normal-vector

    public static Vector3f calculate(Vertex vertex1, Vertex vertex2, Vertex vertex3) {
        Vector3f a = VertexTranslator.translate(vertex1);
        Vector3f b = VertexTranslator.translate(vertex2);
        Vector3f c = VertexTranslator.translate(vertex3);

        Vector3f bSubA = new Vector3f();
        Vector3f cSubA = new Vector3f();
        b.sub(a, bSubA);
        c.sub(a, cSubA);

        Vector3f cross = new Vector3f();
        cSubA.cross(bSubA, cross);

        return cross.normalize();
    }

}
