package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

public class JSubtypeArray extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left instanceof JArrayType && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        
        JType left = constraint.left();
        JType right = constraint.right();

        JArrayType l = (JArrayType) left;
        JType supertype = right;
        if (supertype instanceof JArrayType r) {
            if (l.component() instanceof JPrimitiveType || r.component() instanceof JPrimitiveType) {
                branch.drop(constraint).add(new JTypeConstraints.Equal(l.component(), r.component()));
            } else {
                branch.drop(constraint).add(new JTypeConstraints.Subtype(l.component(), r.component()));
            }
        } else {
            JTypeConstants c = supertype.typeSystem().constants();
            branch.drop(constraint)
                    .diverge(new JTypeConstraints.Subtype(c.object(), supertype),
                            new JTypeConstraints.Subtype(c.cloneable(), supertype),
                            new JTypeConstraints.Subtype(c.serializable(), supertype));
        }
    }
}
