package senfile.parts.mesh;

import senfile.factories.VerticesFactory;

import java.util.HashMap;
import java.util.Map;

public class MeshCharacter extends SenMesh {

    public final int meshDataBaseSize; // == 280 (0x118) byte offset to vertex data from mesh start

    public final int boneTransform_0_0; // == 0xFF430000
    public final int boneTransform_0_1; // == 65529 (0xFFF9)
    public final int boneTransform_0_2; // == -20971520 (0xFEC00000)
    public final int boneTransform_1_0; // == 20447210 (0x137FFEA)
    public final int boneTransform_1_1; // == 0xFF57FF4C or 0xFF57FF4D, bone angle diff by 1 unit
    public final int boneTransform_1_2; // == 65502 (0xFFDE)
    public final int boneTransform_2_0; // == 0x00D20000
    public final int boneTransform_2_1; // == 20250624 (0x1350000)
    public final int boneTransform_2_2; // == 0xB50000
    public final int boneTransform_3_0; // == 20250624 (0x1350000)
    public final int boneTransform_3_1; // == 0xFF5700B3 or 0xFF5700B4, bone angle diff by 1 unit
    public final int boneTransform_3_2; // == 0x77F9FFDE
    public final int boneTransform_4_0; // == 0x00D20000
    public final int boneTransform_4_1; // == 0x01350000
    public final int boneTransform_4_2; // == 0x00B50000
    public final int boneTransform_5_0; // == 0x77BF0000
    public final int boneTransform_5_1; // == 0x004F0000
    public final int boneTransform_5_2; // == 0x7782FFEC or 0x7782FFED, bone angle diff by 1 unit
    public final int boneTransform_6_0; // == 0xFFA6 or 0xFFA7, -90/-89 deg as int16
    public final int boneTransform_6_1; // == 0
    public final int boneTransform_6_2; // == 0x017CFFEC or 0x017CFFED, bone angle diff by 1 unit
    public final int boneTransform_7_0; // == 0x77E10000
    public final int boneTransform_7_1; // == 0x0153FFF7
    public final int boneTransform_7_2; // == 0x0138FFBE
    public final int boneTransform_8_0; // == 0x59 or 0x5A, 89/90 deg as int16
    public final int boneTransform_8_1; // == 0x01380000
    public final int boneTransform_8_2; // == 0x017C0013 or 0x017C0014, bone angle diff by 1 unit
    public final int boneTransform_9_0; // == 0x00890000
    public final int boneTransform_9_1; // == 0x01530009
    public final int boneTransform_9_2; // == 0x0135FFBE
    public final int boneTransformPadding; // == 0

    public final int boneHierarchy_0; // == 1 bone type/parent ID
    public final int boneHierarchy_1; // == 1
    public final int boneHierarchy_2; // == 3
    public final int boneHierarchy_3; // == 4
    public final int boneHierarchy_4; // == 1
    public final int boneHierarchy_5; // == 6
    public final int boneHierarchy_6; // == 7
    public final int boneHierarchy_7; // == 0  (unused slot)
    public final int boneHierarchy_8; // == 0  (unused slot)
    public final int boneHierarchy_9; // == 0xA
    public final int boneHierarchy_10; // == 0xB
    public final int boneHierarchy_11; // == 0  (unused slot)
    public final int boneHierarchy_12; // == 0xD
    public final int boneHierarchy_13; // == 0xE

    public final int maxDrawDistance; // == 10000000
    public final int numVertices;
    public final int vertexDataOffset; // == 280
    public final int reserved0; // == 0
    public final int vertexDataEndOffset; // == 280 + numVertices*8
    public final int numSuboReferences;
    public final int suboRefsTableOffset; // == 280 + numVertices*8
    public final int numVertexGroups;
    public final int faceGroupsCount; // 724 to 1280, dividable by 4
    public final int reserved1; // == 0
    public final int reserved2; // == 0
    public final int vertexGroupDefOffset; // == 280 + numVertices*8
    public final Vertex[] vertices;
    public final int[] suboReferences;
    public final VertexGroupDefinition[] vertexGroupDefinitions;

    public Vertex[][] groupedVertices;
    public int[][] groupedVertexIds;
    public int[] vertexId2Group;

