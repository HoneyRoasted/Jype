package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;

import java.util.Optional;

public class ReduceDelayedExpressionCompatible extends ConstraintMapper.Unary<TypeConstraints.DelayedExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.DelayedExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.DelayedExpressionCompatible constraint, Constraint.Status status) {
        Optional<TypeConstraints.Instantiation> instOpt = branch.constraints().entrySet().stream().filter(cn -> cn.getValue().isTrue() && cn.getKey() instanceof TypeConstraints.Instantiation inst && inst.left().equals(constraint.left())).map(cn -> (TypeConstraints.Instantiation) cn.getKey()).findFirst();
        if (instOpt.isPresent()) {
            branch.drop(constraint).add(new TypeConstraints.ExpressionCompatible(constraint.right().left(), constraint.right().middle(), instOpt.get().right()));
        }
    }
}
