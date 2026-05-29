package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JReduceInstantiationRefExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.InstantiationReference
                && constraint.right() instanceof JClassType;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InstantiationReference instRef =
                (JExpressionInformation.InstantiationReference) constraint.left();
        JClassType targetType = (JClassType) constraint.right();

        // T must be a functional interface
        if (!targetType.classReference().hasModifier(AccessFlag.INTERFACE)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        Optional<JMethodReference> samOpt = JReduceLambdaExpression.findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();
        if (targetType instanceof JParameterizedClassType pct) {
            sam = (JMethodReference) pct.varTypeResolver().visit(sam);
        }

        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(targetType.typeSystem().constants().voidType());

        // The type being constructed must be a class reference
        if (!(instRef.type() instanceof JClassReference classRef)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();

        // Find all constructors of the target class
        classRef.declaredMethods().stream()
                .filter(m -> m.location().isConstructor() && !m.hasModifier(AccessFlag.PRIVATE))
                .forEach(ctor -> {
                    if (ctor.parameters().size() != samParams.size()) return;

                    // Set up type inference for the class's type parameters
                    List<JArgumentType> explicitTypeArgs = instRef.explicitTypeArguments();
                    Set<JTypeConstraints.Infer> typeParamConstraints = new LinkedHashSet<>();
                    Map<JVarType, JType> resolution = new LinkedHashMap<>();

                    if (explicitTypeArgs == null || explicitTypeArgs.isEmpty()
                            || explicitTypeArgs.size() != classRef.typeParameters().size()) {
                        for (JVarType vt : classRef.typeParameters()) {
                            JMetaVarType mvt = vt.createMetaVar();
                            typeParamConstraints.add(new JTypeConstraints.Infer(mvt, vt));
                            resolution.put(vt, mvt);
                        }
                    } else {
                        for (int i = 0; i < classRef.typeParameters().size(); i++) {
                            resolution.put(classRef.typeParameters().get(i), explicitTypeArgs.get(i));
                        }
                    }

                    JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(resolution);

                    // The constructed type (with inferred or explicit type args)
                    JClassType constructedType = resolution.isEmpty()
                            ? classRef
                            : (explicitTypeArgs != null && !explicitTypeArgs.isEmpty()
                            ? classRef.parameterized(explicitTypeArgs)
                            : classRef.parameterizedWithMetaVars());

                    Map<Constraint, Constraint.Status> constraints = new HashMap<>();
                    typeParamConstraints.forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

                    // SAM params → constructor params
                    for (int i = 0; i < samParams.size(); i++) {
                        constraints.put(
                                new JTypeConstraints.Compatible(
                                        samParams.get(i), constraint.middle(),
                                        resolver.apply(ctor.parameters().get(i))),
                                Constraint.Status.UNKNOWN);
                    }

                    // Constructed type must be compatible with the SAM's return type
                    if (!returnsVoid) {
                        constraints.put(
                                new JTypeConstraints.Compatible(
                                        resolver.apply(constructedType), constraint.middle(), samReturn),
                                Constraint.Status.UNKNOWN);
                    }

                    // If no sub-constraints, mark the original constraint TRUE directly
                    if (constraints.isEmpty()) {
                        constraints.put(constraint, Constraint.Status.TRUE);
                    }

                    newChildren.add(new ConstraintBranch.Snapshot(new PropertySet(), constraints));
                });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }
}
