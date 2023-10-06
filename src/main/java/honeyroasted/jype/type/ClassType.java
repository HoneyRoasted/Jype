package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ClassType extends Type, PossiblyUnmodifiable {

    ClassNamespace namespace();

    void setNamespace(ClassNamespace namespace);

    boolean isInterface();

    void setInterface(boolean isInterface);

    ClassType superClass();

    void setSuperClass(ClassType superClass);

    List<ClassType> interfaces();

    void setInterfaces(List<ClassType> interfaces);

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    boolean hasSupertype(ClassReference supertype);

    ClassReference classReference();

    boolean hasTypeArguments();

    @Override
    default TypeWithMetadata<? extends ClassType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }

    @Override
    default  <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
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
