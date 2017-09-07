package senfile.factories;

import senfile.parts.mesh.Vertex;

public class VerticesFactory {

    public static Vertex[] parseVertices(int start, int numVertices, int[] rawData) {
        Vertex[] vertices = new Vertex[numVertices];

        for (int i = 0; i < numVertices; i++) {
            int part1 = rawData[start + i * 2];
            int part2 = rawData[start + i * 2 + 1];
            vertices[i] = parseVertexV2(part1, part2);
        }

        return vertices;
    }

    public static Vertex parseVertex(int part1, int part2) {
        int x = part1 & 0xFF;
        int y = (part1 >> 16) & 0xFF;
        int z = part2 & 0xFF;

        return new Vertex(x, y, z);
    }

    public static Vertex parseVertexV2(int part1, int part2) {
        int x = (part1 << 16) >> 16;
        int y = part1 >> 16;
        int z = (part2 << 16) >> 16;

        return new Vertex(x, y, z);
    }

}
