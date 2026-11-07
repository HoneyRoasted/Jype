package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

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
            if (con instanceof JTypeConstraints.Deferred) {
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
}
