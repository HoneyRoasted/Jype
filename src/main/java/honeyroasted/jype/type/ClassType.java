package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ClassType extends InstantiableType, PossiblyUnmodifiable, ArgumentType {

    ClassNamespace namespace();

    void setNamespace(ClassNamespace namespace);

    int modifiers();

    void setModifiers(int modifiers);

    ClassReference outerClass();

    void setOuterClass(ClassReference outerClass);

    ClassType superClass();

    void setSuperClass(ClassType superClass);

    List<ClassType> interfaces();

    void setInterfaces(List<ClassType> interfaces);

    default boolean hasTypeParameters() {
        return !this.typeParameters().isEmpty();
    }

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    boolean hasSupertype(ClassReference supertype);

    ClassReference classReference();

    boolean hasTypeArguments();

    List<ArgumentType> typeArguments();

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitClassType(this, context);
    }

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
