package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;


public class JExpressionAssignmentConstant extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType right = mapper.apply(constraint.right());

        return status.isUnknown() && constraint.middle() == ASSIGNMENT && constraint.left() instanceof JExpressionInformation.Constant && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType right = mapper.apply(constraint.right());

        JExpressionInformation.Constant constantExpression = (JExpressionInformation.Constant) constraint.left();
        JType subtype = constantExpression.getSimpleType(right.typeSystem(), mapper).get();

        JTypeConstants c = subtype.typeSystem().constants();

        JType target = right;
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
                .add(new JTypeConstraints.Compatible(subtype, LOOSE_INVOCATION, right));
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
