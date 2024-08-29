package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.function.Function;

public class SubtypeVar implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return node.isLeaf() && left.isProperType() && right.isProperType() &&
                left instanceof VarType;
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        VarType l = (VarType) left;
        node.expand(ConstraintNode.Operation.AND, false, l.upperBounds().stream().map(b -> new TypeConstraints.Subtype(b, right)).toArray(Constraint[]::new));
    }
}
