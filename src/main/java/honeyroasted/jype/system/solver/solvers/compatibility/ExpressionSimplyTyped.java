package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver._old.solvers.inference.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;
import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class ExpressionSimplyTyped implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.ExpressionCompatible cmpt &&
                cmpt.left().isSimplyTyped() && !(cmpt.context() == ASSIGNMENT && cmpt.left() instanceof ExpressionInformation.Constant);
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        Type supertype = bound.right();
        Type expr = bound.left().getSimpleType(supertype.typeSystem()).get();

        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(expr, supertype, bound.context()), constraint));
    }
}
