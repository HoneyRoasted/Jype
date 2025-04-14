package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface JClassReference extends PossiblyUnmodifiable, JClassType {
    JParameterizedClassType parameterized(List<JArgumentType> typeArguments);

    JParameterizedClassType parameterized(JArgumentType... typeArguments);

    JParameterizedClassType parameterizedWithTypeVars();

    JParameterizedClassType parameterizedWithMetaVars();

    @Override
    default JClassType outerType() {
        return outerClass();
    }

    @Override
    default Set<JType> knownDirectSupertypes() {
        Set<JType> supertypes = new LinkedHashSet<>();
        if (this.superClass() != null) {
            supertypes.add(this.superClass());
        }
        supertypes.addAll(this.interfaces());
        return supertypes;
    }

    @Override
    default boolean hasTypeArguments() {
        return false;
    }

    @Override
    default List<JArgumentType> typeArguments() {
        return Collections.emptyList();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
