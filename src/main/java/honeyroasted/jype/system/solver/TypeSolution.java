package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeConstraint;

import java.util.List;
import java.util.Optional;

public class TypeSolution {
    private TypeContext context;
    private TypeConstraint root;
    private List<TypeConstraint> unverifiable;

    public TypeSolution(TypeContext context, TypeConstraint root, List<TypeConstraint> unverifiable) {
        this.context = context;
        this.root = root;
        this.unverifiable = unverifiable;
    }

    public Optional<TypeContext> context() {
        return Optional.ofNullable(this.context);
    }

    public TypeConstraint root() {
        return this.root;
    }

    public List<TypeConstraint> unverifiable() {
        return this.unverifiable;
    }
}
