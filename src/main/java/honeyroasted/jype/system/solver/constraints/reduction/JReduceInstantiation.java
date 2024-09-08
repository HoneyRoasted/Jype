package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import org.glavo.classfile.AccessFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class JReduceInstantiation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Instantiation;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);

        JType left = mapper.apply(constraint.right());
        JExpressionInformation.Instantiation inst = (JExpressionInformation.Instantiation) constraint.left();
        JTypeSystem system = left.typeSystem();

        JClassReference target = inst.type();
        JClassReference declaring = inst.declaring();

        List<JExpressionInformation> parameters = new ArrayList<>();
        if (target.hasRelevantOuterType()) {
            Optional<JClassType> outerType = system.operations().outerTypeFromDeclaring(target, declaring);
            if (outerType.isPresent()) {
                parameters.add(JExpressionInformation.of(outerType.get()));
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
                return;
            }
        }
        parameters.addAll(inst.parameters());

        Set<Constraint> typeParams = new LinkedHashSet<>();
        if (inst.explicitTypeArguments().isEmpty() || inst.explicitTypeArguments().size() != target.typeParameters().size()) {
            for (int i = 0; i < target.typeParameters().size(); i++) {
                JVarType vt = target.typeParameters().get(i);
                typeParams.add(new JTypeConstraints.Infer(vt.createMetaVar(), vt));
            }
        }

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        JClassType result = typeParams.isEmpty() ? target.parameterized(inst.explicitTypeArguments()) :
                target.parameterizedWithMetaVars();

        inst.type().declaredMethods().stream().filter(m -> m.location().isConstructor()).forEach(ref -> {
            if (!ref.hasModifier(AccessFlag.STATIC) && ref.outerClass().accessFrom(declaring).canAccess(ref.access())) {
                if (ref.parameters().size() == parameters.size()) {
                    newChildren.add(createConstruct(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, inst, constraint, ref, result, parameters));
                    newChildren.add(createConstruct(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, inst, constraint, ref, result, parameters));
                }

                if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                        ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                    newChildren.add(createVarargConstruct(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, typeParams, inst, constraint, ref, result, parameters, vararg));
                    newChildren.add(createVarargConstruct(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, typeParams, inst, constraint, ref, result, parameters, vararg));
                }
            }
        });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static ConstraintBranch.Snapshot createConstruct(JTypeConstraints.Compatible.Context context, Set<Constraint> typeParams, JExpressionInformation.Instantiation inst, JTypeConstraints.ExpressionCompatible constraint, JMethodReference ref, JType result, List<JExpressionInformation> parameters) {
        Map<Constraint, Constraint.Status> branch = new HashMap<>();
        typeParams.forEach(c -> branch.put(c, Constraint.Status.UNKNOWN));
        branch.put(new JTypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        for (int i = 0; i < parameters.size(); i++) {
            branch.put(new JTypeConstraints.ExpressionCompatible(parameters.get(i), context, ref.parameters().get(i)), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(inst, ref, context, false)), branch);
    }

    private static ConstraintBranch.Snapshot createVarargConstruct(JTypeConstraints.Compatible.Context context, Set<Constraint> typeParams, JExpressionInformation.Instantiation inst, JTypeConstraints.ExpressionCompatible constraint, JMethodReference ref, JType result, List<JExpressionInformation> parameters, JArrayType vararg) {
        Map<Constraint, Constraint.Status> branch = new HashMap<>();
        typeParams.forEach(c -> branch.put(c, Constraint.Status.UNKNOWN));
        branch.put(new JTypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            branch.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, ref.parameters().get(index)), Constraint.Status.UNKNOWN);
        }

        for (; index < parameters.size(); index++) {
            branch.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, vararg.component()), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(inst, ref, context, true)), branch);
    }
}
