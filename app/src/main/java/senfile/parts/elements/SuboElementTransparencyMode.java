package senfile.parts.elements;

public enum SuboElementTransparencyMode {
    NO_TRANSPARENCY(0, false, false),
    NO_TRANSPARENCY_MUST_MIRROR_SURFACE(-128, false, true),
    TRANSPARENCY(64, true, false),
    TRANSPARENCY_MUST_MIRROR_SURFACE(-64, true, true);

    private static final SuboElementTransparencyMode[] _VALUES = values();

    public final byte transparencyByte;
    public final boolean isTransparent;
    public final boolean mustMirrorSurface;

    SuboElementTransparencyMode(int transparencyByte, boolean isTransparent, boolean mustMirrorSurface) {
        this.transparencyByte = (byte) transparencyByte;
        this.isTransparent = isTransparent;
        this.mustMirrorSurface = mustMirrorSurface;
    }

    public static SuboElementTransparencyMode resolveFromByte(byte transparencyByte) {
        for (SuboElementTransparencyMode value : _VALUES) {
            if (value.transparencyByte == transparencyByte) {
                return value;
            }
        }
        return NO_TRANSPARENCY;
    }
}
