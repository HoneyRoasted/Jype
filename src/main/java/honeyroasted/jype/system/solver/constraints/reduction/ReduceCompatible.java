package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceCompatible implements ConstraintMapper.Unary<TypeConstraints.Compatible> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        return node.isLeaf();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Compatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left.isProperType() && right.isProperType()) {
            node.expandInPlace(ConstraintNode.Operation.AND, false)
                    .attach(left.typeSystem().operations().compatibilityApplier().process(
                            node.constraint().createLeaf(),
                            new PropertySet().inheritUnique(context)));
        } else if (left instanceof PrimitiveType pt) {
            node.expandInPlace(ConstraintNode.Operation.AND, false)
                    .attach(new TypeConstraints.Compatible(pt.box(), constraint.middle(), right));
        } else if (right instanceof PrimitiveType pt) {
            node.expandInPlace(ConstraintNode.Operation.AND, false)
                    .attach(new TypeConstraints.Compatible(left, constraint.middle(), pt.box()));
        } else if (left instanceof ClassType pct && pct.hasAnyTypeArguments() && right instanceof ClassType ct && !ct.hasTypeArguments() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), ct.classReference())) {
            node.overrideStatus(true);
        } else if (left instanceof ArrayType at && at.deepComponent() instanceof ClassType pct && pct.hasAnyTypeArguments() &&
                right instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), rpct.classReference())) {
            node.overrideStatus(true);
        } else {
            node.expandInPlace(ConstraintNode.Operation.AND, false)
                    .attach(new TypeConstraints.Subtype(left, right));
        }
    }
}
