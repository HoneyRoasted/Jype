package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface JMethodReference extends PossiblyUnmodifiable, JMethodType {
    JParameterizedMethodType parameterized(List<JArgumentType> typeArguments);

    JParameterizedMethodType parameterized(JArgumentType... typeArguments);

    JParameterizedMethodType parameterizedWithTypeVars();

    default JMethodReference methodReference() {
        return this;
    }

    @Override
    default JClassType outerType() {
        return outerClass();
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
