package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JReduceMethodInvocation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.MethodInvocation<?> invoc &&
                (invoc.source() instanceof JClassReference || invoc.source() instanceof JExpressionInformation);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        //TODO check access from declaring method -> method
        JExpressionInformation.MethodInvocation<?> invocation = (JExpressionInformation.MethodInvocation<?>) constraint.left();
        JType targetType = constraint.right();
        JTypeSystem system = targetType.typeSystem();

        JClassReference declaring = invocation.declaring();
        List<JExpressionInformation> parameters = invocation.parameters();

        Collection<JMethodReference> methods = new LinkedHashSet<>();
        boolean stat;

        if (invocation.source() instanceof JClassReference ref) { //Static method call
            ref.declaredMethods().stream().filter(mref -> Modifier.isStatic(mref.modifiers())).forEach(methods::add);
            stat = true;
        } else if (invocation.source() instanceof JExpressionInformation expr) { //Instance method call
            if (expr.isSimplyTyped()) {
                JType type = expr.getSimpleType(system, branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get();
                findClassTypes(type).forEach(ct -> methods.addAll(getAllMethods(ct.classReference())));
                stat = false;
            } else {
                //TODO not convinced this works
                JMetaVarType callTarget = system.typeFactory().newMetaVarType("CALL_TARGET");
                ConstraintTree solved = system.operations().inferenceSolver()
                        .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, callTarget))
                        .solve();

                List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
                solved.branches().forEach(childBranch -> {
                    if (childBranch.status().isTrue()) {
                        JType inst = JResolveBounds.findInstantiation(callTarget, childBranch);
                        childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(callTarget)); //Drop variable since it is no longer needed
                        findClassTypes(inst).forEach(ct -> getAllMethods(ct.classReference()).forEach(ref -> {
                            if (ref.location().name().equals(invocation.name()))
                                if (ref.outerClass().accessFrom(declaring).canAccess(ref.access())) {
                                    if (ref.parameters().size() == parameters.size()) {
                                        newChildren.add(combine(childBranch, createInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters)));
                                        newChildren.add(combine(childBranch, createInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters)));
                                    }

                                    if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                                            ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                                        newChildren.add(combine(childBranch, createVarargInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg)));
                                        newChildren.add(combine(childBranch, createVarargInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg)));
                                    }
                                }
                        }));
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

            methods.forEach(ref -> {
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

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }

    private static ConstraintBranch.Snapshot createInvoke(JTypeConstraints.Compatible.Context context, JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.MethodInvocation<?> invocation, JMethodReference ref, List<JExpressionInformation> parameters) {
        Map<Constraint, Constraint.Status> invoke = new HashMap<>();
        Pair<Set<Constraint>, JVarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        JVarTypeResolveVisitor resolve = typeParams.right();

        typeParams.left().forEach(c -> invoke.put(c, Constraint.Status.UNKNOWN));
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(resolve.apply(ref.returnType())), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
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
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(resolve.apply(ref.returnType())), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);

        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(ref.parameters().get(index))), Constraint.Status.UNKNOWN);
        }

        for (; index < parameters.size(); index++) {
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
                resolution.put(vt, mvt);
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

    private static Collection<JMethodReference> getAllMethods(JClassReference ref) {
        List<JMethodReference> result = new ArrayList<>();

        Set<JClassType> building = Set.of(ref);
        Set<JClassType> next = new LinkedHashSet<>();

        while (!building.isEmpty()) {
            for (JClassType curr : building) {
                curr.classReference().declaredMethods()
                        .stream().filter(mref -> result.stream().noneMatch(currRef -> isOverriddenBy(mref, currRef)))
                        .forEach(result::add);

                if (curr.superClass() != null) next.add(curr.superClass());
                next.addAll(curr.interfaces());
            }

            building = next;
            next = new LinkedHashSet<>();
        }

        return result;
    }

    private static boolean isOverriddenBy(JMethodReference left, JMethodReference right) {
        left = (JMethodReference) JTypeVisitors.ERASURE.visit(left);
        right = (JMethodReference) JTypeVisitors.ERASURE.visit(right);

        JMethodReference bridge = findBridgeMethod(left);
        if (bridge != null) {
            return isOverriddenBy(bridge, right);
        }

        return left.location().name().equals(right.location().name()) &&
                isOverridableIn(left, right.outerClass()) &&
                !JAccess.fromFlags(right.modifiers()).isMoreRestrictiveThan(JAccess.fromFlags(left.modifiers())) &&
                left.returnType().typeEquals(right.returnType()) &&
                areParametersEqual(left, right);

    }

    private static JMethodReference findBridgeMethod(JMethodReference me) {
        if (isBridge(me.modifiers())) return null;
        return me.outerClass().declaredMethods().stream()
                .map(it -> (JMethodReference) JTypeVisitors.ERASURE.visit(it))
                .filter(it -> !it.equals(me) && isBridge(it.modifiers()) && it.location().name().equals(me.location().name()) && areParametersCovariant(me, it) && it.returnType().isAssignableFrom(me.returnType()))
                .findFirst().orElse(null);
    }

    private static boolean isOverridableIn(JMethodReference me, JClassType cls) {
        if (!isOverridable(me.modifiers())) return false;
        if (!isSubclassVisible(me.modifiers())) return false;
        if (!me.outerClass().isAssignableFrom(cls)) return false;

        if (Modifier.isPublic(me.modifiers())) return true;
        if (isPackageVisible(me.modifiers()) && Objects.equals(cls.namespace().location().getPackage(), me.outerClass().namespace().location().getPackage()))
            return true;

        return false;
    }

    private static boolean areParametersCovariant(JMethodReference left, JMethodReference right) {
        List<JType> leftParams = left.parameters();
        List<JType> rightParams = right.parameters();

        if (leftParams.size() != rightParams.size()) return false;

        for (int i = 0; i < leftParams.size(); i++) {
            if (!rightParams.get(i).isAssignableFrom(leftParams.get(i))) return false;
        }

        return true;
    }

    private static boolean areParametersEqual(JMethodReference left, JMethodReference right) {
        List<JType> leftParams = left.parameters();
        List<JType> rightParams = right.parameters();

        if (leftParams.size() != rightParams.size()) return false;

        for (int i = 0; i < leftParams.size(); i++) {
            if (!rightParams.get(i).typeEquals(leftParams.get(i))) return false;
        }

        return true;
    }

    private static boolean isAccessMoreRestrictive(int left, int right) {
        return JAccess.fromFlags(left).compareTo(JAccess.fromFlags(right)) < 0;
    }

    private static boolean isOverridable(int mods) {
        return !Modifier.isStatic(mods) &&
                !Modifier.isFinal(mods) &&
                !Modifier.isPrivate(mods);
    }

    private static boolean isBridge(int mods) {
        return (mods & AccessFlag.BRIDGE.mask()) != 0;
    }

    private static boolean isPackageVisible(int mods) {
        return !Modifier.isPrivate(mods);
    }

    private static boolean isSubclassVisible(int mods) {
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

}
