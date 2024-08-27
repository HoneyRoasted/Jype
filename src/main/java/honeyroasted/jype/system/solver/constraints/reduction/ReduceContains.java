package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.function.Function;

public class ReduceContains implements ConstraintMapper.Unary<TypeConstraints.Contains> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Contains constraint) {
        return node.isLeaf();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Contains constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type s = mapper.apply(constraint.left());
        Type t = mapper.apply(constraint.right());

        if (t instanceof WildType) {
            if (t instanceof WildType.Upper wtu) {
                if (wtu.hasDefaultBounds()) {
                    node.overrideStatus(false);
                } else {
                    if (s instanceof WildType) {
                        if (s instanceof WildType.Upper swtu) {
                            if (swtu.hasDefaultBounds()) {
                                node.expandInPlace(ConstraintNode.Operation.AND, false)
                                        .attach(new TypeConstraints.Subtype(s.typeSystem().constants().object(), wtu.upperBound()));
                            } else {
                                node.expandInPlace(ConstraintNode.Operation.AND, false)
                                        .attach(new TypeConstraints.Subtype(swtu.upperBound(), wtu.upperBound()));
                            }
                        } else if (s instanceof WildType.Lower swtl) {
                            node.expandInPlace(ConstraintNode.Operation.AND, false)
                                    .attach(new TypeConstraints.Equal(s.typeSystem().constants().object(), wtu.upperBound()));
                        }
                    } else {
                        node.expandInPlace(ConstraintNode.Operation.AND, false)
                                .attach(new TypeConstraints.Subtype(s, wtu.upperBound()));
                    }
                }
            } else if (t instanceof WildType.Lower wtl) {
                if (s instanceof WildType) {
                    if (s instanceof WildType.Lower swtl) {
                        node.expandInPlace(ConstraintNode.Operation.AND, false)
                                .attach(new TypeConstraints.Subtype(wtl.lowerBound(), swtl.lowerBound()));
                    } else {
                        node.overrideStatus(false);
                    }
                } else {
                    node.expandInPlace(ConstraintNode.Operation.AND, false)
                            .attach(new TypeConstraints.Subtype(wtl.lowerBound(), s));
                }
            }
        } else {
            if (s instanceof WildType) {
                node.overrideStatus(false);
            } else {
                node.expandInPlace(ConstraintNode.Operation.AND, false)
                        .attach(new TypeConstraints.Equal(s, t));
            }
        }
    }
}
