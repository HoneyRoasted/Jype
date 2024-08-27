package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;
import java.util.function.Function;

public class IncorporationEqualEqual implements ConstraintMapper.Binary<TypeConstraints.Equal, TypeConstraints.Equal> {

    @Override
    public boolean filterLeft(PropertySet context, ConstraintNode node, TypeConstraints.Equal constraint) {
        return node.status() == ConstraintNode.Status.TRUE;
    }

    @Override
    public boolean filterRight(PropertySet context, ConstraintNode node, TypeConstraints.Equal constraint) {
        return node.status() == ConstraintNode.Status.TRUE;
    }

    @Override
    public boolean commutative() {
        return false;
    }

    @Override
    public void process(PropertySet context, ConstraintNode leftNode, TypeConstraints.Equal leftConstraint, ConstraintNode rightNode, TypeConstraints.Equal rightConstraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(leftNode);
        Type ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof MetaVarType || lr instanceof MetaVarType) {
            MetaVarType mvt = (MetaVarType) (ll instanceof MetaVarType ? ll : lr);
            Type otherType = ll instanceof MetaVarType ? lr : ll;
            MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));

            if (rl.typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                leftNode.expandRoot(ConstraintNode.Operation.AND, false)
                        .attach(new TypeConstraints.Equal(otherType, rr).createLeaf().overrideStatus(true));
            } else if (rr.typeEquals(mvt)) {
                leftNode.expandRoot(ConstraintNode.Operation.AND, false)
                        .attach(new TypeConstraints.Equal(otherType, rl).createLeaf().overrideStatus(true));
            } else {
                leftNode.expandRoot(ConstraintNode.Operation.AND, false)
                        .attach(new TypeConstraints.Equal(subResolver.visit(rl), subResolver.visit(rr)).createLeaf().overrideStatus(true));
            }
        }
    }
}
