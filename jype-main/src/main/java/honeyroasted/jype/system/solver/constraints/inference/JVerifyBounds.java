package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JArrayType;

import java.util.LinkedHashMap;
import java.util.Map;

public class JVerifyBounds implements ConstraintMapper {
    @Override
    public void accept(ConstraintBranch branch) {
        if (!branch.status().isTrue()) {
            return;
        }
        Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();
        branch.constraints().forEach((con, status) -> {
            if (con instanceof JTypeConstraints.Instantiation inst && status.isTrue()) {
                instantiations.put(inst.left(), inst.right());
            }
        });
        JMetaVarTypeResolveVisitor theta = new JMetaVarTypeResolveVisitor(instantiations);

        branch.constraints().forEach((con, status) -> {
            if (status != Constraint.Status.ASSUMED) {
                return;
            }
            if (con instanceof JTypeConstraints.Denotable d) {
                // JLS §15.9.3 inferred type arguments of an anonymous class must be denotable
                branch.set(con, Constraint.Status.known(isDenotable(theta.visit(d.value()))));
            } else if (con instanceof JTypeConstraints.Deferred) {
                // JLS §18.5.2.2 every deferred constraint must eventually be reduced
                branch.set(con, Constraint.Status.FALSE);
            } else if (con instanceof JTypeConstraints.Subtype st) {
                JType l = theta.visit(st.left());
                JType r = theta.visit(st.right());
                branch.set(con, l.isProperType() && r.isProperType() ? Constraint.Status.known(JTypeRelations.isSubtypeUnchecked(l, r)) : Constraint.Status.TRUE);
            } else if (con instanceof JTypeConstraints.Equal eq) {
                JType l = theta.visit(eq.left());
                JType r = theta.visit(eq.right());
                branch.set(con, l.isProperType() && r.isProperType() ? Constraint.Status.known(JTypeRelations.isSameType(l, r)) : Constraint.Status.TRUE);
            } else {
                branch.set(con, Constraint.Status.TRUE);
            }
        });
    }

    private static boolean isDenotable(JType type) {
        if (type instanceof JIntersectionType || type instanceof JMetaVarType || type.isNullType()) {
            return false;
        }
        if (type instanceof JVarType vt) {
            return vt.location() == null || !vt.location().isVirtual();
        }
        if (type instanceof JArrayType at) {
            return isDenotable(at.component());
        }
        if (type instanceof JWildType.Upper wu) {
            return wu.upperBounds().stream().allMatch(JVerifyBounds::isDenotable);
        }
        if (type instanceof JWildType.Lower wl) {
            return wl.lowerBounds().stream().allMatch(JVerifyBounds::isDenotable);
        }
        if (type instanceof JClassType ct) {
            return ct.typeArguments().stream().allMatch(JVerifyBounds::isDenotable) && (!ct.hasRelevantOuterType() || ct.outerType() == null || isDenotable(ct.outerType()));
        }
        return true;
    }
}
