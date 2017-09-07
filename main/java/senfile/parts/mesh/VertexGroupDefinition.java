package senfile.parts.mesh;

public class VertexGroupDefinition {
    public final int size; // 1 to 56
    public final int constant0;
    public final int index; // 0 to 15

    public VertexGroupDefinition(int size, int constant0, int index) {
        this.size = size;
        this.constant0 = constant0;
        this.index = index;
    }
}
