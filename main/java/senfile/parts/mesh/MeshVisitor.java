package senfile.parts.mesh;

public interface MeshVisitor {

    void visit(MeshObject mesh);

    void visit(MeshCharacter mesh);

}
