package senfile;

import senfile.parts.Mapi;
import senfile.parts.Obji;
import senfile.parts.Subo;
import senfile.parts.mesh.SenMesh;

import java.util.List;

public class SenFile {

    private final String title;
    private final List<SenMesh> meshes;
    private final Mapi mapi;
    private final Subo subo;
    private final Obji obji;

    public SenFile(String title, List<SenMesh> meshes, Mapi mapi, Subo subo, Obji obji) {
        this.title = title;
        this.meshes = meshes;
        this.mapi = mapi;
        this.subo = subo;
        this.obji = obji;
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
