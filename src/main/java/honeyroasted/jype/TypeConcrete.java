package honeyroasted.jype;

import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.impl.ForceResolveTypeSolver;
import honeyroasted.jype.type.*;
import honeyroasted.jype.system.TypeConstraint;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface TypeConcrete extends Type {

    TypeConstraint assignabilityTo(TypeConcrete other);

    default boolean isAssignableTo(TypeConcrete other) {
        return new ForceResolveTypeSolver()
                .constrain(this, other)
                .solve()
                .successful();
    }

    default <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(this);
    }

    default void forEach(Consumer<TypeConcrete> consumer) {
        map(t -> {
            consumer.accept(t);
            return t;
        });
    }

    default <T extends TypeConcrete> T copy() {
        return map(Function.identity());
    }

    default <T extends Type> T resolveVariables(Function<TypeParameter, TypeConcrete> mapper) {
        return map(t -> {
            if (t instanceof TypeParameter) {
                return mapper.apply((TypeParameter) t);
            } else {
                return t;
            }
        });
    }

    static TypeConstraint defaultTests(TypeConcrete self, TypeConcrete other, TypeConstraint def) {
        return defaultTests(self, other, () -> def);
    }

    static TypeConstraint defaultTests(TypeConcrete self, TypeConcrete other, Supplier<TypeConstraint> def) {
        if (other instanceof TypeNone) {
            return TypeConstraint.FALSE;
        } else if (other instanceof TypeOr) {
            return new TypeConstraint.Or(((TypeOr) other).types().stream().map(self::assignabilityTo).collect(Collectors.toList()));
        } else if (other instanceof TypeAnd) {
            return new TypeConstraint.And(((TypeAnd) other).types().stream().map(self::assignabilityTo).collect(Collectors.toList()));
        } else if (other instanceof TypeIn) {
             return self.assignabilityTo(((TypeIn) other).bound());
         } else if (other instanceof TypeParameter) {
            return new TypeConstraint.Bound(self, other);
         }

         return def.get();
    }

}
