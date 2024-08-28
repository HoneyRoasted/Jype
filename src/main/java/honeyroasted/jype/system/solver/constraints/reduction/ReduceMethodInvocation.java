package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.AccessFlag;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReduceMethodInvocation implements ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        return node.isLeaf() && constraint.left() instanceof ExpressionInformation.Invocation<?> invoc &&
                (invoc.source() instanceof ClassReference || invoc.source() instanceof ExpressionInformation);
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);

        ExpressionInformation.Invocation<?> invocation = (ExpressionInformation.Invocation<?>) constraint.left();
        Type target = mapper.apply(constraint.right());
        TypeSystem system = target.typeSystem();

        ClassReference declaring = invocation.declaring();
        List<ExpressionInformation> parameters = invocation.parameters();

        Set<ClassType> checked = new LinkedHashSet<>();
        Map<MethodLocation, MethodReference> methods = new LinkedHashMap<>();
        boolean stat;

        if (invocation.source() instanceof ClassReference ref) { //Static method call
            system.expressionInspector().getAllMethods(ref).ifPresent(methods::putAll);
            checked.add(ref);
            stat = true;
        } else if (invocation.source() instanceof ExpressionInformation expr) { //Instance method call
            if (expr.isSimplyTyped()) {
                Type type = expr.getSimpleType(system, mapper).get();
                findClassTypes(type).forEach(ct -> {
                    system.expressionInspector().getAllMethods(ct.classReference()).ifPresent(methods::putAll);
                    checked.add(ct);
                });
                stat = false;
            } else {
                MetaVarType mvt = system.typeFactory().newMetaVarType("RET_" + invocation.name());
                node.expand(ConstraintNode.Operation.AND, true,
                        new TypeConstraints.ExpressionCompatible(expr, TypeConstraints.Compatible.Context.STRICT_INVOCATION, mvt),
                        new TypeConstraints.DelayedExpressionCompatible(mvt, constraint));
                return;
            }
        } else {
            stat = false;
        }

        if (methods.isEmpty()) {
            node.expand(ConstraintNode.Operation.AND, true,
                            checked.stream().map(ct -> new TypeConstraints.MethodNotFound(ct.classReference(), invocation.name())).toArray(Constraint[]::new));
        } else {
            Set<ConstraintNode> newChildren = new LinkedHashSet<>();

            methods.forEach((loc, ref) -> {
                if (ref.location().name().equals(invocation.name()) && ref.outerClass().accessFrom(declaring).canAccess(ref.access()) && (stat || !ref.hasModifier(AccessFlag.STATIC))) {
                    if (ref.parameters().size() == parameters.size()) {
                        newChildren.add(createInvoke(TypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters));
                        newChildren.add(createInvoke(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters));
                    }

                    if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                        ref.parameters().get(ref.parameters().size() - 1) instanceof ArrayType vararg) {
                        newChildren.add(createVarargInvoke(TypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg));
                        newChildren.add(createVarargInvoke(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg));
                    }
                }
            });

            if (!newChildren.isEmpty()) {
                node.expand(ConstraintNode.Operation.OR, newChildren, false);
            } else {
                node.expand(ConstraintNode.Operation.AND, true,
                        checked.stream().map(ct -> new TypeConstraints.MethodNotFound(ct.classReference(), invocation.name())).toArray(Constraint[]::new));
            }
        }
    }

    private static ConstraintTree createInvoke(TypeConstraints.Compatible.Context context, TypeConstraints.ExpressionCompatible constraint, ExpressionInformation.Invocation<?> invocation, MethodReference ref, List<ExpressionInformation> parameters) {
        ConstraintTree invoke = new ConstraintTree(new TypeConstraints.MethodInvocation(ref, context, false), ConstraintNode.Operation.AND)
                .preserve();

        Pair<Set<Constraint>, VarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        VarTypeResolveVisitor resolve = typeParams.right();

        invoke.attach(typeParams.left().toArray(Constraint[]::new));
        invoke.attach(new TypeConstraints.Compatible(resolve.apply(ref.returnType()), constraint.middle(), constraint.right()));
        for (int i = 0; i < parameters.size(); i++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(i), context, resolve.apply(ref.parameters().get(i))));
        }
        return invoke;
    }

    private static ConstraintTree createVarargInvoke(TypeConstraints.Compatible.Context context, TypeConstraints.ExpressionCompatible constraint, ExpressionInformation.Invocation<?> invocation, MethodReference ref, List<ExpressionInformation> parameters, ArrayType vararg) {
        ConstraintTree invoke = new ConstraintTree(new TypeConstraints.MethodInvocation(ref, context, false), ConstraintNode.Operation.AND)
                .preserve();

        Pair<Set<Constraint>, VarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        VarTypeResolveVisitor resolve = typeParams.right();

        invoke.attach(typeParams.left().toArray(Constraint[]::new));
        invoke.attach(new TypeConstraints.Compatible(resolve.apply(ref.returnType()), constraint.middle(), constraint.right()));

        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(ref.parameters().get(index))));
        }

        for(; index < parameters.size(); index++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(vararg.component())));
        }
        return invoke;
    }

    private static Pair<Set<Constraint>, VarTypeResolveVisitor> createTypeParams(MethodReference ref, ExpressionInformation.Invocation<?> invoc) {
        Set<Constraint> newConstraints = new LinkedHashSet<>();
        Map<VarType, Type> resolution = new LinkedHashMap<>();

        if (invoc.explicitTypeArguments().isEmpty() || invoc.explicitTypeArguments().size() != ref.typeParameters().size()) {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                VarType vt = ref.typeParameters().get(i);
                MetaVarType mvt = vt.createMetaVar();

                newConstraints.add(new TypeConstraints.Infer(mvt, vt));
            }
        } else {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                resolution.put(ref.typeParameters().get(i), invoc.explicitTypeArguments().get(i));
            }
        }


        return Pair.of(newConstraints, new VarTypeResolveVisitor(resolution));
    }

    private static Set<ClassType> findClassTypes(Type type) {
        if (type instanceof ClassType ct) {
            return Set.of(ct);
        } else {
            Set<Type> working = Set.of(type);
            Set<ClassType> attempt;

            do {
                attempt = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).filter(t -> t instanceof ClassType)
                        .map(t -> (ClassType) t).collect(Collectors.toCollection(LinkedHashSet::new));
                working = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
            } while (attempt.isEmpty() && !working.isEmpty());

            return attempt;
        }
    }
}
