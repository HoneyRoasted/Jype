package honeyroasted.jype;

import honeyroasted.jype.concrete.TypeAnd;
import honeyroasted.jype.concrete.TypeIn;
import honeyroasted.jype.concrete.TypeNone;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.concrete.TypeOr;
import honeyroasted.jype.concrete.TypeParameterReference;
import honeyroasted.jype.system.Constraint;

public interface TypeConcrete extends Type {

    Constraint assignabilityTo(TypeConcrete other);

    default boolean isAssignableTo(TypeConcrete other) {
        return assignabilityTo(other).forceResolve().equals(Constraint.TRUE);
    }

    static Constraint defaultTests(TypeConcrete self, TypeConcrete other, Constraint def) {
        if (other instanceof TypeNone) {
            return Constraint.FALSE;
        } else if (other instanceof TypeOr or) {
            return new Constraint.Or(or.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeAnd and) {
            return new Constraint.And(and.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeIn in) {
             return self.assignabilityTo(in.bound());
         } else if (other instanceof TypePlaceholder inferable) {
             return new Constraint.InferFrom(inferable, self);
         } else if (other instanceof TypeParameterReference ref) {
             return new Constraint.BoundedFrom(ref, self);
         }

         return def;
    }

}
