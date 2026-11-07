package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JOperatorTypes;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.List;

public class JReduceArrayAccess extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.ArrayAccess;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.ArrayAccess access = (JExpressionInformation.ArrayAccess) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        List<ConstraintBranch.Snapshot> children = new ArrayList<>();
        for (JStandaloneType array : JStandaloneTyping.resolve(access.array(), system)) {
            if (!(array.type() instanceof JArrayType at)) {
                // JLS §15.10.3 the array reference expression must have an array type
                continue;
            }
            for (JStandaloneType index : JStandaloneTyping.resolve(access.index(), system)) {
                // JLS §15.10.3 the index undergoes unary numeric promotion and must be int
                if (JOperatorTypes.unaryNumericPromotion(index.type(), system).filter(p -> p.typeEquals(system.constants().intType())).isEmpty()) {
                    continue;
                }
                JType component = at.component();
                JType result = component instanceof JClassType ct && component.isProperType() ? JTypeRelations.capture(ct) : component;
                children.add(JReduceOperands.snapshot(List.of(array, index), access, result, constraint));
            }
        }
        if (children.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).divergeBranches(children);
        }
    }
}
