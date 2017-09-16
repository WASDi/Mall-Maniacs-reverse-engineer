package senfile.factories;

import senfile.HeaderTexts;
import senfile.Util;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.MeshObject;
import senfile.parts.mesh.SenMesh;

import java.nio.ByteBuffer;

public class MeshFactory {

    public static SenMesh parseFromBufferPosition(int meshIdx, ByteBuffer buffer) {
        int bytesLeftUntilName = buffer.getInt();
        int nameOffset = buffer.position() + bytesLeftUntilName;

        int[] rawData = new int[bytesLeftUntilName / 4];

        for (int i = 0; i < rawData.length; i++) {
            rawData[i] = buffer.getInt();
        }

        String name = parseName(buffer, nameOffset);
        if (isCharacter(rawData)) {
            return new MeshCharacter(name, meshIdx, rawData);
        } else {
            return new MeshObject(name, meshIdx, rawData);
        }
    }

    private static boolean isCharacter(int[] rawData) {
        return rawData[2] != 0;
    }


    private static String parseName(ByteBuffer buffer, int nameOffset) {
        buffer.position(nameOffset);
        //Util.skip(buffer, bytesLeftUntilName);
        int nameHeader = buffer.getInt();
        if (nameHeader != HeaderTexts.NAME) {
            throw new IllegalStateException("Expected NAME header before " + buffer.position() + " but found " + nameHeader);
        }
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        int indexOfNul = Util.getIndexOfNul(nameBytes);
        return new String(nameBytes, 0, indexOfNul);
    }

}
