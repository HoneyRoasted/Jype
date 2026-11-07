package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.List;

public class JReduceLambdaThrowsCheck extends ConstraintMapper.Unary<JTypeConstraints.LambdaThrowsCheck> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.LambdaThrowsCheck constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.LambdaThrowsCheck constraint, Constraint.Status status) {
        List<JType> thrown = null;
        for (JTypeContext.ThrownTypes tt : branch.metadata().all(JTypeContext.ThrownTypes.class)) {
            if (tt.expression() == constraint.left()) {
                thrown = tt.types();
                break;
            }
        }
        if (thrown == null) {
            return;
        }
        branch.set(constraint, Constraint.Status.TRUE);
        List<JType> samThrows = constraint.right();
        for (JType exc : thrown) {
            if (!exc.isProperType()) {
                continue;
            }
            JTypeSystem system = exc.typeSystem();
            if (JMemberLookup.isUncheckedException(exc)) {
                continue;
            }
            boolean covered = samThrows.stream().anyMatch(t -> t.isProperType() && system.operations().isSubtype(exc, t));
            if (covered) {
                continue;
            }
            List<JMetaVarType> vars = samThrows.stream().filter(t -> t instanceof JMetaVarType).map(t -> (JMetaVarType) t).toList();
            if (vars.isEmpty()) {
                // JLS §18.2.5 a checked exception not covered by the function type's throws clause
                branch.add(new JTypeConstraints.Contradiction(constraint, constraint), Constraint.Status.FALSE);
                return;
            }
            for (JMetaVarType v : vars) {
                // JLS §18.2.5 ‹E <: αj› and throws αj
                branch.add(new JTypeConstraints.Throws(v), Constraint.Status.TRUE);
                branch.add(new JTypeConstraints.Subtype(exc, v));
            }
        }
    }
}
