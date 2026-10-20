package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

/**
 * Reduces an explicit cast expression {@code (T) operand} (JLS §5.5/§15.16) into two
 * independent checks: that casting the operand's type to T is legal ({@code EXPLICIT_CAST}
 * compatibility, handled for proper types by {@link honeyroasted.jype.system.solver.constraints.compatibility.JCompatibleExplicitCast}),
 * and that the cast's resulting type T satisfies whatever context this whole expression is
 * itself embedded in. Casting a lambda or method/constructor reference to a functional
 * interface type is handled naturally here too, since the operand's own
 * {@code ExpressionCompatible} reducers ignore the compatibility context entirely.
 */
public class JReduceCast extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Cast;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Cast cast = (JExpressionInformation.Cast) constraint.left();
        JType castType = cast.castType();

        branch.drop(constraint)
                .add(new JTypeConstraints.ExpressionCompatible(cast.operand(), JTypeConstraints.Compatible.Context.EXPLICIT_CAST, castType))
                .add(new JTypeConstraints.Compatible(castType, constraint.middle(), constraint.right()));
    }
}
