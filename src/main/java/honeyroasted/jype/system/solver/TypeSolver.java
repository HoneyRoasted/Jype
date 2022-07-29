package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

public interface TypeSolver {

    TypeSolver constrain(TypeConstraint constraint);

    TypeSolution solve();

    TypeConstraint root();

    default TypeSolver constrain(TypeConcrete subtype, TypeConcrete parent) {
        return constrain(new TypeConstraint.Bound(subtype, parent));
    }

}
