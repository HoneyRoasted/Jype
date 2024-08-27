package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class ExpressionSimplyTyped implements ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        return node.isLeaf() && constraint.left().isSimplyTyped() && !(constraint.middle() == ASSIGNMENT && constraint.left() instanceof ExpressionInformation.Constant);
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);

        Type supertype = mapper.apply(constraint.right());
        node.expand(ConstraintNode.Operation.OR, false, new TypeConstraints.Compatible(constraint.left().getSimpleType(supertype.typeSystem(), mapper).get(), constraint.middle(), supertype));
    }
}
