package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JInvocationCandidate;
import honeyroasted.jype.system.solver.JInvocationSelection;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JReduceInstantiation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Instantiation;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Instantiation inst = (JExpressionInformation.Instantiation) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JClassReference type = inst.type();

        if (type.hasModifier(AccessFlag.ABSTRACT) || type.hasModifier(AccessFlag.INTERFACE)) {
            // JLS §15.9.1 a class instance creation may not instantiate an abstract class or interface
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }
        if (!inst.raw() && !inst.explicitTypeArguments().isEmpty() && inst.explicitTypeArguments().size() != type.typeParameters().size()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        if (type.hasRelevantOuterType() && inst.outerInstance() != null) {
            for (JStandaloneType outer : JStandaloneTyping.resolve(inst.outerInstance(), system)) {
                JClassType outerType = JTypeRelations.asSuper(outer.type(), type.outerClass());
                if (outerType == null) {
                    continue;
                }
                Map<Constraint, Constraint.Status> extra = new LinkedHashMap<>();
                if (outer.extra() != null) {
                    extra.put(outer.extra(), Constraint.Status.UNKNOWN);
                }
                select(system, inst, outerType, constraint, extra).ifPresent(s -> newChildren.add(outer.branch() == null ? s : JStandaloneTyping.combine(outer.branch(), s)));
            }
        } else if (type.hasRelevantOuterType()) {
            // JLS §15.9.2 the immediately enclosing instance is implicit
            Optional<JClassType> outerType = inst.declaring() == null ? Optional.empty() : system.operations().outerTypeFromDeclaring(type, inst.declaring());
            outerType.flatMap(o -> select(system, inst, o, constraint, Map.of())).ifPresent(newChildren::add);
        } else {
            select(system, inst, null, constraint, Map.of()).ifPresent(newChildren::add);
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static Optional<ConstraintBranch.Snapshot> select(JTypeSystem system, JExpressionInformation.Instantiation inst, JClassType outer, JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        JClassReference type = inst.type();
        boolean generic = !type.typeParameters().isEmpty();
        boolean diamond = generic && !inst.raw() && inst.explicitTypeArguments().isEmpty();

        JClassType classType;
        List<JVarType> classParams = new ArrayList<>();
        Map<JVarType, JType> classTheta = new LinkedHashMap<>();
        if (inst.raw() || !generic) {
            classType = type;
        } else if (diamond) {
            // JLS §15.9.3 diamond: class type parameters are inferred alongside the constructor's
            classParams.addAll(type.typeParameters());
            classType = type.parameterized(type.typeParameters().stream().map(p -> (JArgumentType) p).toList());
        } else {
            classType = type.parameterized(inst.explicitTypeArguments());
            for (int i = 0; i < type.typeParameters().size(); i++) {
                classTheta.put(type.typeParameters().get(i), inst.explicitTypeArguments().get(i));
            }
        }
        if (outer != null && outer.hasAnyTypeArguments()) {
            JParameterizedClassType pct = classType instanceof JParameterizedClassType p ? p : type.parameterized(List.of());
            pct.setUnmodifiable(false);
            pct.setOuterType(outer);
            pct.setUnmodifiable(true);
            classType = pct;
        }
        Map<JVarType, JType> outerTheta = outer == null ? Map.of() : JTypeRelations.substitution(outer);

        List<JInvocationCandidate> candidates = new ArrayList<>();
        for (JMethodReference ctor : type.declaredMethods()) {
            if (!ctor.location().isConstructor() || ctor.hasModifier(AccessFlag.SYNTHETIC)) {
                continue;
            }
            if (inst.declaring() != null && !ctor.outerClass().accessFrom(inst.declaring()).canAccess(ctor.access())) {
                // JLS §15.9.3 the constructor must be accessible
                continue;
            }
            JMethodReference c = ctor;
            if (inst.raw()) {
                c = (JMethodReference) JTypeVisitors.ERASURE.visit(ctor);
            } else if (!classTheta.isEmpty() || !outerTheta.isEmpty()) {
                Map<JVarType, JType> theta = new LinkedHashMap<>(outerTheta);
                theta.putAll(classTheta);
                c = (JMethodReference) new JVarTypeResolveVisitor(theta).visit(ctor);
            }
            List<JType> formals = new ArrayList<>(c.parameters());
            if (outer != null && formals.size() == inst.parameters().size() + 1
                    && JTypeRelations.erasure(formals.getFirst()) instanceof JClassType first && first.classReference().typeEquals(type.outerClass())) {
                formals.removeFirst();
            }
            List<JVarType> typeParams = new ArrayList<>(classParams);
            typeParams.addAll(c.typeParameters());
            candidates.add(new JInvocationCandidate(c, formals, typeParams, c.typeParameters(), classType, c.exceptionTypes(), c.hasModifier(AccessFlag.VARARGS)));
        }
        return JInvocationSelection.select(system, inst, candidates, inst.parameters(), inst.constructorTypeArguments(), constraint, extra);
    }
}
