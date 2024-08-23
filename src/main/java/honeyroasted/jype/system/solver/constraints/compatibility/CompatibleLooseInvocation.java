package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class CompatibleLooseInvocation implements ConstraintMapper.Unary<TypeConstraints.Compatible> {

    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return (constraint.middle() == LOOSE_INVOCATION || constraint.middle() == ASSIGNMENT) &&
                left.isProperType() && right.isProperType();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        Set<Constraint> newChildren = new LinkedHashSet<>();

        newChildren.add(new TypeConstraints.Subtype(left, right));
        if (left instanceof PrimitiveType l && !(right instanceof PrimitiveType)) {
            newChildren.add(new TypeConstraints.Subtype(l.box(), right));
        } else if (right instanceof PrimitiveType r && !(left instanceof PrimitiveType)) {
            newChildren.add(new TypeConstraints.Subtype(left, r.box()));
        }

        node.expand(ConstraintNode.Operation.OR, newChildren.stream().map(c -> c.tracked(node.trackedConstraint()).createLeaf()).toList());
    }
}
