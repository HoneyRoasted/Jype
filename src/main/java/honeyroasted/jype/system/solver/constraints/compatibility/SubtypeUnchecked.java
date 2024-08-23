package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeUnchecked implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return left.isProperType() && right.isProperType() &&
                left instanceof ClassType l && right instanceof ClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) || (!l.hasAnyTypeArguments() && r.hasAnyTypeArguments()));
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ClassType l = (ClassType) left;
        ClassType r = (ClassType) right;

        node.expandInPlace(ConstraintNode.Operation.OR).attach(new TypeConstraints.Subtype(l.classReference(), r.classReference()));
    }
}
