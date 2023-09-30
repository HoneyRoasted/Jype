package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public sealed interface ClassType extends Type, PossiblyUnmodifiable permits ClassReference, ParameterizedClassType {

    ClassNamespace namespace();

    void setNamespace(ClassNamespace namespace);

    ClassType superClass();

    void setSuperClass(ClassType superClass);

    List<ClassType> interfaces();

    void setInterfaces(List<ClassType> interfaces);

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    boolean hasSupertype(ClassReference supertype);

    ClassReference classReference();

    default boolean buildHierarchyPath(ClassReference supertype, List<ClassType> building) {
        if (supertype == null) return false;

        if (this.classReference().equals(supertype)) {
            building.add(0, this);
            return true;
        } else {
            if (this.superClass() != null && this.superClass().buildHierarchyPath(supertype, building)) {
                building.add(0, this);
                return true;
            }

            for (ClassType inter : this.interfaces()) {
                if (inter.buildHierarchyPath(supertype, building)) {
                    building.add(0, this);
                    return true;
                }
            }

            return false;
        }
    }

    default Optional<List<ClassType>> hierarchyPathTo(ClassReference supertype) {
        List<ClassType> building = new ArrayList<>();
        if (buildHierarchyPath(supertype, building)) {
            return Optional.of(building);
        }
        return Optional.empty();
    }


}
