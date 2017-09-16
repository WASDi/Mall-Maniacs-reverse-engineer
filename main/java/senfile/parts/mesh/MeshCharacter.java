package senfile.parts.mesh;

import senfile.factories.VerticesFactory;

public class MeshCharacter extends SenMesh {

    public final int constant8; // == 280 (0x118)
    public final int constant9; // == 0xFF430000
    public final int constant10; // == 65529 (0xFFF9)
    public final int constant11; // == -20971520 (0xFEC00000)
    public final int constant12; // == 20447210 (0x137FFEA)
    public final int boolean1; // == 0xFF57FF4C or 0xFF57FF4D
    public final int constant13; // == 65502 (0xFFDE)
    public final int constant14; // == 0x00D20000
    public final int constant15; // == 20250624 (0x1350000)
    public final int constant16; // == 0xB50000
    public final int constant17; // == 20250624 (0x1350000)
    public final int boolean2; // == 0xFF5700B3 or 0xFF5700B4
    public final int constant18; // == 0x77F9FFDE
    public final int constant19; // == 0x00D20000
    public final int constant20; // == 0x01350000
    public final int constant21; // == 0x00B50000
    public final int constant22; // == 0x77BF0000
    public final int constant23; // == 0x004F0000
    public final int boolean3; // == 0x7782FFEC or 0x7782FFED
    public final int boolean4; // == 0xFFA6 or 0xFFA7
    public final int constant24; // == 0
    public final int boolean5; // == 0x017CFFEC or 0x017CFFED
    public final int constant25; // == 0x77E10000
    public final int constant26; // == 0x0153FFF7
    public final int constant27; // == 0x0138FFBE
    public final int boolean6; // == 0x59 or 0x5A
    public final int constant28; // == 0x01380000
    public final int boolean7; // == 0x017C0013 or 0x017C0014
    public final int constant29; // == 0x00890000
    public final int constant30; // == 0x01530009
    public final int constant31; // == 0x0135FFBE
    public final int constant32; // == 0
    public final int constant33; // == 1
    public final int constant34; // == 1
    public final int constant35; // == 3
    public final int constant36; // == 4
    public final int constant37; // == 1
    public final int constant38; // == 6
    public final int constant39; // == 7
    public final int constant40; // == 0
    public final int constant41; // == 0
    public final int constant42; // == 0xA
    public final int constant43; // == 0xB
    public final int constant44; // == 0
    public final int constant45; // == 0xD
    public final int constant46; // == 0xE
    public final int constant47; // == 10000000
    public final int numVertices;
    public final int constant48; // == 280
    public final int constant49; // == 0
    public final int ok_63; // == 280 + numVertices*8
    public final int numSuboReferences;
    public final int ok_65; // == 280 + numVertices*8
    public final int numVertexGroups;
    public final int _67; // 724 to 1280, dividable by 4. Very likely like _22 for object
    public final int constant50; // == 0
    public final int constant51; // == 0
    public final int ok_70; // == 280 + numVertices*8
    public final Vertex[] vertices;
    public final int[] suboReferences;
    public final VertexGroupDefinition[] vertexGroupDefinitions;

    public Vertex[][] groupedVertices;

