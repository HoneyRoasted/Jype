package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JRelationTrees;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

public class JProperSubtype extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    private final JSolverConfig config;

    public JProperSubtype() {
        this(JSolverConfig.FAST);
    }

    public JProperSubtype(JSolverConfig config) {
        this.config = config;
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isProperType() && constraint.right().isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JRelationTrees.ask(branch, constraint, this.config);
    }
}
