package honeyroasted.jype.system.solver.circle;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.TypeVerification;

import java.util.List;

public class DeCircleTypeSolver extends AbstractTypeSolver implements TypeSolver {

    public DeCircleTypeSolver(TypeSystem system) {
        super(system, DeCircleConstraint.DeCircle.class);
    }

    @Override
    public TypeSolution solve() {
        List<TypeConstraint> constraints = this.constraints();
        TypeContext context = new TypeContext();

        TypeVerification.Builder builder = TypeVerification.builder()
                .constraint(TypeConstraint.TRUE)
                .success();

        constraints.forEach(c -> {
            if (c instanceof DeCircleConstraint.DeCircle deCircle) {
                builder.children(TypeVerification.success(deCircle));

                TypeConcrete type = deCircle.type();
                if (!context.parameters().containsKey(type)) {
                    if (type.isCircular()) {
                        //TODO
                    } else {
                        context.put(type, type);
                    }
                }
            }
        });

        return new TypeSolution(context, constraints, builder.build());
    }
}
