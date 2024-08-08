package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver._old.solvers.inference.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;

public class ExpressionAssignmentConstant implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.context() == ASSIGNMENT && bound.left() instanceof ExpressionInformation.Constant;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.OR);

        Type target = bound.right();
        ExpressionInformation.Constant constantExpression = (ExpressionInformation.Constant) bound.left();
        Type subtype = constantExpression.type(target.typeSystem());

        TypeConstants c = target.typeSystem().constants();

        Type cnst = constantExpression.type(subtype.typeSystem());
        Object val = constantExpression.value();

        if (cnst.typeEquals(c.byteType()) || cnst.typeEquals(c.shortType()) || cnst.typeEquals(c.charType()) || cnst.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(true));
                } else {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(true));
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(true));
                } else {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(false));
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(true));
                } else {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), builder)
                            .setSatisfied(false));
                }
            }
        }

        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(subtype, target, LOOSE_INVOCATION), builder));
    }

    private static boolean fits(Object obj, long min, long max) {
        if (obj instanceof Number n) {
            return min <= n.longValue() && n.longValue() <= max;
        } else if (obj instanceof Character c) {
            return min <= c && c <= max;
        }
        return false;
    }
}
