package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashMap;
import java.util.Map;

public class JReduceDeferred extends ConstraintMapper.Unary<JTypeConstraints.Deferred> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Deferred constraint, Constraint.Status status) {
        return status == Constraint.Status.ASSUMED && !branch.parent().metadata().has(JTypeConstraints.ResolutionScratch.class);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Deferred constraint, Constraint.Status status) {
        Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();
        for (JMetaVarType input : constraint.right()) {
            JType inst = JResolveBounds.findInstantiation(input, branch);
            if (inst == null) {
                return;
            }
            instantiations.put(input, inst);
        }
        // JLS §18.5.2.2 the input variables are resolved: reduce the constraint
        JTypeConstraints.ExpressionCompatible original = constraint.left();
        JType target = new JMetaVarTypeResolveVisitor(instantiations).visit(original.right());
        branch.drop(constraint);
        branch.add(new JTypeConstraints.ExpressionCompatible(original.left(), original.middle(), target));
    }
}
