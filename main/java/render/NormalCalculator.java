package render;

import org.joml.Vector3f;
import senfile.parts.mesh.Vertex;

public class NormalCalculator {

    // https://stackoverflow.com/questions/1966587/given-3-pts-how-do-i-calculate-the-normal-vector

    public static Vector3f calculate(Vertex vertex1, Vertex vertex2, Vertex vertex3) {
        Vector3f a = asVector3f(vertex1);
        Vector3f b = asVector3f(vertex2);
        Vector3f c = asVector3f(vertex3);

        Vector3f b_a = new Vector3f();
        Vector3f c_a = new Vector3f();
        b.sub(a, b_a);
        c.sub(a, c_a);

        Vector3f cross = new Vector3f();
        b_a.cross(c_a, cross);

        return cross.normalize();
    }

    private static Vector3f asVector3f(Vertex vertex) {
        return new Vector3f(vertex.x, vertex.y, vertex.z);
    }
}
