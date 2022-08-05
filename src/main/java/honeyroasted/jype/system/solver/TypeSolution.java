package honeyroasted.jype.system.solver;

import java.util.List;
import java.util.Optional;

public class TypeSolution {
    private TypeContext context;
    private List<TypeConstraint> constraints;
    private TypeVerification verification;

    public TypeSolution(TypeContext context, List<TypeConstraint> constraints, TypeVerification verification) {
        this.context = context;
        this.constraints = constraints;
        this.verification = verification;
    }

    public TypeContext context() {
        return this.context;
    }

    public List<TypeConstraint> constraints() {
        return this.constraints;
    }

    public TypeVerification verification() {
        return this.verification;
    }

    public boolean successful() {
        return this.verification.success();
    }
}
