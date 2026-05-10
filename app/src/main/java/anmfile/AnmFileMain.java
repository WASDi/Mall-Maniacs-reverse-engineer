package anmfile;

import anmfile.factories.AnmFileFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class AnmFileMain {

    public static void main(String[] args) throws IOException {
        AnmFile anmFile = fromFile("/Users/wasd/Downloads/Mall Maniacs/anim/s_grab.anm");
        anmFile.dumpInfo();
    }

    public static AnmFile fromFile(String filePath) throws IOException {
        RandomAccessFile aFile = new RandomAccessFile(filePath, "r");
        FileChannel inChannel = aFile.getChannel();
        long fileSize = inChannel.size();
        ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
        inChannel.read(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        AnmFile anmFile = AnmFileFactory.parseFromBufferPosition(buffer);

        inChannel.close();
        aFile.close();

        return anmFile;
    }


}
