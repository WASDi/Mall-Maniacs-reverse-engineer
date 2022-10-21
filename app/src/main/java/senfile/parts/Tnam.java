package senfile.parts;

import java.util.List;

/**
 * Names of TPG-files without file extension
 */
public class Tnam {

    public final List<String> names;
    public final int sizeOfTnam;

    public Tnam(List<String> names, int sizeOfTnam) {
        this.names = names;
        this.sizeOfTnam = sizeOfTnam;
    }
}
