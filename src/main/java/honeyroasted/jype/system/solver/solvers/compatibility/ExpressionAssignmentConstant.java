package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver._old.solvers.inference.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;
import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class ExpressionAssignmentConstant implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.ExpressionCompatible cmpt &&
                cmpt.context() == ASSIGNMENT && cmpt.left() instanceof ExpressionInformation.Constant;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.OR);

        Type target = bound.right();
        ExpressionInformation.Constant constantExpression = (ExpressionInformation.Constant) bound.left();
        Type subtype = constantExpression.type(target.typeSystem());

        TypeConstants c = target.typeSystem().constants();

        Type cnst = constantExpression.type(subtype.typeSystem());
        Object val = constantExpression.value();

        if (cnst.typeEquals(c.byteType()) || cnst.typeEquals(c.shortType()) || cnst.typeEquals(c.charType()) || cnst.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(true));
                } else {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(true));
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(true));
                } else {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(false));
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(true));
                } else {
                    bounds.add(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), constraint)
                            .setSatisfied(false));
                }
            }
        }

        constraints.add(TypeBound.Result.builder(new TypeBound.Compatible(subtype, target, LOOSE_INVOCATION), constraint));
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
