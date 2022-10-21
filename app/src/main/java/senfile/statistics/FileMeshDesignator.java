package senfile.statistics;

public class FileMeshDesignator implements Comparable<FileMeshDesignator> {

    public final String file;
    public final String mesh;

    public FileMeshDesignator(String file, String mesh) {
        this.file = file;
        this.mesh = mesh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMeshDesignator that = (FileMeshDesignator) o;

        if (file != null ? !file.equals(that.file) : that.file != null) return false;
        return mesh != null ? mesh.equals(that.mesh) : that.mesh == null;
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (mesh != null ? mesh.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return file + " : " + mesh;
    }

    @Override
    public int compareTo(FileMeshDesignator o) {
        return toString().compareTo(o.toString());
    }
}
