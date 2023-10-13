package honeyroasted.jype.system.solver.solvers.inference.expression;

import honeyroasted.jype.type.ClassReference;

public interface ExpressionResolver {

    boolean isFunctionalInterface(ClassReference ref);

    default ExpressionResolver or(ExpressionResolver other) {
        return new ExpressionResolver() {
            @Override
            public boolean isFunctionalInterface(ClassReference ref) {
                return ExpressionResolver.this.isFunctionalInterface(ref) || other.isFunctionalInterface(ref);
            }
        };
    }

}
