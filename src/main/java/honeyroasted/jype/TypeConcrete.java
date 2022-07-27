package honeyroasted.jype;

import honeyroasted.jype.concrete.TypeAnd;
import honeyroasted.jype.concrete.TypeIn;
import honeyroasted.jype.concrete.TypeInferable;
import honeyroasted.jype.concrete.TypeOr;
import honeyroasted.jype.system.Constraint;

public interface TypeConcrete extends Type {

    Constraint assignabilityTo(TypeConcrete other);

    static Constraint defaultTests(TypeConcrete self, TypeConcrete other, Constraint def) {
         if (other instanceof TypeOr or) {
            return new Constraint.Or(or.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeAnd and) {
            return new Constraint.And(and.types().stream().map(self::assignabilityTo).toList());
        } else if (other instanceof TypeIn in) {
             return self.assignabilityTo(in.bound());
         } else if (other instanceof TypeInferable inferable) {
             return new Constraint.InferFrom(inferable, other);
         }

         return def;
    }

}
