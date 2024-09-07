package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JReduceMethodInvocation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.MethodInvocation<?> invoc &&
                (invoc.source() instanceof JClassReference || invoc.source() instanceof JExpressionInformation);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);

        JExpressionInformation.MethodInvocation<?> invocation = (JExpressionInformation.MethodInvocation<?>) constraint.left();
        JType target = mapper.apply(constraint.right());
        JTypeSystem system = target.typeSystem();

        JClassReference declaring = invocation.declaring();
        List<JExpressionInformation> parameters = invocation.parameters();

        Map<JMethodLocation, JMethodReference> methods = new LinkedHashMap<>();
        boolean stat;

        if (invocation.source() instanceof JClassReference ref) { //Static method call
            system.expressionInspector().getAllMethods(ref).ifPresent(methods::putAll);
            stat = true;
        } else if (invocation.source() instanceof JExpressionInformation expr) { //Instance method call
            if (expr.isSimplyTyped()) {
                JType type = expr.getSimpleType(system, mapper).get();
                findClassTypes(type).forEach(ct -> system.expressionInspector().getAllMethods(ct.classReference()).ifPresent(methods::putAll));
                stat = false;
            } else {
                JMetaVarType mvt = system.typeFactory().newMetaVarType("RET_" + invocation.name());
                ConstraintTree solved = system.operations().inferenceSolver()
                        .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, mvt))
                        .solve();

                List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
                solved.branches().forEach(childBranch -> {
                    if (childBranch.status().isTrue()) {
                        JType inst = JResolveBounds.findInstantiation(mvt, childBranch);
                        findClassTypes(inst).forEach(ct -> system.expressionInspector().getAllMethods(ct.classReference()).ifPresent(map -> map.forEach((loc, ref) -> {
                            if (ref.location().name().equals(invocation.name()) && ref.outerClass().accessFrom(declaring).canAccess(ref.access())) {
                                if (ref.parameters().size() == parameters.size()) {
                                    newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters));
                                    newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters));
                                }

                                if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                                        ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                                    newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg));
                                    newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg));
                                }
                            }
                        })));
                    } else {
                        newChildren.add(childBranch.snapshot());
                    }
                });

                if (!newChildren.isEmpty()) {
                    branch.drop(constraint).divergeBranches(newChildren);
                } else {
                    branch.set(constraint, Constraint.Status.FALSE);
                }
                return;
            }
        } else {
            stat = false;
        }

        if (methods.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();

            methods.forEach((loc, ref) -> {
                if (ref.location().name().equals(invocation.name()) && ref.outerClass().accessFrom(declaring).canAccess(ref.access()) && (!stat || ref.hasModifier(AccessFlag.STATIC))) {
                    if (ref.parameters().size() == parameters.size()) {
                        newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters));
                        newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters));
                    }

                    if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                            ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                        newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg));
                        newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg));
                    }
                }
            });

            if (!newChildren.isEmpty()) {
                branch.drop(constraint).divergeBranches(newChildren);
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        }
    }

    private static ConstraintBranch.Snapshot createInvoke(JTypeConstraints.Compatible.Context context, JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.MethodInvocation<?> invocation, JMethodReference ref, List<JExpressionInformation> parameters) {
        Map<Constraint, Constraint.Status> invoke = new HashMap<>();
        Pair<Set<Constraint>, JVarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        JVarTypeResolveVisitor resolve = typeParams.right();

        typeParams.left().forEach(c -> invoke.put(c, Constraint.Status.UNKNOWN));
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(ref.returnType()), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        for (int i = 0; i < parameters.size(); i++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(i), context, resolve.apply(ref.parameters().get(i))), Constraint.Status.UNKNOWN);
        }
        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(invocation, ref, context, false)), invoke);
    }

    private static ConstraintBranch.Snapshot createVarargInvoke(JTypeConstraints.Compatible.Context context, JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.MethodInvocation<?> invocation, JMethodReference ref, List<JExpressionInformation> parameters, JArrayType vararg) {
        Map<Constraint, Constraint.Status> invoke = new HashMap<>();
        Pair<Set<Constraint>, JVarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        JVarTypeResolveVisitor resolve = typeParams.right();

        typeParams.left().forEach(c -> invoke.put(c, Constraint.Status.UNKNOWN));
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(ref.returnType()), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);

        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(ref.parameters().get(index))), Constraint.Status.UNKNOWN);
        }

        for(; index < parameters.size(); index++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(vararg.component())), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(invocation, ref, context, true)), invoke);
    }

    private static Pair<Set<Constraint>, JVarTypeResolveVisitor> createTypeParams(JMethodReference ref, JExpressionInformation.MethodInvocation<?> invoc) {
        Set<Constraint> newConstraints = new LinkedHashSet<>();
        Map<JVarType, JType> resolution = new LinkedHashMap<>();

        if (invoc.explicitTypeArguments().isEmpty() || invoc.explicitTypeArguments().size() != ref.typeParameters().size()) {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                JVarType vt = ref.typeParameters().get(i);
                JMetaVarType mvt = vt.createMetaVar();

                newConstraints.add(new JTypeConstraints.Infer(mvt, vt));
            }
        } else {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                resolution.put(ref.typeParameters().get(i), invoc.explicitTypeArguments().get(i));
            }
        }


        return Pair.of(newConstraints, new JVarTypeResolveVisitor(resolution));
    }

    private static Set<JClassType> findClassTypes(JType type) {
        if (type instanceof JClassType ct) {
            return Set.of(ct);
        } else {
            Set<JType> working = Set.of(type);
            Set<JClassType> attempt;

            do {
                attempt = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).filter(t -> t instanceof JClassType)
                        .map(t -> (JClassType) t).collect(Collectors.toCollection(LinkedHashSet::new));
                working = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
            } while (attempt.isEmpty() && !working.isEmpty());

            return attempt;
        }
    }

}