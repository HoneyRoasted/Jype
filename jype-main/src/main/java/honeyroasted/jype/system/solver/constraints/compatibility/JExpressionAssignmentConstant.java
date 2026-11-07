package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.solver.JConstantEvaluator;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

import java.util.Optional;

public class JExpressionAssignmentConstant extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && applies(constraint);
    }

    public static boolean applies(JTypeConstraints.ExpressionCompatible constraint) {
        JType right = constraint.right();
        if (constraint.middle() != JTypeConstraints.Compatible.Context.ASSIGNMENT || !right.isProperType()) {
            return false;
        }
        JTypeConstants c = right.typeSystem().constants();
        if (!(right.typeEquals(c.byteType()) || right.typeEquals(c.shortType()) || right.typeEquals(c.charType())
                || right.typeEquals(c.byteBox()) || right.typeEquals(c.shortBox()) || right.typeEquals(c.charBox()))) {
            return false;
        }
        Optional<JConstantEvaluator.Value> value = JConstantEvaluator.evaluate(constraint.left(), right.typeSystem());
        if (value.isEmpty()) {
            return false;
        }
        JType t = value.get().type();
        return t.typeEquals(c.byteType()) || t.typeEquals(c.shortType()) || t.typeEquals(c.charType()) || t.typeEquals(c.intType());
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JType right = constraint.right();
        JTypeConstants c = right.typeSystem().constants();
        JConstantEvaluator.Value value = JConstantEvaluator.evaluate(constraint.left(), right.typeSystem()).orElseThrow();
        long v = value.value() instanceof Character ch ? ch : ((Number) value.value()).longValue();
        boolean fits;
        if (right.typeEquals(c.charType()) || right.typeEquals(c.charBox())) {
            fits = v >= Character.MIN_VALUE && v <= Character.MAX_VALUE;
        } else if (right.typeEquals(c.byteType()) || right.typeEquals(c.byteBox())) {
            fits = v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE;
        } else {
            fits = v >= Short.MIN_VALUE && v <= Short.MAX_VALUE;
        }
        if (fits) {
            // JLS §5.2 narrowing primitive conversion (then boxing) of a representable constant expression
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.ExpressionCompatible(constraint.left(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, value.type()));
        } else {
            branch.drop(constraint).add(new JTypeConstraints.ExpressionCompatible(constraint.left(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, right));
        }
    }
}
