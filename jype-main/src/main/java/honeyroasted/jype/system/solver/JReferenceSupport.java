package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JReferenceSupport {
    private JReferenceSupport() {
    }

    public record FunctionType(JMethodReference sam, List<JType> parameters, JType returnType) {
    }

    public static Optional<FunctionType> functionType(JType target) {
        if (!(target instanceof JClassType) && !(target instanceof JIntersectionType)) {
            return Optional.empty();
        }
        // JLS §15.13.2 the ground target type is the non-wildcard parameterization
        Optional<JType> ground = JFunctionalInterfaces.nonWildcardParameterization(target);
        if (ground.isEmpty()) {
            return Optional.empty();
        }
        return JFunctionalInterfaces.functionType(ground.get()).map(sam -> new FunctionType(sam, sam.parameters(), sam.returnType()));
    }

    public static Optional<List<JType>> resolveInputs(ConstraintBranch branch, List<JType> types, Set<JMetaVarType> missing) {
        Set<JMetaVarType> inputs = new LinkedHashSet<>();
        types.forEach(t -> inputs.addAll(metaVars(t)));
        if (inputs.isEmpty()) {
            return Optional.of(types);
        }
        Map<JMetaVarType, JType> inst = new LinkedHashMap<>();
        for (JMetaVarType in : inputs) {
            JType i = JResolveBounds.findInstantiation(in, branch);
            if (i == null) {
                missing.addAll(inputs);
                return Optional.empty();
            }
            inst.put(in, i);
        }
        JMetaVarTypeResolveVisitor theta = new JMetaVarTypeResolveVisitor(inst);
        return Optional.of(types.stream().map(theta::visit).toList());
    }

    public static void defer(ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Set<JMetaVarType> inputs) {
        branch.drop(constraint);
        branch.add(new JTypeConstraints.Deferred(constraint, inputs), Constraint.Status.ASSUMED);
    }

    public static Set<JMetaVarType> metaVars(JType type) {
        return new LinkedHashSet<>(new JRecursiveTypeVisitor<JMetaVarType, Void>((JTypeVisitor.Default) (t, c) -> t instanceof JMetaVarType m ? m : null, null, false)
                .visit(type, new HashMap<>()));
    }
}
