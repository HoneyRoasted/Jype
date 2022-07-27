package honeyroasted.jype.system;

import honeyroasted.jype.TypeConcrete;

import java.util.ArrayList;
import java.util.List;

public class TypeSolver {
    private List<Constraint> constraints = new ArrayList<>();


    public TypeSolver constrain(Constraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public TypeSolver subtype(TypeConcrete subtype, TypeConcrete parent) {
        return this.constrain(subtype.assignabilityTo(parent));
    }

}
