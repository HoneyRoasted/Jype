package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JReferenceSupport;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveMostSpecificMethod;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JReduceInstantiationRefExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.InstantiationReference;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InstantiationReference ref = (JExpressionInformation.InstantiationReference) constraint.left();
        JType target = constraint.right();
        JTypeSystem system = target.typeSystem();

        if (target instanceof JMetaVarType mvt) {
            // JLS §18.5.2.2 the target is an input variable
            JReferenceSupport.defer(branch, constraint, Set.of(mvt));
            return;
        }
        Optional<JReferenceSupport.FunctionType> fn = JReferenceSupport.functionType(target);
        if (fn.isEmpty()) {
            // JLS §15.13.2 the target must be a functional interface type
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }
        List<JType> params = fn.get().parameters();
        if (!JResolveMostSpecificMethod.isExactMethodReference(system, ref)) {
            Set<JMetaVarType> missing = new LinkedHashSet<>();
            Optional<List<JType>> resolved = JReferenceSupport.resolveInputs(branch, params, missing);
            if (resolved.isEmpty()) {
                // JLS §18.5.2.2 an inexact constructor reference waits for its parameter types
                JReferenceSupport.defer(branch, constraint, missing);
                return;
            }
            params = resolved.get();
        }
        JType ret = fn.get().returnType();
        JType voidType = system.constants().voidType();

        if (ref.type() instanceof JArrayType array) {
            if (params.size() != 1) {
                // JLS §15.13.1 an array constructor reference takes exactly one int argument
                branch.set(constraint, Constraint.Status.FALSE);
                return;
            }
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.Compatible(params.getFirst(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, system.constants().intType()));
            if (!ret.typeEquals(voidType)) {
                branch.add(new JTypeConstraints.Compatible(array, JTypeConstraints.Compatible.Context.ASSIGNMENT, ret));
            }
            return;
        }
        if (!(ref.type() instanceof JClassType classType)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JClassReference cls = classType.classReference();
        List<JArgumentType> classArgs = classType.hasTypeArguments() ? classType.typeArguments() : List.of();
        List<JArgumentType> ctorArgs = ref.explicitTypeArguments() == null ? List.of() : ref.explicitTypeArguments();
        List<JExpressionInformation> args = params.stream().map(p -> (JExpressionInformation) JExpressionInformation.of(p)).toList();
        // JLS §15.13.1 ClassType::new behaves as a class instance creation with arguments of types P1..Pn
        JExpressionInformation.Instantiation synthetic = new JExpressionInformation.Instantiation.Full(ref.declaring(), null, cls, args, classArgs, false, ctorArgs, null);
        branch.drop(constraint);
        branch.add(new JTypeConstraints.ExpressionCompatible(synthetic, JTypeConstraints.Compatible.Context.ASSIGNMENT, ret));
        branch.add(new JTypeConstraints.LambdaThrowsCheck(synthetic, fn.get().sam().exceptionTypes()));
    }
}
