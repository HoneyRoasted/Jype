package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.operations.TypeOperations;

import java.util.LinkedHashSet;
import java.util.Set;

public class ReduceDelayedExpressionCompatible extends ConstraintMapper.Unary<TypeConstraints.DelayedExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.DelayedExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && !branch.diverged();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.DelayedExpressionCompatible constraint, Constraint.Status status) {
        ConstraintTree test = new ConstraintTree();
        test.addBranch(branch.copy(test));

        TypeOperations operations = allContext.firstOr(TypeSystem.class, TypeSystem.RUNTIME_REFLECTION).operations();
        operations.resolutionApplier().accept(test);
        operations.verifyApplier().accept(test);


        Set<Constraint> newChildren = new LinkedHashSet<>();
        test.branches().forEach(newBranch -> {
            if (newBranch.status().isTrue()) {
                newBranch.constraints().entrySet().stream()
                        .filter(cn -> cn.getValue().isTrue() && cn.getKey() instanceof TypeConstraints.Instantiation inst && inst.left().equals(constraint.left()))
                        .map(cn -> (TypeConstraints.Instantiation) cn.getKey())
                        .findFirst()
                        .ifPresent(instantiation -> newChildren.add(new TypeConstraints.ExpressionCompatible(constraint.right().left(), constraint.right().middle(), instantiation.right())));
            }
        });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint)
                    .diverge(newChildren);
        }
    }
}
