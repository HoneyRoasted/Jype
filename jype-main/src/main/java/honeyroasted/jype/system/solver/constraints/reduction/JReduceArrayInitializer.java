package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

/**
 * Reduces a bare array initializer {@code {e1, e2, ...}} (JLS §10.6) against whatever array type
 * its enclosing {@code ExpressionCompatible} constraint supplies as the target, checking each
 * element as assignment-compatible with the component type. A nested element that's itself an
 * {@link JExpressionInformation.ArrayInitializer} recurses naturally: its own
 * {@code ExpressionCompatible(element, ASSIGNMENT, componentType)} constraint is handled by this
 * same reducer again, with the next-level-down component type as the new target. A target that
 * isn't an array type at all makes the initializer illegal outright.
 */
public class JReduceArrayInitializer extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.ArrayInitializer;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.ArrayInitializer initializer = (JExpressionInformation.ArrayInitializer) constraint.left();
        JType target = constraint.right();

        if (!(target instanceof JArrayType arrayType)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JType component = arrayType.component();
        branch.drop(constraint);
        for (JExpressionInformation element : initializer.elements()) {
            branch.add(new JTypeConstraints.ExpressionCompatible(element, JTypeConstraints.Compatible.Context.ASSIGNMENT, component));
        }
    }
}
