package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;

import java.util.Optional;

public class ReduceDelayedExpressionCompatible implements ConstraintMapper.Unary<TypeConstraints.DelayedExpressionCompatible> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.DelayedExpressionCompatible constraint) {
        return node.isLeaf();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.DelayedExpressionCompatible constraint) {
        Optional<TypeConstraints.Instantiation> instOpt = node.root(ConstraintNode.Operation.AND).stream().filter(cn -> cn.constraint() instanceof TypeConstraints.Instantiation inst && inst.left().equals(constraint.left())).map(cn -> (TypeConstraints.Instantiation) cn.constraint()).findFirst();
        if (instOpt.isPresent()) {
            node.expandInPlace(ConstraintNode.Operation.AND, false)
                    .attach(new TypeConstraints.ExpressionCompatible(constraint.right().left(), constraint.right().middle(), instOpt.get().right()));
        }
    }
}
