package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

public class JReduceCompatible extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {
    private final JSolverConfig config;

    public JReduceCompatible() {
        this(JSolverConfig.FAST);
    }

    public JReduceCompatible(JSolverConfig config) {
        this.config = config;
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        JType s = constraint.left();
        JType t = constraint.right();
        JTypeConstraints.Compatible.Context context = constraint.middle();

        if (s.isProperType() && t.isProperType()) {
            // JLS §18.2.2 S and T are proper types
            boolean compatible = JTypeRelations.ask(branch, constraint, this.config);
            if (compatible && context != JTypeConstraints.Compatible.Context.EXPLICIT_CAST && !(s instanceof JPrimitiveType) && !(t instanceof JPrimitiveType)
                    && !JTypeRelations.isSubtype(s, t)) {
                branch.add(new JTypeConstraints.UncheckedConversion(s, t), Constraint.Status.TRUE);
            }
            return;
        }

        if (context == JTypeConstraints.Compatible.Context.STRICT_INVOCATION && (s instanceof JPrimitiveType) != (t instanceof JPrimitiveType)) {
            // JLS §18.5.1 strict invocation never boxes or unboxes
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (s instanceof JPrimitiveType ps) {
            // JLS §18.2.2 S is primitive: ‹box(S) → T›
            reduceTo(branch, constraint, new JTypeConstraints.Compatible(ps.box(), context, t));
        } else if (t instanceof JPrimitiveType pt) {
            // JLS §18.2.2 T is primitive: ‹S = box(T)›
            reduceTo(branch, constraint, new JTypeConstraints.Equal(s, pt.box()));
        } else if (isUncheckedClass(s, t)) {
            // JLS §18.2.2 unchecked conversion to a parameterized type
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.UncheckedConversion(s, t), Constraint.Status.TRUE);
        } else if (isUncheckedArray(s, t)) {
            // JLS §18.2.2 unchecked conversion to an array of a parameterized type
            branch.set(constraint, Constraint.Status.TRUE);
            branch.add(new JTypeConstraints.UncheckedConversion(s, t), Constraint.Status.TRUE);
        } else {
            // JLS §18.2.2 ‹S <: T›
            reduceTo(branch, constraint, new JTypeConstraints.Subtype(s, t));
        }
    }

    private static boolean isUncheckedClass(JType s, JType t) {
        if (!(t instanceof JClassType tc) || !tc.hasAnyTypeArguments() || JTypeRelations.isRaw(tc) || s instanceof JMetaVarType) {
            return false;
        }
        JClassType sup = JTypeRelations.asSuper(s, tc.classReference());
        return sup != null && JTypeRelations.isRaw(sup);
    }

    private static boolean isUncheckedArray(JType s, JType t) {
        if (!(s instanceof JArrayType) || !(t instanceof JArrayType)) {
            return false;
        }
        while (s instanceof JArrayType sa && t instanceof JArrayType ta) {
            s = sa.component();
            t = ta.component();
        }
        return isUncheckedClass(s, t);
    }

    private static void reduceTo(ConstraintBranch branch, Constraint constraint, Constraint result) {
        branch.set(constraint, Constraint.Status.TRUE);
        branch.add(result);
    }
}
