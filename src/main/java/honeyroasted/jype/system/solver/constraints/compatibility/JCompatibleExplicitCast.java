package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class JCompatibleExplicitCast extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        return status.isUnknown() && constraint.middle() == EXPLICIT_CAST && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        branch.drop(constraint)
                .diverge(new JTypeConstraints.Compatible(constraint.left(), LOOSE_INVOCATION, constraint.right()),
                        new JTypeConstraints.Compatible(constraint.right(), LOOSE_INVOCATION, constraint.left()));
    }
}
