package senfile.parts;

import java.util.List;

/**
 * Names of all meshes present in Sen file
 */
public class Onam {

    public final List<String> names;
    public final int sizeOfOnam;

    public Onam(List<String> names, int sizeOfOnam) {
        this.names = names;
        this.sizeOfOnam = sizeOfOnam;
    }
}
