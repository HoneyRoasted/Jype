package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceEqual implements ConstraintMapper.Unary<TypeConstraints.Equal> {
    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Equal constraint) {
        return node.isLeaf();
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Equal constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type s = mapper.apply(constraint.left());
        Type t = mapper.apply(constraint.right());

        if (s.isProperType() && t.isProperType()) {
            node.overrideStatus(s.typeEquals(t));
        } else if (s.isNullType() || t.isNullType()) {
            node.overrideStatus(false);
        } else if ((t instanceof MetaVarType && !(s instanceof PrimitiveType)) ||
                (s instanceof MetaVarType && !(t instanceof PrimitiveType))) {
            node.overrideStatus(true);
        }
    }
}
