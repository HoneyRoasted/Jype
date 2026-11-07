package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.HashSet;
import java.util.Set;

public class JReduceSubtype extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType s = constraint.left();
        JType t = constraint.right();

        if (s.isProperType() && t.isProperType()) {
            branch.set(constraint, Constraint.Status.known(JTypeRelations.isSubtype(s, t)));
        } else if (s.isNullType()) {
            // JLS §18.2.3 S is the null type
            branch.set(constraint, Constraint.Status.TRUE);
        } else if (t.isNullType()) {
            // JLS §18.2.3 T is the null type
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (s instanceof JMetaVarType || t instanceof JMetaVarType) {
            // JLS §18.2.3 S or T is an inference variable: the bound S <: T
            branch.set(constraint, Constraint.Status.ASSUMED);
        } else if (s instanceof JPrimitiveType || t instanceof JPrimitiveType) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (t instanceof JIntersectionType it) {
            // JLS §18.2.3 T is an intersection type
            branch.set(constraint, Constraint.Status.TRUE);
            it.children().forEach(c -> branch.add(new JTypeConstraints.Subtype(s, c)));
        } else if (t instanceof JVarType tv) {
            if (s instanceof JIntersectionType si && si.children().stream().anyMatch(c -> c.typeEquals(tv))) {
                // JLS §18.2.3 S is an intersection with T as an element
                branch.set(constraint, Constraint.Status.TRUE);
            } else if (tv.hasLowerBound()) {
                // JLS §18.2.3 T has a lower bound B
                branch.set(constraint, Constraint.Status.TRUE);
                branch.add(new JTypeConstraints.Subtype(s, tv.lowerBound()));
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        } else if (t instanceof JArrayType ta) {
            JArrayType sa = mostSpecificArraySupertype(s, new HashSet<>());
            if (sa == null) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else if (sa.component() instanceof JPrimitiveType || ta.component() instanceof JPrimitiveType) {
                // JLS §18.2.3 primitive array components must be the same
                branch.set(constraint, Constraint.Status.known(sa.component().typeEquals(ta.component())));
            } else {
                // JLS §18.2.3 ‹S' <: T'›
                branch.set(constraint, Constraint.Status.TRUE);
                branch.add(new JTypeConstraints.Subtype(sa.component(), ta.component()));
            }
        } else if (t instanceof JClassType tc) {
            JType source = s instanceof JClassType sc && JTypeRelations.hasWildcardArguments(sc) ? JTypeRelations.capture(sc) : s;
            JClassType sup = JTypeRelations.asSuper(source, tc.classReference());
            if (sup == null) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else if (!tc.hasAnyTypeArguments() || JTypeRelations.isRaw(tc)) {
                // JLS §18.2.3 T is a non-parameterized class or interface type
                branch.set(constraint, Constraint.Status.TRUE);
            } else if (JTypeRelations.isRaw(sup)) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                // JLS §18.2.3 T is a parameterized type: ‹Bi <= Ai›
                branch.set(constraint, Constraint.Status.TRUE);
                addContainment(branch, sup, tc);
            }
        } else if (t instanceof JWildType tw) {
            if (tw instanceof JWildType.Lower tl) {
                branch.set(constraint, Constraint.Status.TRUE);
                branch.add(new JTypeConstraints.Subtype(s, tl.lowerBound()));
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static void addContainment(ConstraintBranch branch, JClassType sup, JClassType t) {
        if (t.hasTypeArguments()) {
            if (sup.typeArguments().size() != t.typeArguments().size()) {
                branch.add(Constraint.FALSE, Constraint.Status.FALSE);
                return;
            }
            for (int i = 0; i < t.typeArguments().size(); i++) {
                branch.add(new JTypeConstraints.Contains(sup.typeArguments().get(i), t.typeArguments().get(i)));
            }
        }
        if (t.hasRelevantOuterType() && t.outerType() != null && t.outerType().hasAnyTypeArguments()) {
            if (sup.outerType() == null) {
                branch.add(Constraint.FALSE, Constraint.Status.FALSE);
            } else {
                branch.add(new JTypeConstraints.Subtype(sup.outerType(), t.outerType()));
            }
        }
    }

    private static JArrayType mostSpecificArraySupertype(JType s, Set<JType> seen) {
        if (s instanceof JArrayType sa) {
            return sa;
        }
        if (!seen.add(s)) {
            return null;
        }
        if (s instanceof JVarType sv) {
            for (JType b : JTypeRelations.upperBounds(sv)) {
                JArrayType r = mostSpecificArraySupertype(b, seen);
                if (r != null) {
                    return r;
                }
            }
        } else if (s instanceof JIntersectionType si) {
            for (JType c : si.children()) {
                JArrayType r = mostSpecificArraySupertype(c, seen);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }
}
