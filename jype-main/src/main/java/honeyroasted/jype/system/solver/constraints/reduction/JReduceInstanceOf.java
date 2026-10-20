package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

/**
 * Reduces a type comparison expression {@code operand instanceof T} (JLS §15.20.2) into the
 * same castability check used for an explicit cast (JLS §15.20.2 explicitly defers to §5.5 here:
 * the comparison is only legal if a cast of the operand's type to T would also be legal), plus
 * the expression's own {@code boolean} result type satisfying whatever context it is embedded in.
 */
public class JReduceInstanceOf extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.InstanceOf;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InstanceOf instOf = (JExpressionInformation.InstanceOf) constraint.left();
        JType checkedType = instOf.checkedType();
        JTypeSystem system = checkedType.typeSystem();

        branch.drop(constraint)
                .add(new JTypeConstraints.ExpressionCompatible(instOf.operand(), JTypeConstraints.Compatible.Context.EXPLICIT_CAST, checkedType))
                .add(new JTypeConstraints.Compatible(system.constants().booleanType(), constraint.middle(), constraint.right()));
    }
}
