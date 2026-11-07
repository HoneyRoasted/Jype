package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JFieldLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JReduceGetField extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.GetField<?> get && !get.isSimplyTyped();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.GetField<?> getField = (JExpressionInformation.GetField<?>) constraint.left();
        JType targetType = constraint.right();
        JTypeSystem system = targetType.typeSystem();

        if (!(getField.source() instanceof JExpressionInformation expr) || expr.isSimplyTyped()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMetaVarType callTarget = system.typeFactory().newMetaVarType("CALL_TARGET");
        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, callTarget))
                .solve();

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        solved.branches().forEach(childBranch -> {
            if (childBranch.status().isTrue()) {
                JType inst = JResolveBounds.findInstantiation(callTarget, childBranch);
                childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(callTarget));
                if (inst != null) {
                    JFieldLookup.instanceAccess(inst, getField.name(), getField.declaring()).ifPresent(found ->
                            newChildren.add(combine(childBranch, createGetField(constraint, getField, found))));
                }
            }
        });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static ConstraintBranch.Snapshot createGetField(JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.GetField<?> getField, JFieldLookup.Found found) {
        Map<Constraint, Constraint.Status> getfield = new LinkedHashMap<>();
        getfield.put(new JTypeConstraints.Compatible(found.type(), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        PropertySet metadata = new PropertySet();
        if (found.field() != null) {
            metadata.attach(new JTypeContext.ChosenField(getField, found.field()));
        }
        return new ConstraintBranch.Snapshot(metadata, getfield);
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
