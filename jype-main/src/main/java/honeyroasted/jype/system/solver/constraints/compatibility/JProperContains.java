package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

public class JProperContains extends ConstraintMapper.Unary<JTypeConstraints.Contains> {
    private final JSolverConfig config;

    public JProperContains() {
        this(JSolverConfig.FAST);
    }

    public JProperContains(JSolverConfig config) {
        this.config = config;
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isProperType() && constraint.right().isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        JTypeRelations.ask(branch, constraint, this.config);
    }
}
