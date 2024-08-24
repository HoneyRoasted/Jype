package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class SubtypeIntersection implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return left.isProperType() && right.isProperType() &&
                (left instanceof IntersectionType || right instanceof IntersectionType);
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        Set<Constraint> newChildren = new LinkedHashSet<>();
        ConstraintNode.Operation operation = null;

        if (left instanceof IntersectionType l) {
            l.children().forEach(t -> newChildren.add(new TypeConstraints.Subtype(t, right)));
            operation = ConstraintNode.Operation.OR;
        } else if (right instanceof IntersectionType r) {
            r.children().forEach(t -> newChildren.add(new TypeConstraints.Subtype(left, t)));
            operation = ConstraintNode.Operation.AND;
        }

        node.expand(operation, newChildren.stream().map(cn -> cn.tracked(node.trackedConstraint()).createLeaf()).toList());
    }
}
