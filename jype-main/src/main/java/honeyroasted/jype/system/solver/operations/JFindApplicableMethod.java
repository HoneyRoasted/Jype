package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JFindApplicableMethod implements JTypeOperation<JExpressionInformation.MethodInvocation<?>, JParameterizedMethodType> {

    @Override
    public JParameterizedMethodType apply(JTypeSystem system, JExpressionInformation.MethodInvocation<?> invocation) {
        // JLS §15.12.2 compile-time declaration selection without a target type
        ConstraintTree tree = system.operations().inferenceSolver(JSolverConfig.FAST)
                .bind(new JTypeConstraints.ExpressionCompatible(invocation, JTypeConstraints.Compatible.Context.STANDALONE, system.constants().voidType()))
                .solve();

        JParameterizedMethodType found = null;
        for (ConstraintBranch branch : tree.branches()) {
            if (!branch.status().isTrue()) {
                continue;
            }
            Optional<JParameterizedMethodType> result = this.resultOf(invocation, branch);
            if (result.isEmpty()) {
                continue;
            }
            if (found != null && !found.methodReference().equals(result.get().methodReference())) {
                return null;
            }
            found = result.get();
        }
        return found;
    }

    private Optional<JParameterizedMethodType> resultOf(JExpressionInformation.MethodInvocation<?> invocation, ConstraintBranch branch) {
        JMethodReference chosen = null;
        for (JTypeContext.ChosenMethod cm : branch.metadata().all(JTypeContext.ChosenMethod.class)) {
            if (cm.expression() == invocation) {
                chosen = cm.chosen();
            }
        }
        if (chosen == null) {
            return Optional.empty();
        }
        if (chosen.typeParameters().isEmpty()) {
            return Optional.of(chosen.parameterizedWithTypeVars());
        }
        Map<JVarType, JType> theta = null;
        for (JTypeContext.InvocationTypeArguments ita : branch.metadata().all(JTypeContext.InvocationTypeArguments.class)) {
            if (ita.expression() == invocation) {
                theta = ita.theta();
            }
        }
        if (theta == null) {
            return Optional.empty();
        }
        Map<JMetaVarType, JType> instantiations = JStandaloneTyping.instantiationsOf(branch);
        JMetaVarTypeResolveVisitor resolver = new JMetaVarTypeResolveVisitor(instantiations);
        List<JArgumentType> typeArguments = new ArrayList<>();
        for (JVarType p : chosen.typeParameters()) {
            JType arg = theta.get(p);
            JType resolved = arg == null ? p : resolver.visit(arg);
            if (!resolved.isProperType() || !(resolved instanceof JArgumentType argType)) {
                return Optional.empty();
            }
            typeArguments.add(argType);
        }
        return Optional.of(chosen.parameterized(typeArguments));
    }
}
