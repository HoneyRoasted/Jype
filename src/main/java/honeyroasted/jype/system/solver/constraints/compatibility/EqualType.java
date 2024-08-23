package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class EqualType implements ConstraintMapper.Unary<TypeConstraints.Equal> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Equal constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return left.isProperType() && right.isProperType();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Equal constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        node.overrideStatus(left.typeEquals(right));
    }
}
