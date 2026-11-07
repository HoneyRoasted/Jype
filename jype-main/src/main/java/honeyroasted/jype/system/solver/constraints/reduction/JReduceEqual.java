package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JRelationTrees;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

public class JReduceEqual extends ConstraintMapper.Unary<JTypeConstraints.Equal> {
    private final JSolverConfig config;

    public JReduceEqual() {
        this(JSolverConfig.FAST);
    }

    public JReduceEqual(JSolverConfig config) {
        this.config = config;
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        JType s = constraint.left();
        JType t = constraint.right();
        JType object = s.typeSystem().constants().object();

        if (s.isProperType() && t.isProperType()) {
            JRelationTrees.ask(branch, constraint, this.config);
        } else if (s.isNullType() || t.isNullType()) {
            // JLS §18.2.4 the null type is not equal to an improper type
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (s instanceof JMetaVarType || t instanceof JMetaVarType) {
            if (s instanceof JPrimitiveType || t instanceof JPrimitiveType) {
                // JLS §18.2.4 S or T is a primitive type
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                // JLS §18.2.4 the bound S = T
                branch.set(constraint, Constraint.Status.ASSUMED);
            }
        } else if (s instanceof JWildType || t instanceof JWildType) {
            reduceWildcards(branch, constraint, s, t, object);
        } else if (s instanceof JClassType sc && t instanceof JClassType tc) {
            if (!sc.classReference().typeEquals(tc.classReference()) || sc.typeArguments().size() != tc.typeArguments().size()) {
                branch.set(constraint, Constraint.Status.FALSE);
                return;
            }
            // JLS §18.2.4 ‹Bi = Ai› for each type argument
            branch.set(constraint, Constraint.Status.TRUE);
            for (int i = 0; i < sc.typeArguments().size(); i++) {
                branch.add(new JTypeConstraints.Equal(sc.typeArguments().get(i), tc.typeArguments().get(i)));
            }
            if (sc.hasRelevantOuterType() && tc.hasRelevantOuterType() && sc.outerType() != null && tc.outerType() != null) {
                branch.add(new JTypeConstraints.Equal(sc.outerType(), tc.outerType()));
            }
        } else if (s instanceof JArrayType sa && t instanceof JArrayType ta) {
            // JLS §18.2.4 ‹S' = T'›
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.Equal(sa.component(), ta.component()));
        } else if (s instanceof JIntersectionType || t instanceof JIntersectionType) {
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.Subtype(s, t));
            branch.add(new JTypeConstraints.Subtype(t, s));
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static void reduceWildcards(ConstraintBranch branch, Constraint constraint, JType s, JType t, JType object) {
        if (!(s instanceof JWildType) || !(t instanceof JWildType)) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (s instanceof JWildType.Upper su && t instanceof JWildType.Upper tu) {
            if (su.hasDefaultBounds() && tu.hasDefaultBounds()) {
                // JLS §18.2.4 ? = ?
                branch.set(constraint, Constraint.Status.TRUE);
            } else {
                // JLS §18.2.4 ? extends S' = ? extends T' (? is ? extends Object)
                branch.set(constraint, Constraint.Status.TRUE);
                branch.add(new JTypeConstraints.Equal(su.hasDefaultBounds() ? object : su.upperBound(), tu.hasDefaultBounds() ? object : tu.upperBound()));
            }
        } else if (s instanceof JWildType.Lower sl && t instanceof JWildType.Lower tl) {
            // JLS §18.2.4 ? super S' = ? super T'
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.Equal(sl.lowerBound(), tl.lowerBound()));
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }
}
