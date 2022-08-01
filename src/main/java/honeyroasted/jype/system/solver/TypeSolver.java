package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

import java.util.List;

public interface TypeSolver {

    TypeSolver constrain(TypeConstraint constraint);

    TypeSolution solve();

    List<TypeConstraint> constraints();

    default TypeSolver constrain(TypeConcrete subtype, TypeConcrete parent) {
        return constrain(new TypeConstraint.Bound(subtype, parent));
    }

}
