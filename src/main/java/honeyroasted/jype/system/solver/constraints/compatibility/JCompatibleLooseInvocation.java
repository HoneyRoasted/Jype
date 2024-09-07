package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class JCompatibleLooseInvocation extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        return status.isUnknown() && (constraint.middle() == LOOSE_INVOCATION || constraint.middle() == ASSIGNMENT) &&
                left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        Set<Constraint> newChildren = new LinkedHashSet<>();

        newChildren.add(new JTypeConstraints.Subtype(left, right));
        if (left instanceof JPrimitiveType l && !(right instanceof JPrimitiveType)) {
            newChildren.add(new JTypeConstraints.Subtype(l.box(), right));
        } else if (right instanceof JPrimitiveType r && !(left instanceof JPrimitiveType)) {
            newChildren.add(new JTypeConstraints.Subtype(left, r.box()));
        }

        branch.drop(constraint).diverge(newChildren);
    }
}
