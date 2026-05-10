package anmfile.parts;

public record Keyframe(
        int opcode,
        String targetName,
        Integer targetIndex,
        Float x,
        Float y,
        Float z,
        Integer u,
        Integer v,
        Integer w
) {
}
