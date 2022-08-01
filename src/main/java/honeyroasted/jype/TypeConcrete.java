package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.impl.ForceResolveTypeSolver;
import honeyroasted.jype.type.*;
import honeyroasted.jype.system.TypeConstraint;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TypeConcrete extends Type {

    TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system);

    default boolean isAssignableTo(TypeConcrete other, TypeSystem system) {
        return new ForceResolveTypeSolver(system)
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
            if (t instanceof TypeParameter ref) {
                return mapper.apply(ref);
            } else {
                return t;
            }
        });
    }

    default TypeConcrete flatten() {
        return this;
    }

    static TypeConstraint defaultTests(TypeConcrete self, TypeConcrete other, TypeSystem system, TypeConstraint def) {
        return defaultTests(self, other, system, () -> def);
    }

    static TypeConstraint defaultTests(TypeConcrete self, TypeConcrete other, TypeSystem system, Supplier<TypeConstraint> def) {
        if (other instanceof TypeNone) {
            return TypeConstraint.FALSE;
        } else if (other instanceof TypeOr or) {
            return new TypeConstraint.Or(or.types().stream().map(t -> self.assignabilityTo(t, system)).toList());
        } else if (other instanceof TypeAnd and) {
            return new TypeConstraint.And(and.types().stream().map(t -> self.assignabilityTo(t, system)).toList());
        } else if (other instanceof TypeIn in) {
             return self.assignabilityTo(in.bound(), system);
         } else if (other instanceof TypeParameter ref) {
            return new TypeConstraint.Bound(self, ref);
         }

         return def.get();
    }

}
