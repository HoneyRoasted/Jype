package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTypeSolver implements TypeSolver {
    protected List<TypeConstraint> constraints = new ArrayList<>();

    @Override
    public TypeSolver constrain(TypeConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    @Override
    public TypeConstraint.And root() {
        return new TypeConstraint.And(this.constraints);
    }
}
