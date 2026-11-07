package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.Map;

public final class JIncorporationSupport {
    private JIncorporationSupport() {
    }

    public static boolean isBound(Constraint.Binary<JType, JType> constraint) {
        return constraint.left() instanceof JMetaVarType || constraint.right() instanceof JMetaVarType;
    }

    public static boolean mentions(JType type, JMetaVarType mvt) {
        return !new JRecursiveTypeVisitor<JType, Void>((JTypeVisitor.Default) (t, c) -> t instanceof JMetaVarType m && m.typeEquals(mvt) ? m : null, null, false)
                .visit(type, new HashMap<>()).isEmpty();
    }

    public static JType substitute(JType type, JMetaVarType mvt, JType replacement) {
        return new JMetaVarTypeResolveVisitor(Map.of(mvt, replacement)).visit(type);
    }

    public static void deriveSubtype(ConstraintBranch branch, Constraint a, Constraint b, JType left, JType right) {
        if (left.typeEquals(right)) {
            return;
        }
        if (left.isProperType() && right.isProperType()) {
            // JLS §18.3.1 derived bound; javac checks it as unchecked subtyping (Infer IS_SUBTYPE)
            if (JTypeRelations.isSubtypeUnchecked(left, right)) {
                branch.add(new JTypeConstraints.Subtype(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Subtype(left, right), Constraint.Status.FALSE);
                branch.add(new JTypeConstraints.Contradiction(a, b), Constraint.Status.FALSE);
            }
        } else {
            branch.add(new JTypeConstraints.Subtype(left, right), Constraint.Status.UNKNOWN);
        }
    }

    public static void deriveEqual(ConstraintBranch branch, Constraint a, Constraint b, JType left, JType right) {
        if (left.typeEquals(right)) {
            return;
        }
        if (left.isProperType() && right.isProperType()) {
            branch.add(new JTypeConstraints.Equal(left, right), Constraint.Status.FALSE);
            branch.add(new JTypeConstraints.Contradiction(a, b), Constraint.Status.FALSE);
        } else {
            branch.add(new JTypeConstraints.Equal(left, right), Constraint.Status.UNKNOWN);
        }
    }
}
