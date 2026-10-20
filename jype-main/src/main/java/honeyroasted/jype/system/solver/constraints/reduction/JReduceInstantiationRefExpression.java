package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
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
import java.util.Optional;
import java.util.Set;

public class JReduceInstantiationRefExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    private record TypeParamSetup(Set<JTypeConstraints.Infer> constraints, JVarTypeResolveVisitor resolver, JClassType constructedType) {
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.InstantiationReference
                && (constraint.right() instanceof JClassType || constraint.right() instanceof JIntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InstantiationReference instRef =
                (JExpressionInformation.InstantiationReference) constraint.left();
        JType targetType = constraint.right();

        Optional<JMethodReference> samOpt = JReduceLambdaExpression.findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();
        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(targetType.typeSystem().constants().voidType());

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();

        if (instRef.type() instanceof JArrayType arrayType) {
            // Array creation method reference (JLS §15.13.1): T[]::new is a synthetic
            // single-parameter (int length) constructor.
            if (samParams.size() == 1) {
                newChildren.add(createArrayConstructorRefBranch(arrayType, samParams, samReturn, returnsVoid, constraint));
            }
        } else if (instRef.type() instanceof JClassReference classRef) {
            // Find all constructors of the target class
            classRef.declaredMethods().stream()
                    .filter(m -> m.location().isConstructor() && !m.hasModifier(AccessFlag.PRIVATE))
                    .forEach(ctor -> {
                        int declaredParamCount = ctor.parameters().size();

                        boolean fixed = declaredParamCount == samParams.size();
                        boolean vararg = ctor.hasModifier(AccessFlag.VARARGS) &&
                                samParams.size() >= declaredParamCount - 1 &&
                                ctor.parameters().get(declaredParamCount - 1) instanceof JArrayType;

                        if (fixed) {
                            TypeParamSetup setup = setupTypeParams(classRef, instRef.explicitTypeArguments());
                            addIfPresent(newChildren, createConstructorRefBranch(ctor, setup, samParams, ctor.parameters(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                        }

                        if (vararg) {
                            TypeParamSetup setup = setupTypeParams(classRef, instRef.explicitTypeArguments());
                            addIfPresent(newChildren, createVarargConstructorRefBranch(ctor, setup, samParams, ctor.parameters(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                        }
                    });
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static ConstraintBranch.Snapshot createArrayConstructorRefBranch(
            JArrayType arrayType, List<JType> samParams, JType samReturn, boolean returnsVoid,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();

        constraints.put(
                new JTypeConstraints.Compatible(samParams.get(0), constraint.middle(), arrayType.typeSystem().constants().intType()),
                Constraint.Status.UNKNOWN);

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(arrayType, constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    private static TypeParamSetup setupTypeParams(JClassReference classRef, List<JArgumentType> explicitTypeArgs) {
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

        JClassType constructedType = resolution.isEmpty()
                ? classRef
                : (explicitTypeArgs != null && !explicitTypeArgs.isEmpty()
                ? classRef.parameterized(explicitTypeArgs)
                : classRef.parameterizedWithMetaVars());

        return new TypeParamSetup(typeParamConstraints, resolver, constructedType);
    }

    private static void addIfPresent(List<ConstraintBranch.Snapshot> children, ConstraintBranch.Snapshot snapshot) {
        if (snapshot != null) children.add(snapshot);
    }

    private static ConstraintBranch.Snapshot createConstructorRefBranch(
            JMethodReference ctor, TypeParamSetup setup, List<JType> samParams, List<JType> ctorParams,
            JType samReturn, boolean returnsVoid, List<JType> samThrows, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(ctor, false)), Constraint.Status.TRUE);
        setup.constraints().forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        for (int i = 0; i < samParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), setup.resolver().apply(ctorParams.get(i))),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(setup.resolver().apply(setup.constructedType()), constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        if (!JMemberLookup.addThrowsConstraints(constraints, ctor, samThrows)) return null;

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    private static ConstraintBranch.Snapshot createVarargConstructorRefBranch(
            JMethodReference ctor, TypeParamSetup setup, List<JType> samParams, List<JType> ctorParams,
            JType samReturn, boolean returnsVoid, List<JType> samThrows, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(ctor, true)), Constraint.Status.TRUE);
        setup.constraints().forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        int fixed = ctorParams.size() - 1;
        JArrayType vararg = (JArrayType) setup.resolver().apply(ctorParams.get(fixed));
        JType component = vararg.component();

        for (int i = 0; i < fixed; i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), setup.resolver().apply(ctorParams.get(i))),
                    Constraint.Status.UNKNOWN);
        }

        for (int i = fixed; i < samParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), component),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(setup.resolver().apply(setup.constructedType()), constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        if (!JMemberLookup.addThrowsConstraints(constraints, ctor, samThrows)) return null;

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }
}
