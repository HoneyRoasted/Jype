package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReduceInstantiationExpression implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        return bound.left() instanceof ExpressionInformation.Instantiation;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        ExpressionInformation.Instantiation inst = (ExpressionInformation.Instantiation) bound.left();
        ClassReference targetType = context.view(inst.type());

        List<ExpressionInformation> params = new ArrayList<>(inst.parameters());

        if (targetType.hasRelevantOuterType()) {
            Optional<ClassType> outerOpt = context.system().operations().outerTypeFromDeclaring(targetType, inst.declaring());
            if (outerOpt.isEmpty()) {
                context.bounds().accept(builder.setSatisfied(false));
                return;
            } else {
                params.add(0, ExpressionInformation.of(outerOpt.get()));
            }
        }


    }
}
