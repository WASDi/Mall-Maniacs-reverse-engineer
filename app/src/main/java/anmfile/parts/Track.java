package anmfile.parts;

import java.util.List;

public record Track(List<Keyframe> keyframes) {

    public Keyframe forTargetIndex(int idx) {
        for (Keyframe keyframe : keyframes) {
            if (keyframe.targetIndex() != null && keyframe.targetIndex() == idx) {
                return keyframe;
            }
        }
        return null;
    }
}
