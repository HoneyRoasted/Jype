package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ReduceInstantiation extends ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof ExpressionInformation.Instantiation;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);

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
                branch.setStatus(constraint, Constraint.Status.FALSE);
                return;
            }
        }
        parameters.addAll(inst.parameters());

        Optional<Map<MethodLocation, MethodReference>> consOpt = system.expressionInspector().getAllConstructors(inst.type());
        if (!consOpt.isPresent()) {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        } else {
            Set<Constraint> typeParams = new LinkedHashSet<>();
            if (inst.explicitTypeArguments().isEmpty() || inst.explicitTypeArguments().size() != target.typeParameters().size()) {
                for (int i = 0; i < target.typeParameters().size(); i++) {
                    VarType vt = target.typeParameters().get(i);
                    typeParams.add(new TypeConstraints.Infer(vt.createMetaVar(), vt));
                }
            }

            List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
            ClassType result = typeParams.isEmpty() ? target.parameterized(inst.explicitTypeArguments()) :
                    target.parameterizedWithMetaVars();

            consOpt.get().forEach((loc, ref) -> {
                if (!ref.hasModifier(AccessFlag.STATIC) && ref.outerClass().accessFrom(declaring).canAccess(ref.access())) {
                    if (ref.parameters().size() == parameters.size()) {
                        newChildren.add(createConstruct(TypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, constraint, ref, result, parameters));
                        newChildren.add(createConstruct(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, constraint, ref, result, parameters));
                    }

                    if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                            ref.parameters().get(ref.parameters().size() - 1) instanceof ArrayType vararg) {
                        newChildren.add(createVarargConstruct(TypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, constraint, ref, result, parameters, vararg));
                        newChildren.add(createVarargConstruct(TypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, constraint, ref, result, parameters, vararg));
                    }
                }
            });

            if (!newChildren.isEmpty()) {
                branch.drop(constraint).divergeBranches(newChildren);
            } else {
                branch.setStatus(constraint, Constraint.Status.FALSE);
            }
        }
    }

    private static ConstraintBranch.Snapshot createConstruct(TypeConstraints.Compatible.Context context, Set<Constraint> typeParams, TypeConstraints.ExpressionCompatible constraint, MethodReference ref, Type result, List<ExpressionInformation> parameters) {
        Map<Constraint, Constraint.Status> branch = new HashMap<>();
        typeParams.forEach(c -> branch.put(c, Constraint.Status.UNKNOWN));
        branch.put(new TypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        for (int i = 0; i < parameters.size(); i++) {
            branch.put(new TypeConstraints.ExpressionCompatible(parameters.get(i), context, ref.parameters().get(i)), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new TypeConstraints.MethodInvocation(ref, context, false)), branch);
    }

    private static ConstraintBranch.Snapshot createVarargConstruct(TypeConstraints.Compatible.Context context, Set<Constraint> typeParams, TypeConstraints.ExpressionCompatible constraint, MethodReference ref, Type result, List<ExpressionInformation> parameters, ArrayType vararg) {
        Map<Constraint, Constraint.Status> branch = new HashMap<>();
        typeParams.forEach(c -> branch.put(c, Constraint.Status.UNKNOWN));
        branch.put(new TypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            branch.put(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, ref.parameters().get(index)), Constraint.Status.UNKNOWN);
        }

        for (; index < parameters.size(); index++) {
            branch.put(new TypeConstraints.ExpressionCompatible(parameters.get(index), context, vararg.component()), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new TypeConstraints.MethodInvocation(ref, context, true)), branch);
    }
}
