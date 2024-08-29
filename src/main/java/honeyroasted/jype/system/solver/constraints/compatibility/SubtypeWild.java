package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.function.Function;

public class SubtypeWild implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return node.isLeaf() && left.isProperType() && right.isProperType() &&
                (left instanceof WildType.Upper || right instanceof WildType.Lower);
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left instanceof WildType.Upper l) {
            node.expand(ConstraintNode.Operation.OR, false,
                    l.upperBounds().stream().map(b -> new TypeConstraints.Subtype(b, right)).toArray(Constraint[]::new));
        } else if (right instanceof WildType.Lower r) {
            node.expand(ConstraintNode.Operation.AND, false,
                    r.upperBounds().stream().map(b -> new TypeConstraints.Subtype(left, b)).toArray(Constraint[]::new));
        }
    }
}
