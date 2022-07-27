package honeyroasted.jype;

import honeyroasted.jype.concrete.TypeAnd;
import honeyroasted.jype.concrete.TypeIn;
import honeyroasted.jype.concrete.TypeNone;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.concrete.TypeOr;
import honeyroasted.jype.concrete.TypeParameterReference;
import honeyroasted.jype.system.TypeConstraint;

public interface TypeConcrete extends Type {

    TypeConstraint assignabilityTo(TypeConcrete other);

    default boolean isAssignableTo(TypeConcrete other) {
        return assignabilityTo(other).forceResolve() instanceof TypeConstraint.True;
    }

    static TypeConstraint defaultTests(TypeConcrete self, TypeConcrete other, TypeConstraint def) {
        if (other instanceof TypeNone) {
            return TypeConstraint.FALSE;
        } else if (other instanceof TypeOr or) {
            return new TypeConstraint.Or(or.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeAnd and) {
            return new TypeConstraint.And(and.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeIn in) {
             return self.assignabilityTo(in.bound());
         } else if (other instanceof TypePlaceholder inferable) {
            return new TypeConstraint.Bound(self, inferable);
         } else if (other instanceof TypeParameterReference ref) {
            return new TypeConstraint.Bound(self, ref);
         }

         return def;
    }

}
