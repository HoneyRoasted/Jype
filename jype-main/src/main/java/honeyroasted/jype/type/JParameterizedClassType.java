package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public interface JParameterizedClassType extends PossiblyUnmodifiable, JClassType, JParameterizedType {

    void setClassReference(JClassReference classReference);

    JClassType outerType();

    void setOuterType(JClassType outerType);

    @Override
    default Set<JType> knownDirectSupertypes() {
        Set<JType> supertypes = new LinkedHashSet<>();
        if (this.superClass() != null) {
            supertypes.add(this.directSupertype(this.superClass()));
        }
        this.interfaces().forEach(c -> supertypes.add(this.directSupertype(c)));
        return supertypes;
    }

    @Override
    default boolean hasTypeArguments() {
        return this.typeArguments() != null && !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
