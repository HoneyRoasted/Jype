package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypeSolver implements TypeSolver {
    protected TypeSystem system;
    protected List<TypeConstraint> constraints = Collections.synchronizedList(new ArrayList<>());

    public AbstractTypeSolver(TypeSystem system) {
        this.system = system;
    }

    @Override
    public TypeSolver constrain(TypeConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    @Override
    public List<TypeConstraint> constraints() {
        return List.copyOf(this.constraints);
    }
}
