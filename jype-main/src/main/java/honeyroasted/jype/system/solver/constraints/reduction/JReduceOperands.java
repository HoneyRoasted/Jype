package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JReduceOperands {
    private JReduceOperands() {
    }

    public static ConstraintBranch.Snapshot snapshot(List<JStandaloneType> operands, JExpressionInformation expression, JType resultType, JTypeConstraints.ExpressionCompatible constraint) {
        return snapshot(operands, List.of(), expression, resultType, constraint);
    }

    public static ConstraintBranch.Snapshot snapshot(List<JStandaloneType> operands, List<Constraint> extra, JExpressionInformation expression, JType resultType, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
        constraints.put(new JTypeConstraints.Compatible(resultType, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        extra.forEach(c -> constraints.putIfAbsent(c, Constraint.Status.UNKNOWN));
        for (JStandaloneType operand : operands) {
            if (operand.extra() != null) {
                constraints.putIfAbsent(operand.extra(), Constraint.Status.UNKNOWN);
            }
        }
        ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ExpressionType(expression, resultType)), constraints);
        for (JStandaloneType operand : operands) {
            if (operand.branch() != null) {
                snapshot = JStandaloneTyping.combine(operand.branch(), snapshot);
            }
        }
        return snapshot;
    }
}