    public MeshCharacter(String name, int meshIdx, int[] rawData) {
        super(name, meshIdx, rawData);
        int idx = SHARED_DATA_SIZE;
        constant8 = rawData[idx++];
        constant9 = rawData[idx++];
        constant10 = rawData[idx++];
        constant11 = rawData[idx++];
        constant12 = rawData[idx++];
        boolean1 = rawData[idx++];
        constant13 = rawData[idx++];
        constant14 = rawData[idx++];
        constant15 = rawData[idx++];
        constant16 = rawData[idx++];
        constant17 = rawData[idx++];
        boolean2 = rawData[idx++];
        constant18 = rawData[idx++];
        constant19 = rawData[idx++];
        constant20 = rawData[idx++];
        constant21 = rawData[idx++];
        constant22 = rawData[idx++];
        constant23 = rawData[idx++];
        boolean3 = rawData[idx++];
        boolean4 = rawData[idx++];
        constant24 = rawData[idx++];
        boolean5 = rawData[idx++];
        constant25 = rawData[idx++];
        constant26 = rawData[idx++];
        constant27 = rawData[idx++];
        boolean6 = rawData[idx++];
        constant28 = rawData[idx++];
        boolean7 = rawData[idx++];
        constant29 = rawData[idx++];
        constant30 = rawData[idx++];
        constant31 = rawData[idx++];
        constant32 = rawData[idx++];
        constant33 = rawData[idx++];
        constant34 = rawData[idx++];
        constant35 = rawData[idx++];
        constant36 = rawData[idx++];
        constant37 = rawData[idx++];
        constant38 = rawData[idx++];
        constant39 = rawData[idx++];
        constant40 = rawData[idx++];
        constant41 = rawData[idx++];
        constant42 = rawData[idx++];
        constant43 = rawData[idx++];
        constant44 = rawData[idx++];
        constant45 = rawData[idx++];
        constant46 = rawData[idx++];
        constant47 = rawData[idx++];
        numVertices = rawData[idx++];
        constant48 = rawData[idx++];
        constant49 = rawData[idx++];
        ok_63 = rawData[idx++];
        numSuboReferences = rawData[idx++];
        ok_65 = rawData[idx++];
        numVertexGroups = rawData[idx++];
        _67 = rawData[idx++];
        constant50 = rawData[idx++];
        constant51 = rawData[idx++];
        ok_70 = rawData[idx++];

        vertices = VerticesFactory.parseVertices(idx, numVertices, rawData);
        idx += numVertices * 2;

        suboReferences = new int[numSuboReferences];
        System.arraycopy(rawData, idx, suboReferences, 0, numSuboReferences);
        idx += numSuboReferences;

        vertexGroupDefinitions = new VertexGroupDefinition[numVertexGroups];
        int totalIntsLeft = parseVertexGroups(name, rawData, idx);

//        groupVertices();

        idx += totalIntsLeft;
        int remainingInts = rawData.length - idx;
        if (remainingInts != 0) {
            throw new IllegalStateException("Parsed character but has " + remainingInts + " left for " + name);
        }
    }

    private void groupVertices() {
        //FIXME useless since vertex group index used for animation is lost
        groupedVertices = new Vertex[numVertexGroups][];
        int i = 0;
        for (int x = 0; x < vertexGroupDefinitions.length; x++) {
            VertexGroupDefinition vertexGroup = vertexGroupDefinitions[x];
            groupedVertices[x] = new Vertex[vertexGroup.size];
            for (int y = 0; y < vertexGroup.size; y++) {
                groupedVertices[x][y] = vertices[i++];
            }
        }
    }

    private int parseVertexGroups(String name, int[] rawData, int idx) {
        int totalIntsLeft = rawData.length - idx;
        int expectedNumVertexGroups = totalIntsLeft / 3;
        if (vertexGroupDefinitions.length != expectedNumVertexGroups) {
            throw new IllegalStateException("vertexGroupDefinitions has " + vertexGroupDefinitions.length
                                                    + " elements but expectedNumVertexGroups is " + expectedNumVertexGroups
                                                    + " for character " + name);
        }

        for (int i = 0; i < vertexGroupDefinitions.length; i++) {
            int size = rawData[idx + i * 3];
            int constant0 = rawData[idx + i * 3 + 1];
            int index = rawData[idx + i * 3 + 2];
            VertexGroupDefinition ending3Ints = new VertexGroupDefinition(size, constant0, index);
            vertexGroupDefinitions[i] = ending3Ints;
        }
        return totalIntsLeft;
    }

    @Override
    public Vertex[] getVertices() {
        return vertices;
    }

    @Override
    public int[] getSuboOffsets() {
        return suboReferences;
    }
}
