package senfile.factories;

import senfile.GameMap;
import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.parts.Cols;
import senfile.parts.Mapi;
import senfile.parts.Obji;
import senfile.parts.Onam;
import senfile.parts.Subo;
import senfile.parts.Tani;
import senfile.parts.Tnam;
import senfile.parts.elements.ObjiElement;
import senfile.parts.mesh.SenMesh;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SenFileFactory {

    private static final Map<GameMap, SenFile> SINGLETON_MAPS = new HashMap<>();

    public static SenFile getMap(GameMap map) {
        return SINGLETON_MAPS.computeIfAbsent(map, m -> fromFile(m.senFilePath));
    }

    public static SenFile fromFile(String filePath) {
        int slashIndex = filePath.lastIndexOf('/');
        String title = slashIndex == -1 ? filePath : filePath.substring(slashIndex + 1);
        try {
            return fromFile(filePath, title);
        } catch (Exception ex) {
            System.err.println("ERROR FOR " + filePath);
            ex.printStackTrace();
            throw new IllegalArgumentException("snopp");
        }
    }

    public static SenFile fromFile(String filePath, String title) throws IOException {
        RandomAccessFile aFile = new RandomAccessFile(filePath, "r");
        FileChannel inChannel = aFile.getChannel();
        long fileSize = inChannel.size();
        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
        inChannel.read(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        SenFile senFile = parseSenFile(title, buffer);

        inChannel.close();
        aFile.close();

        return senFile;
    }

    public static SenFile parseSenFile(String title, ByteBuffer buffer) {
        int rev2Header = buffer.getInt();// REV2-header
        if (rev2Header != HeaderTexts.REV2) {
            throw new IllegalArgumentException("Expected REV2 header but found " + rev2Header);
        }
        int totalBytesLeft = buffer.getInt();

        List<SenMesh> meshes = new ArrayList<>();
        Cols cols = null;
        Mapi mapi = null;
        Subo subo = null;
        Tnam tnam = null;
        Tani tani = null;
        Obji obji = null;
        Onam onam = null;

        int meshIdx = 0;

        while (buffer.hasRemaining()) {
            int header = buffer.getInt();
            if (header == HeaderTexts.MESH) {
                SenMesh mesh = MeshFactory.parseFromBufferPosition(meshIdx++, buffer);
                meshes.add(mesh);
            } else if (header == HeaderTexts.COLS) {
                if (cols != null) {
                    throw new IllegalArgumentException("Multiple COLS?");
                }
                cols = ColsFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.MAPI) {
                if (mapi != null) {
                    throw new IllegalArgumentException("Multiple MAPI?");
                }
                mapi = MapiFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.SUBO) {
                if (subo != null) {
                    throw new IllegalArgumentException("Multiple SUBO?");
                }
                subo = SuboFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.TNAM) {
                if (tnam != null) {
                    throw new IllegalArgumentException("Multiple TNAM?");
                }
                tnam = TnamFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.TANI) {
                if (tani != null) {
                    throw new IllegalArgumentException("Multiple TANI?");
                }
                tani = TaniFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.OBJI) {
                if (obji != null) {
                    throw new IllegalArgumentException("Multiple OBJI?");
                }
                obji = ObjiFactory.parseFromBufferPosition(buffer);
            } else if (header == HeaderTexts.ONAM) {
                if (onam != null) {
                    throw new IllegalArgumentException("Multiple ONAM?");
                }
                onam = OnamFactory.parseFromBufferPosition(buffer);
            } else {
                System.out.println("(" + title + ") Unknown header " + header + " before position " + buffer.position() + ", aborting.");
                break;
            }
        }

//        setMeshNamesForObji(meshes, obji);

        return new SenFile(title, meshes, mapi, subo, obji);
    }

    private static void setMeshNamesForObji(List<SenMesh> meshes, Obji obji) {
        if (meshes.size() != obji.elements.length) {
            System.out.printf("Not same amount of meshes as obji! %d and %d\n", meshes.size(), obji.elements.length);
            for (ObjiElement element : obji.elements) {
                element.setNameOfMesh("_NO_LINK");
            }
            return;
        }

        for (SenMesh mesh : meshes) {
            ObjiElement objiElement = obji.elements[mesh.meshIdx];
            objiElement.setNameOfMesh(mesh.name);
        }
    }

}
