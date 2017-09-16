package senfile;

import senfile.parts.Cols;
import senfile.parts.Mapi;
import senfile.parts.Obji;
import senfile.parts.Onam;
import senfile.parts.Subo;
import senfile.parts.Tani;
import senfile.parts.Tnam;
import senfile.parts.mesh.SenMesh;

import java.util.List;

public class SenFile {

    private final String title;
    public final int fileSize;
    private final List<SenMesh> meshes;
    private final Mapi mapi;
    private final Subo subo;
    private final Obji obji;

    public final Cols cols;
    public final Tnam tnam;
    public final Onam onam;
    public final Tani tani;

    public SenFile(String title, int fileSize, List<SenMesh> meshes, Mapi mapi, Subo subo, Obji obji, Cols cols, Tnam tnam, Onam onam, Tani tani) {
        this.title = title;
        this.fileSize = fileSize;
        this.meshes = meshes;
        this.mapi = mapi;
        this.subo = subo;
        this.obji = obji;
        this.cols = cols;
        this.tnam = tnam;
        this.onam = onam;
        this.tani = tani;
    }

    public String getTitle() {
        return title;
    }

    public List<SenMesh> getMeshes() {
        return meshes;
    }

    public Mapi getMapi() {
        return mapi;
    }

    public Subo getSubo() {
        return subo;
    }

    public Obji getObji() {
        return obji;
    }

    public void dumpInfo() {
        System.out.println("SENFILE");
        System.out.println("Title: " + title);
        System.out.println("Num mesh: " + meshes.size());
        System.out.println("Num mapi: " + mapi.elements.length);
        System.out.println("Num subo: " + subo.elements.size());
        System.out.println("Num obji: " + obji.elements.length);
        System.out.println();
    }
}
