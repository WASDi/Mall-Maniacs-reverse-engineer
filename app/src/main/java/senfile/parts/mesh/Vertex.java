package senfile.parts.mesh;

public class Vertex {

    public final int x;
    public final int y;
    public final int z;

    public Vertex(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("[%d, %d, %d]", x, y, z);
    }

    public String toCodeString() {
        return String.format("new Vertex(%d, %d, %d)", x, y, z);

    }
}
