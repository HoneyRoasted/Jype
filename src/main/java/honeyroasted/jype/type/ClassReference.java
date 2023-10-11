package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface ClassReference extends PossiblyUnmodifiable, ClassType {
    ParameterizedClassType parameterized(List<ArgumentType> typeArguments);

    ParameterizedClassType parameterized(ArgumentType... typeArguments);

    ParameterizedClassType parameterizedWithTypeVars();

    @Override
    default Set<Type> knownDirectSupertypes() {
        Set<Type> supertypes =  new LinkedHashSet<>();
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
    default List<ArgumentType> typeArguments() {
        return Collections.emptyList();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
