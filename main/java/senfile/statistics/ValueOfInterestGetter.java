package senfile.statistics;

import senfile.parts.elements.MapiElement;
import senfile.parts.elements.ObjiElement;
import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.MeshCharacter;
import senfile.parts.mesh.MeshObject;
import senfile.parts.mesh.SenMesh;

import java.util.function.Function;

public class ValueOfInterestGetter {

    private final Function<SenMesh, Object> forCommon;
    private final Function<MeshCharacter, Object> forCharacter;
    private final Function<MeshObject, Object> forObject;

    public final Function<MapiElement, Object> forMapi;
    public final Function<SuboElement, Object> forSubo;
    public final Function<ObjiElement, Object> forObji;

    public ValueOfInterestGetter(Function<SenMesh, Object> forCommon,
                                 Function<MeshCharacter, Object> forCharacter,
                                 Function<MeshObject, Object> forObject,
                                 Function<MapiElement, Object> forMapi,
                                 Function<SuboElement, Object> forSubo,
                                 Function<ObjiElement, Object> forObji) {
        this.forCommon = forCommon;
        this.forCharacter = forCharacter;
        this.forObject = forObject;
        this.forMapi = forMapi;
        this.forSubo = forSubo;
        this.forObji = forObji;
    }

    public Object getFor(SenMesh mesh) {
        if (mesh instanceof MeshCharacter) {
            Object result = forCharacter.apply((MeshCharacter) mesh);
            if (result != null) {
                return result;
            }
        } else if (mesh instanceof MeshObject) {
            Object result = forObject.apply((MeshObject) mesh);
            if (result != null) {
                return result;
            }
        }

        return forCommon.apply(mesh);
    }
}

