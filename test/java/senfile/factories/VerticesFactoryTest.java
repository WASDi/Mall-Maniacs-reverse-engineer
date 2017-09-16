package senfile.factories;

import org.junit.Test;
import senfile.parts.mesh.Vertex;

import static org.junit.Assert.assertEquals;

public class VerticesFactoryTest {


    @Test
    public void testReverse() {
        int part1 = -17039150;
        int part2 = -2;
        Vertex v = new Vertex(210, -260, -2);

        assertEquals(part1, VerticesFactory.reversePart1(v));
        assertEquals(part2, VerticesFactory.reversePart2(v));
    }

    @Test
    public void testReverseWithNegatives() {

        int part1 = -196545464;

        Vertex v = new Vertex(-3000, -3000, 0);

        assertEquals(part1, VerticesFactory.reversePart1(v));
    }

    @Test
    public void parseVertexV2_only_negatives() {
        int part1 = -10420403;
        int part2 = -112;

        Vertex vertexV2 = VerticesFactory.parseVertexV2(part1, part2);

        assertEquals(-179, vertexV2.x);
        assertEquals(-160, vertexV2.y);
        assertEquals(-112, vertexV2.z);
    }

    @Test
    public void parseVertexV2_only_positives() {
        int part1 = 13697083;
        int part2 = -65504;

        Vertex vertexV2 = VerticesFactory.parseVertexV2(part1, part2);

        assertEquals(59, vertexV2.x);
        assertEquals(209, vertexV2.y);
        assertEquals(32, vertexV2.z);
    }

    @Test
    public void parseVertexV2_NegPos() {
        int part1 = 12648407;
        int part2 = -21;

        Vertex vertexV2 = VerticesFactory.parseVertexV2(part1, part2);

        assertEquals(-41, vertexV2.x);
        assertEquals(192, vertexV2.y);
        assertEquals(-21, vertexV2.z);
    }

}