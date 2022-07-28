package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeConstraint;

import java.util.List;

public class TypeSolution {
    private TypeContext context;
    private TypeConstraint root;
    private List<TypeConstraint> unverifiable;
    private boolean successful;

    public TypeSolution(TypeContext context, TypeConstraint root, List<TypeConstraint> unverifiable, boolean successful) {
        this.context = context;
        this.root = root;
        this.unverifiable = unverifiable;
        this.successful = successful;
    }

    public TypeContext context() {
        return this.context;
    }

    public boolean successful() {
        return this.successful;
    }

    public TypeConstraint root() {
        return this.root;
    }

    public List<TypeConstraint> unverifiable() {
        return this.unverifiable;
    }
}
