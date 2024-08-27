package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ReduceInstantiation implements ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {

    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        return node.isLeaf() && constraint.left() instanceof ExpressionInformation.Instantiation;
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);

        Type left = mapper.apply(constraint.right());
        ExpressionInformation.Instantiation inst = (ExpressionInformation.Instantiation) constraint.left();
        TypeSystem system = left.typeSystem();

        ClassReference target = inst.type();
        ClassReference declaring = inst.declaring();

        List<ExpressionInformation> parameters = new ArrayList<>();
        if (target.hasRelevantOuterType()) {
            Optional<ClassType> outerType = system.operations().outerTypeFromDeclaring(target, declaring);
            if (outerType.isPresent()) {
                parameters.add(ExpressionInformation.of(outerType.get()));
            } else {
                node.expandInPlace(ConstraintNode.Operation.AND, true)
                                .attach(new TypeConstraints.LackingOuterType(inst));
                node.overrideStatus(false);
                return;
            }
        }
        parameters.addAll(inst.parameters());

        Optional<Map<MethodLocation, MethodReference>> consOpt = system.expressionInspector().getDeclaredConstructors(inst.type());
        if (!consOpt.isPresent()) {
            node.expandInPlace(ConstraintNode.Operation.AND, true)
                            .attach(new TypeConstraints.MethodNotFound(inst.type(), "<init>"));
        } else {
            Set<Constraint> typeParams = new LinkedHashSet<>();
            if (inst.explicitTypeArguments().isEmpty() || inst.explicitTypeArguments().size() != target.typeParameters().size()) {
                for (int i = 0; i < target.typeParameters().size(); i++) {
                    VarType vt = target.typeParameters().get(i);
                    typeParams.add(new TypeConstraints.Infer(vt.createMetaVar(), vt));
                }
            }

            Set<ConstraintNode> newChildren = new LinkedHashSet<>();
            ClassType result = typeParams.isEmpty() ? target.parameterized(inst.explicitTypeArguments()) :
                    target.parameterizedWithMetaVars();

            consOpt.get().forEach((loc, ref) -> {
                if (ref.parameters().size() == parameters.size()) {
                    newChildren.add(createInvoke(TypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, constraint, ref, result, parameters));
                    newChildren.add(createInvoke(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, constraint, ref, result, parameters));
                }

                if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                        ref.parameters().get(ref.parameters().size() - 1) instanceof ArrayType vararg) {
                    newChildren.add(createVarargInvoke(TypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, constraint, ref, result, parameters, vararg));
                    newChildren.add(createVarargInvoke(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, constraint, ref, result, parameters, vararg));
                }
            });

            if (!newChildren.isEmpty()) {
                node.expand(ConstraintNode.Operation.OR, newChildren, false);
            } else {
                node.expandInPlace(ConstraintNode.Operation.AND, true)
                                .attach(new TypeConstraints.MethodNotFound(inst.type(), "<init>"));
            }
        }
    }

    private static ConstraintTree createInvoke(TypeConstraints.Compatible.Context context, Set<Constraint> typeParams, TypeConstraints.ExpressionCompatible constraint, MethodReference ref, Type result, List<ExpressionInformation> parameters) {
        ConstraintTree invoke = new ConstraintTree(new TypeConstraints.MethodInvocation(ref, context, false), ConstraintNode.Operation.AND);
        invoke.preserve();
        invoke.attach(typeParams.toArray(Constraint[]::new));
        invoke.attach(new TypeConstraints.Compatible(result, constraint.middle(), constraint.right()));
        for (int i = 0; i < parameters.size(); i++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(i), context, ref.parameters().get(i)));
        }

        return invoke;
    }

    private static ConstraintTree createVarargInvoke(TypeConstraints.Compatible.Context context, Set<Constraint> typeParams, TypeConstraints.ExpressionCompatible constraint, MethodReference ref, Type result, List<ExpressionInformation> parameters, ArrayType vararg) {
        ConstraintTree invoke = new ConstraintTree(new TypeConstraints.MethodInvocation(ref, context, true), ConstraintNode.Operation.AND);
        invoke.preserve();
        invoke.attach(typeParams.toArray(Constraint[]::new));
        invoke.attach(new TypeConstraints.Compatible(result, constraint.middle(), constraint.right()));
        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, ref.parameters().get(index)));
        }

        for (; index < parameters.size(); index++) {
            invoke.attach(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, vararg.component()));
        }

        return invoke;
    }

}
