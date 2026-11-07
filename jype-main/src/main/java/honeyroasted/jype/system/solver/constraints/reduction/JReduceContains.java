package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JRelationTrees;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

public class JReduceContains extends ConstraintMapper.Unary<JTypeConstraints.Contains> {
    private final JSolverConfig config;

    public JReduceContains() {
        this(JSolverConfig.FAST);
    }

    public JReduceContains(JSolverConfig config) {
        this.config = config;
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        JType s = constraint.left();
        JType t = constraint.right();
        JType object = s.typeSystem().constants().object();

        if (s.isProperType() && t.isProperType()) {
            JRelationTrees.ask(branch, constraint, this.config);
        } else if (!(t instanceof JWildType)) {
            if (s instanceof JWildType) {
                // JLS §18.2.3 T is a type and S is a wildcard
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                // JLS §18.2.3 ‹S = T›
                reduceTo(branch, constraint, new JTypeConstraints.Equal(s, t));
            }
        } else if (t instanceof JWildType.Upper tu && tu.hasDefaultBounds()) {
            // JLS §18.2.3 T is ?
            branch.set(constraint, Constraint.Status.TRUE);
        } else if (t instanceof JWildType.Upper tu) {
            JType tb = tu.upperBound();
            if (s instanceof JWildType.Upper su) {
                // JLS §18.2.3 ? extends S' <= ? extends T' (? is ? extends Object)
                reduceTo(branch, constraint, new JTypeConstraints.Subtype(su.hasDefaultBounds() ? object : su.upperBound(), tb));
            } else if (s instanceof JWildType.Lower) {
                // JLS §18.2.3 ? super S' <= ? extends T'
                reduceTo(branch, constraint, new JTypeConstraints.Equal(object, tb));
            } else {
                // JLS §18.2.3 S <= ? extends T'
                reduceTo(branch, constraint, new JTypeConstraints.Subtype(s, tb));
            }
        } else {
            JType tb = ((JWildType.Lower) t).lowerBound();
            if (s instanceof JWildType.Lower sl) {
                // JLS §18.2.3 ? super S' <= ? super T'
                reduceTo(branch, constraint, new JTypeConstraints.Subtype(tb, sl.lowerBound()));
            } else if (s instanceof JWildType) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                // JLS §18.2.3 S <= ? super T'
                reduceTo(branch, constraint, new JTypeConstraints.Subtype(tb, s));
            }
        }
    }

    private static void reduceTo(ConstraintBranch branch, Constraint constraint, Constraint result) {
        branch.set(constraint, Constraint.Status.TRUE);
        branch.add(result);
    }
}
