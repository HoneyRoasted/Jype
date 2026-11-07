package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.expression.JExpressionSubstitution;
import honeyroasted.jype.system.solver.JFunctionalInterfaces;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JReduceLambdaExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Lambda;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Lambda lambda = (JExpressionInformation.Lambda) constraint.left();
        JType target = constraint.right();
        JTypeSystem system = target.typeSystem();

        if (target instanceof JMetaVarType mvt) {
            // JLS §18.5.2.2 the target is an input variable that must be resolved first
            defer(branch, constraint, Set.of(mvt));
            return;
        }
        if (!(target instanceof JClassType) && !(target instanceof JIntersectionType)) {
            // JLS §15.27.3 the target must be a functional interface type
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        Optional<JType> ground = groundTargetType(lambda, target);
        if (ground.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }
        Optional<JMethodReference> samOpt = JFunctionalInterfaces.functionType(ground.get());
        if (samOpt.isEmpty() || !samOpt.get().typeParameters().isEmpty()) {
            // JLS §15.27.3 the function type must exist and must not be generic
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }
        JMethodReference sam = samOpt.get();
        List<JType> samParams = sam.parameters();
        if (lambda.parameterCount() != samParams.size()) {
            // JLS §15.27.3 arity must match
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        List<JType> parameterTypes = new ArrayList<>();
        List<Constraint> sub = new ArrayList<>();
        boolean explicit = lambda.isExplicitlyTyped() && lambda.parameterCount() > 0;
        if (explicit) {
            for (int i = 0; i < lambda.parameterCount(); i++) {
                // JLS §18.2.1 ‹Fi = Gi› for explicitly typed parameters
                sub.add(new JTypeConstraints.Equal(lambda.explicitParameterTypes().get(i), samParams.get(i)));
                parameterTypes.add(lambda.explicitParameterTypes().get(i));
            }
        } else {
            Set<JMetaVarType> inputs = new LinkedHashSet<>();
            samParams.forEach(p -> inputs.addAll(metaVars(p)));
            if (!inputs.isEmpty()) {
                Map<JMetaVarType, JType> inst = new LinkedHashMap<>();
                for (JMetaVarType in : inputs) {
                    JType i = JResolveBounds.findInstantiation(in, branch);
                    if (i == null) {
                        // JLS §18.5.2.2 an implicitly typed lambda waits for its parameter types
                        defer(branch, constraint, inputs);
                        return;
                    }
                    inst.put(in, i);
                }
                JMetaVarTypeResolveVisitor theta = new JMetaVarTypeResolveVisitor(inst);
                samParams = samParams.stream().map(theta::visit).toList();
            }
            parameterTypes.addAll(samParams);
        }

        Map<JExpressionInformation.LambdaParameter, JExpressionInformation> replacements = new HashMap<>();
        for (int i = 0; i < lambda.parameterCount(); i++) {
            replacements.put(lambda.parameters().get(i), JExpressionInformation.of(parameterTypes.get(i)));
        }
        JExpressionInformation.LambdaBody body = JExpressionSubstitution.body(lambda.lambdaBody(), replacements);

        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(system.constants().voidType());
        if (body instanceof JExpressionInformation.LambdaBody.Expression e) {
            if (returnsVoid) {
                if (!isStatementExpression(e.expression())) {
                    // JLS §15.27.3 a void function type needs a statement expression body
                    branch.set(constraint, Constraint.Status.FALSE);
                    return;
                }
                sub.add(new JTypeConstraints.ExpressionCompatible(e.expression(), JTypeConstraints.Compatible.Context.ASSIGNMENT, system.constants().voidType()));
            } else {
                // JLS §18.2.1 ‹e → R›
                sub.add(new JTypeConstraints.ExpressionCompatible(e.expression(), JTypeConstraints.Compatible.Context.ASSIGNMENT, samReturn));
            }
        } else {
            JExpressionInformation.LambdaBody.Block b = (JExpressionInformation.LambdaBody.Block) body;
            if (returnsVoid ? !b.isVoidCompatible() : !b.isValueCompatible()) {
                // JLS §15.27.2 void- or value-compatible block body
                branch.set(constraint, Constraint.Status.FALSE);
                return;
            }
            for (JExpressionInformation stmt : b.statements()) {
                sub.add(new JTypeConstraints.ExpressionCompatible(stmt, JTypeConstraints.Compatible.Context.ASSIGNMENT, system.constants().voidType()));
            }
            for (JExpressionInformation ret : b.returnExpressions()) {
                // JLS §18.2.1 ‹ei → R› for each result expression
                sub.add(new JTypeConstraints.ExpressionCompatible(ret, JTypeConstraints.Compatible.Context.ASSIGNMENT, samReturn));
            }
        }

        List<JExpressionInformation> roots = new ArrayList<>();
        if (body instanceof JExpressionInformation.LambdaBody.Block b) {
            roots.addAll(b.statements());
        }
        roots.addAll(body.resultExpressions());
        for (JExpressionInformation invocation : invocations(roots)) {
            // JLS §18.2.5 checked exceptions thrown by the body
            sub.add(new JTypeConstraints.LambdaThrowsCheck(invocation, sam.exceptionTypes()));
        }

        branch.set(constraint, Constraint.Status.TRUE);
        sub.forEach(branch::add);
    }

    private static void defer(ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Set<JMetaVarType> inputs) {
        branch.drop(constraint);
        branch.add(new JTypeConstraints.Deferred(constraint, inputs), Constraint.Status.ASSUMED);
    }

    public static boolean isStatementExpression(JExpressionInformation expr) {
        // JLS §14.8 statement expressions
        return expr instanceof JExpressionInformation.MethodInvocation<?> || expr instanceof JExpressionInformation.Instantiation
                || expr instanceof JExpressionInformation.Assignment || expr instanceof JExpressionInformation.IncrementDecrement;
    }

    private static List<JExpressionInformation> invocations(List<JExpressionInformation> roots) {
        List<JExpressionInformation> result = new ArrayList<>();
        for (JExpressionInformation root : roots) {
            collectInvocations(root, result);
        }
        return result;
    }

    private static void collectInvocations(JExpressionInformation expr, List<JExpressionInformation> result) {
        if (expr == null || expr instanceof JExpressionInformation.Lambda) {
            return;
        }
        if (expr instanceof JExpressionInformation.Invocation inv) {
            result.add(expr);
            inv.parameters().forEach(p -> collectInvocations(p, result));
            if (expr instanceof JExpressionInformation.MethodInvocation<?> mi && mi.source() instanceof JExpressionInformation src) {
                collectInvocations(src, result);
            }
        } else if (expr instanceof JExpressionInformation.GetField<?> gf && gf.source() instanceof JExpressionInformation src) {
            collectInvocations(src, result);
        } else if (expr instanceof JExpressionInformation.Cast c) {
            collectInvocations(c.operand(), result);
        } else if (expr instanceof JExpressionInformation.InstanceOf i) {
            collectInvocations(i.operand(), result);
        } else if (expr instanceof JExpressionInformation.UnaryOperator u) {
            collectInvocations(u.operand(), result);
        } else if (expr instanceof JExpressionInformation.BinaryOperator b) {
            collectInvocations(b.left(), result);
            collectInvocations(b.right(), result);
        } else if (expr instanceof JExpressionInformation.Conditional c) {
            collectInvocations(c.condition(), result);
            collectInvocations(c.whenTrue(), result);
            collectInvocations(c.whenFalse(), result);
        } else if (expr instanceof JExpressionInformation.ArrayCreation a) {
            a.dimensions().forEach(d -> collectInvocations(d, result));
            collectInvocations(a.initializer(), result);
        } else if (expr instanceof JExpressionInformation.ArrayInitializer a) {
            a.elements().forEach(e -> collectInvocations(e, result));
        } else if (expr instanceof JExpressionInformation.ArrayAccess a) {
            collectInvocations(a.array(), result);
            collectInvocations(a.index(), result);
        } else if (expr instanceof JExpressionInformation.Assignment a) {
            collectInvocations(a.variable(), result);
            collectInvocations(a.value(), result);
        } else if (expr instanceof JExpressionInformation.IncrementDecrement i) {
            collectInvocations(i.variable(), result);
        } else if (expr instanceof JExpressionInformation.Switch sw) {
            collectInvocations(sw.selector(), result);
            sw.results().forEach(r -> collectInvocations(r, result));
        }
    }

    private static Optional<JType> groundTargetType(JExpressionInformation.Lambda lambda, JType target) {
        if (!(target instanceof JParameterizedClassType pct) || !JTypeRelations.hasWildcardArguments(pct)) {
            return Optional.of(target);
        }
        if (!lambda.isExplicitlyTyped() || lambda.parameterCount() == 0) {
            // JLS §15.27.3 implicitly typed: the non-wildcard parameterization (§9.9)
            return JFunctionalInterfaces.nonWildcardParameterization(target);
        }
        // JLS §18.5.3 functional interface parameterization inference
        JTypeSystem system = target.typeSystem();
        List<JVarType> params = pct.classReference().typeParameters();
        Map<JVarType, JType> theta = new LinkedHashMap<>();
        List<Constraint> constraints = new ArrayList<>();
        for (JVarType p : params) {
            JMetaVarType alpha = p.createMetaVar();
            theta.put(p, alpha);
            constraints.add(new JTypeConstraints.Infer(alpha, p));
        }
        JParameterizedClassType withVars = pct.classReference().parameterized(params.stream().map(p -> (JArgumentType) theta.get(p)).toList());
        Optional<JMethodReference> sam = JFunctionalInterfaces.functionType(withVars);
        if (sam.isEmpty() || sam.get().parameters().size() != lambda.parameterCount()) {
            return Optional.empty();
        }
        for (int i = 0; i < lambda.parameterCount(); i++) {
            constraints.add(new JTypeConstraints.Equal(lambda.explicitParameterTypes().get(i), sam.get().parameters().get(i)));
        }
        ConstraintTree tree = system.operations().inferenceSolver().bind(constraints.toArray(Constraint[]::new)).solve();
        Optional<ConstraintBranch> solved = tree.branches().stream().filter(b -> b.status().isTrue()).findFirst();
        if (solved.isEmpty()) {
            return Optional.empty();
        }
        List<JArgumentType> args = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            JArgumentType ai = pct.typeArguments().get(i);
            JMetaVarType alpha = (JMetaVarType) theta.get(params.get(i));
            JType inferred = JResolveBounds.findInstantiation(alpha, solved.get());
            if (ai instanceof JWildType && inferred != null && mentioned(sam.get(), alpha)) {
                args.add((JArgumentType) inferred);
            } else {
                args.add(ai);
            }
        }
        JParameterizedClassType result = pct.classReference().parameterized(args);
        if (JTypeRelations.hasWildcardArguments(result)) {
            return JFunctionalInterfaces.nonWildcardParameterization(result);
        }
        return Optional.of(result);
    }

    private static boolean mentioned(JMethodReference sam, JMetaVarType alpha) {
        for (JType p : sam.parameters()) {
            if (metaVars(p).contains(alpha)) {
                return true;
            }
        }
        return false;
    }

    private static Set<JMetaVarType> metaVars(JType type) {
        return new LinkedHashSet<>(new JRecursiveTypeVisitor<JMetaVarType, Void>((JTypeVisitor.Default) (t, c) -> t instanceof JMetaVarType m ? m : null, null, false)
                .visit(type, new HashMap<>()));
    }

    public static Optional<JMethodReference> findFunctionalMethod(JType targetType) {
        return JFunctionalInterfaces.functionType(targetType);
    }
}
