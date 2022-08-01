package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeConstraint;

import java.util.Optional;

public class TypeSolution {
    private TypeContext context;
    private TypeConstraint root;
    private TypeVerification verification;

    public TypeSolution(TypeContext context, TypeConstraint root, TypeVerification verification) {
        this.context = context;
        this.root = root;
        this.verification = verification;
    }

    public Optional<TypeContext> context() {
        return Optional.ofNullable(this.context);
    }

    public TypeConstraint root() {
        return this.root;
    }

    public TypeVerification verification() {
        return this.verification;
    }
}
