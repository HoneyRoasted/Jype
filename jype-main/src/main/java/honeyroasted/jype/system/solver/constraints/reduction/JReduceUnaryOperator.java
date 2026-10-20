package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reduces a unary operator expression (JLS §15.15). The operand's type is resolved directly when
 * it's already simply typed (re-threading its own {@code ExpressionCompatible} constraint so a
 * nested cast/operator/etc. still gets checked), or - mirroring how {@link JReduceMethodInvocation}
 * and {@link JReduceGetField} handle a non-simply-typed receiver/source - via its own sub-solve
 * against a fresh metavariable otherwise. {@link JExpressionInformation.UnaryOperator#resultType}
 * is then applied to the resolved type(s); an operand type the operator doesn't accept (or a
 * sub-solve with no true branch) makes the whole expression false.
 */
public class JReduceUnaryOperator extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.UnaryOperator;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.UnaryOperator unary = (JExpressionInformation.UnaryOperator) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JExpressionInformation operand = unary.operand();

        if (operand.isSimplyTyped()) {
            JType operandType = operand.getSimpleType(system, Function.identity()).get();
            Optional<JType> resultType = JExpressionInformation.UnaryOperator.resultType(unary.kind(), operandType, system);

            if (resultType.isEmpty()) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                branch.drop(constraint)
                        .add(new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, operandType))
                        .add(new JTypeConstraints.Compatible(resultType.get(), constraint.middle(), constraint.right()));
            }
            return;
        }

        JMetaVarType operandTarget = system.typeFactory().newMetaVarType("OPERAND");
        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, operandTarget))
                .solve();

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        solved.branches().forEach(childBranch -> {
            if (childBranch.status().isTrue()) {
                JType inst = JResolveBounds.findInstantiation(operandTarget, childBranch);
                inst = JMemberLookup.captureConvert(childBranch, inst); // JLS §5.1.10
                childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(operandTarget));

                JExpressionInformation.UnaryOperator.resultType(unary.kind(), inst, system)
                        .ifPresent(resultType -> newChildren.add(combine(childBranch, resultSnapshot(resultType, constraint))));
            }
        });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static ConstraintBranch.Snapshot resultSnapshot(JType resultType, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
        constraints.put(new JTypeConstraints.Compatible(resultType, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
