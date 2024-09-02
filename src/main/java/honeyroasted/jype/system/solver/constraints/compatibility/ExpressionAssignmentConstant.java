package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;


public class ExpressionAssignmentConstant extends ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && constraint.middle() == ASSIGNMENT && constraint.left() instanceof ExpressionInformation.Constant && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type right = mapper.apply(constraint.right());

        ExpressionInformation.Constant constantExpression = (ExpressionInformation.Constant) constraint.left();
        Type subtype = constantExpression.getSimpleType(right.typeSystem(), mapper).get();

        TypeConstants c = subtype.typeSystem().constants();

        Type target = right;
        Object val = constantExpression.value();

        if (subtype.typeEquals(c.byteType()) || subtype.typeEquals(c.shortType()) || subtype.typeEquals(c.charType()) || subtype.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    branch.set(constraint, Constraint.Status.TRUE);
                    return;
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    branch.set(constraint, Constraint.Status.TRUE);
                    return;
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    branch.set(constraint, Constraint.Status.TRUE);
                    return;
                }
            }
        }

        branch.drop(constraint)
                .add(new TypeConstraints.Compatible(subtype, LOOSE_INVOCATION, right));
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
