package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class CompatibleExplicitCast implements ConstraintMapper.Unary<TypeConstraints.Compatible> {

    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return node.isLeaf() && constraint.middle() == EXPLICIT_CAST && left.isProperType() && right.isProperType();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        node.expand(ConstraintNode.Operation.OR, false,
                new TypeConstraints.Compatible(constraint.left(), LOOSE_INVOCATION, constraint.right()),
                new TypeConstraints.Compatible(constraint.right(), LOOSE_INVOCATION, constraint.left()));
    }
}
