package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JType;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class JExpressionSimplyTyped extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isSimplyTyped() && !(constraint.middle() == ASSIGNMENT && constraint.left() instanceof JExpressionInformation.Constant);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JType supertype = constraint.right();
        branch.drop(constraint)
                .add(new JTypeConstraints.Compatible(constraint.left().getSimpleType(supertype.typeSystem(), branchContext.firstOr(JTypeContext.JTypeMetavarMap.class, JTypeContext.JTypeMetavarMap.empty())).get(), constraint.middle(), supertype));
    }
}
