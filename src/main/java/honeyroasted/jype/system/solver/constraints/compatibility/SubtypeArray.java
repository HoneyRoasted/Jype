package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeArray implements ConstraintMapper.Unary<TypeConstraints.Subtype> {

    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return left instanceof ArrayType && left.isProperType() && right.isProperType();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper();
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ArrayType l = (ArrayType) left;
        Type supertype = right;
        if (supertype instanceof ArrayType r) {
            if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                node.expand(ConstraintNode.Operation.OR, new TypeConstraints.Equal(l.component(), r.component()));
            } else {
                node.expand(ConstraintNode.Operation.OR, new TypeConstraints.Subtype(l.component(), r.component()));
            }
        } else {
            TypeConstants c = supertype.typeSystem().constants();
            node.expand(ConstraintNode.Operation.OR,
                    new TypeConstraints.Subtype(c.object(), supertype),
                    new TypeConstraints.Subtype(c.cloneable(), supertype),
                    new TypeConstraints.Subtype(c.serializable(), supertype));
        }
    }
}
