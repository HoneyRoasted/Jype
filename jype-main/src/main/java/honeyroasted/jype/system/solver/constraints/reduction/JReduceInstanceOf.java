package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JCastability;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reduces a type comparison expression {@code operand instanceof T} (JLS §15.20.2) into the
 * same castability check used for an explicit cast (JLS §15.20.2 explicitly defers to §5.5 here:
 * the comparison is only legal if a cast of the operand's type to T would also be legal), plus
 * the expression's own {@code boolean} result type satisfying whatever context it is embedded in.
 */
public class JReduceInstanceOf extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.InstanceOf;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InstanceOf instOf = (JExpressionInformation.InstanceOf) constraint.left();
        JType checkedType = instOf.checkedType();
        JTypeSystem system = checkedType.typeSystem();
        JTypeConstraints.Compatible result = new JTypeConstraints.Compatible(system.constants().booleanType(), constraint.middle(), constraint.right());

        List<ConstraintBranch.Snapshot> children = new ArrayList<>();
        if (!(checkedType instanceof JPrimitiveType)) {
            for (JStandaloneType operand : JStandaloneTyping.resolve(instOf.operand(), system)) {
                JType s = operand.type();
                if (s instanceof JPrimitiveType) {
                    // JLS §15.20.2 the operand must have a reference type
                    continue;
                }
                if (!JTypeRelations.isCastable(s, checkedType)) {
                    // JLS §15.20.2 the operand must be castable to the checked type
                    continue;
                }
                if (!isReifiable(checkedType) && !JTypeRelations.isSubtype(s, checkedType) && !checkedCast(s, checkedType)) {
                    // JLS §15.20.2 a non-reifiable type requires a checked cast
                    continue;
                }
                Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
                constraints.put(result, Constraint.Status.UNKNOWN);
                if (operand.extra() != null) {
                    constraints.put(operand.extra(), Constraint.Status.UNKNOWN);
                }
                ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet(), constraints);
                children.add(operand.branch() == null ? snapshot : JStandaloneTyping.combine(operand.branch(), snapshot));
            }
        }
        if (children.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).divergeBranches(children);
        }
    }

    private static boolean checkedCast(JType s, JType t) {
        if (!(t instanceof JClassType tc)) {
            return false;
        }
        JType source = s instanceof JClassType sc && JTypeRelations.hasWildcardArguments(sc) ? JTypeRelations.capture(sc) : s;
        if (!(source instanceof JClassType sct)) {
            return false;
        }
        JClassType sub = JCastability.asSub(sct, tc.classReference());
        return sub != null && JTypeRelations.isSubtype(sub, t) && JTypeRelations.isSubtype(t, sub);
    }

    public static boolean isReifiable(JType type) {
        if (type instanceof JPrimitiveType) {
            return true;
        } else if (type instanceof JArrayType at) {
            return isReifiable(at.component());
        } else if (type instanceof JClassType ct) {
            // JLS §4.7 raw, non-generic, or all-unbounded-wildcard parameterizations are reifiable
            if (ct.hasTypeArguments() && !ct.typeArguments().stream().allMatch(a -> a instanceof JWildType.Upper wu && wu.hasDefaultBounds())) {
                return false;
            }
            return !(ct.hasRelevantOuterType() && ct.outerType() != null) || isReifiable(ct.outerType());
        }
        return false;
    }
}
