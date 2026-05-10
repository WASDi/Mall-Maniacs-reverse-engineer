package anmfile;

import anmfile.factories.AnmFileFactory;
import senfile.Util;

import java.io.IOException;

public class AnmFileMain {

    public static void main(String[] args) throws IOException {
        AnmFile anmFile = AnmFileFactory.fromFile(Util.ROOT_DIR + "anim/s_grab.anm");
        anmFile.dumpInfo();
    }

}
