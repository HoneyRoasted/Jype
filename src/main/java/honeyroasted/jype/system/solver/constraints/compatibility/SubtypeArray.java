package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeArray extends ConstraintMapper.Unary<TypeConstraints.Subtype> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left instanceof ArrayType && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ArrayType l = (ArrayType) left;
        Type supertype = right;
        if (supertype instanceof ArrayType r) {
            if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                branch.drop(constraint).add(new TypeConstraints.Equal(l.component(), r.component()));
            } else {
                branch.drop(constraint).add(new TypeConstraints.Subtype(l.component(), r.component()));
            }
        } else {
            TypeConstants c = supertype.typeSystem().constants();
            branch.drop(constraint)
                    .diverge(new TypeConstraints.Subtype(c.object(), supertype),
                            new TypeConstraints.Subtype(c.cloneable(), supertype),
                            new TypeConstraints.Subtype(c.serializable(), supertype));
        }
    }
}
