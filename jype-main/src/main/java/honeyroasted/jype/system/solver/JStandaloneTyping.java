package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class JStandaloneTyping {
    private JStandaloneTyping() {
    }

    public static List<JStandaloneType> resolve(JExpressionInformation expr, JTypeSystem system) {
        if (needsTarget(expr)) {
            // JLS §15.27, §15.13 lambdas and method references are only typed against a target
            return List.of();
        }
        if (expr.isSimplyTyped()) {
            Optional<JType> type = expr.getSimpleType(system, Function.identity());
            if (type.isEmpty()) {
                return List.of();
            }
            Constraint extra = new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, JTypeRelations.erasure(type.get()));
            return List.of(new JStandaloneType(null, extra, type.get()));
        }

        JMetaVarType target = system.typeFactory().newMetaVarType("STANDALONE");
        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STANDALONE, target))
                .solve();

        List<JStandaloneType> result = new ArrayList<>();
        solved.branches().forEach(childBranch -> {
            if (childBranch.status().isTrue()) {
                JType type = typeOf(expr, childBranch);
                if (type == null) {
                    type = JResolveBounds.findInstantiation(target, childBranch);
                }
                childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(target));
                if (type != null) {
                    if (type instanceof JClassType ct && type.isProperType()) {
                        // JLS §5.1.10 the type of an expression is captured
                        type = JTypeRelations.capture(ct);
                    }
                    result.add(new JStandaloneType(childBranch, null, type));
                }
            }
        });
        return result;
    }

    private static boolean needsTarget(JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.Lambda || expr instanceof JExpressionInformation.InvocationReference<?> || expr instanceof JExpressionInformation.InstantiationReference) {
            return true;
        }
        if (expr instanceof JExpressionInformation.Conditional c) {
            return needsTarget(c.whenTrue()) || needsTarget(c.whenFalse());
        }
        return false;
    }

    private static JType typeOf(JExpressionInformation expr, ConstraintBranch branch) {
        for (JTypeContext.ExpressionType et : branch.metadata().all(JTypeContext.ExpressionType.class)) {
            if (et.expression() == expr) {
                Map<JMetaVarType, JType> instantiations = instantiationsOf(branch);
                JType type = new JMetaVarTypeResolveVisitor(instantiations).visit(et.type());
                return type.isProperType() ? type : null;
            }
        }
        return null;
    }

    public static Map<JMetaVarType, JType> instantiationsOf(ConstraintBranch branch) {
        Map<JMetaVarType, JType> result = new LinkedHashMap<>();
        branch.constraints().forEach((con, status) -> {
            if (con instanceof JTypeConstraints.Instantiation inst && status.isTrue()) {
                result.put(inst.left(), inst.right());
            }
        });
        branch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(m -> m.instantiations().forEach(result::putIfAbsent));
        return result;
    }

    public static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
