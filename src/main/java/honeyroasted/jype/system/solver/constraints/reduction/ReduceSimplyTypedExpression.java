package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceSimplyTypedExpression implements ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        return constraint.left().isSimplyTyped();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        node.expandInPlace(ConstraintNode.Operation.OR)
                .attach(new TypeConstraints.Compatible(constraint.left().getSimpleType(constraint.right().typeSystem(), mapper).get(), constraint.middle(), constraint.right()));
    }
}
