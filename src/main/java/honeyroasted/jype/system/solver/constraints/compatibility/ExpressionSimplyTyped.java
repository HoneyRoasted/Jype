package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class ExpressionSimplyTyped extends ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isSimplyTyped() && !(constraint.middle() == ASSIGNMENT && constraint.left() instanceof ExpressionInformation.Constant);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);

        Type supertype = mapper.apply(constraint.right());
        branch.drop(constraint)
                .add(new TypeConstraints.Compatible(constraint.left().getSimpleType(supertype.typeSystem(), mapper).get(), constraint.middle(), supertype));
    }
}