    public MeshCharacter(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        super(name, bytesLeftUntilName, meshIdx, rawData);
        int idx = SHARED_DATA_SIZE;
        meshDataBaseSize = rawData[idx++];
        boneTransform_0_0 = rawData[idx++];
        boneTransform_0_1 = rawData[idx++];
        boneTransform_0_2 = rawData[idx++];
        boneTransform_1_0 = rawData[idx++];
        boneTransform_1_1 = rawData[idx++];
        boneTransform_1_2 = rawData[idx++];
        boneTransform_2_0 = rawData[idx++];
        boneTransform_2_1 = rawData[idx++];
        boneTransform_2_2 = rawData[idx++];
        boneTransform_3_0 = rawData[idx++];
        boneTransform_3_1 = rawData[idx++];
        boneTransform_3_2 = rawData[idx++];
        boneTransform_4_0 = rawData[idx++];
        boneTransform_4_1 = rawData[idx++];
        boneTransform_4_2 = rawData[idx++];
        boneTransform_5_0 = rawData[idx++];
        boneTransform_5_1 = rawData[idx++];
        boneTransform_5_2 = rawData[idx++];
        boneTransform_6_0 = rawData[idx++];
        boneTransform_6_1 = rawData[idx++];
        boneTransform_6_2 = rawData[idx++];
        boneTransform_7_0 = rawData[idx++];
        boneTransform_7_1 = rawData[idx++];
        boneTransform_7_2 = rawData[idx++];
        boneTransform_8_0 = rawData[idx++];
        boneTransform_8_1 = rawData[idx++];
        boneTransform_8_2 = rawData[idx++];
        boneTransform_9_0 = rawData[idx++];
        boneTransform_9_1 = rawData[idx++];
        boneTransform_9_2 = rawData[idx++];
        boneTransformPadding = rawData[idx++];
        boneHierarchy_0 = rawData[idx++];
        boneHierarchy_1 = rawData[idx++];
        boneHierarchy_2 = rawData[idx++];
        boneHierarchy_3 = rawData[idx++];
        boneHierarchy_4 = rawData[idx++];
        boneHierarchy_5 = rawData[idx++];
        boneHierarchy_6 = rawData[idx++];
        boneHierarchy_7 = rawData[idx++];
        boneHierarchy_8 = rawData[idx++];
        boneHierarchy_9 = rawData[idx++];
        boneHierarchy_10 = rawData[idx++];
        boneHierarchy_11 = rawData[idx++];
        boneHierarchy_12 = rawData[idx++];
        boneHierarchy_13 = rawData[idx++];
        maxDrawDistance = rawData[idx++];
        numVertices = rawData[idx++];
        vertexDataOffset = rawData[idx++];
        reserved0 = rawData[idx++];
        vertexDataEndOffset = rawData[idx++];
        numSuboReferences = rawData[idx++];
        suboRefsTableOffset = rawData[idx++];
        numVertexGroups = rawData[idx++];
        faceGroupsCount = rawData[idx++];
        reserved1 = rawData[idx++];
        reserved2 = rawData[idx++];
        vertexGroupDefOffset = rawData[idx++];

        vertices = VerticesFactory.parseVertices(idx, numVertices, rawData);
        idx += numVertices * 2;

        suboReferences = new int[numSuboReferences];
        System.arraycopy(rawData, idx, suboReferences, 0, numSuboReferences);
        idx += numSuboReferences;

        vertexGroupDefinitions = new VertexGroupDefinition[numVertexGroups];
        int totalIntsLeft = parseVertexGroups(name, rawData, idx);

        groupVertices();

        idx += totalIntsLeft;
        int remainingInts = rawData.length - idx;
        if (remainingInts != 0) {
            throw new IllegalStateException("Parsed character but has " + remainingInts + " left for " + name);
        }
    }

    private void groupVertices() {
        //FIXME useless since vertex group index used for animation is lost
        groupedVertices = new Vertex[numVertexGroups][];
        groupedVertexIds = new int[numVertexGroups][];
        vertexId2Group = new int[numVertices];
        int i = 0;
        for (int x = 0; x < vertexGroupDefinitions.length; x++) {
            VertexGroupDefinition vertexGroup = vertexGroupDefinitions[x];
            groupedVertices[x] = new Vertex[vertexGroup.size];
            groupedVertexIds[x] = new int[vertexGroup.size];
            for (int y = 0; y < vertexGroup.size; y++) {
                groupedVertices[x][y] = vertices[i];
                groupedVertexIds[x][y] = i;
                vertexId2Group[i] = x;

                i++;
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

        // VONKEL     sizes (16): 4, 13, 20, 3, 4, 4, 3, 4, 4, 1, 6, 7, 4, 6, 7, 4
        // KASSOERSKA sizes (10): 4, 14, 13, 3, 4, 4, 3, 4, 4, 10
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

    @Override
    public void visit(MeshVisitor visitor) {
        visitor.visit(this);
    }
}
