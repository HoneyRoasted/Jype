package honeyroasted.jype.system.solver;

import java.util.List;

public interface TypeSolver {

    TypeSolver constrain(TypeConstraint constraint);

    TypeSolution solve();

    List<TypeConstraint> constraints();

}
