package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public interface ParameterizedClassType extends PossiblyUnmodifiable, ClassType, ParameterizedType {

    void setClassReference(ClassReference classReference);

    ClassType outerType();

    void setOuterType(ClassType outerType);

    @Override
    default Set<Type> knownDirectSupertypes() {
        Set<Type> supertypes = new LinkedHashSet<>();
        if (this.superClass() != null) {
            supertypes.add(this.directSupertype(this.superClass()));
        }
        this.interfaces().forEach(c -> supertypes.add(this.directSupertype(c)));
        return supertypes;
    }

    @Override
    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
