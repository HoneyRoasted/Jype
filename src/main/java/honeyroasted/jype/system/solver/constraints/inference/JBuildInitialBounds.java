package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.LinkedHashMap;
import java.util.Map;

public class JBuildInitialBounds implements ConstraintMapper {
    @Override
    public void accept(ConstraintBranch branch) {
        Map<JVarType, JMetaVarType> metaVars = new LinkedHashMap<>();

        branch.constraints().forEach((con, status) -> {
            if (con instanceof JTypeConstraints.Infer inf && status == Constraint.Status.UNKNOWN) {
                metaVars.put(inf.right(), inf.left());
                branch.set(inf, Constraint.Status.ASSUMED);
            }
        });

        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(metaVars);
        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                //Case where P_l has no upper classBound, alpha_l <: Object
                branch.add(JTypeConstraints.Subtype.createBound(mvt, vt.typeSystem().constants().object()), Constraint.Status.ASSUMED);
            } else {
                //Case where P_l has upper interfaceBounds
                boolean foundProperUpper = false;
                for (JType bound : vt.upperBounds()) {
                    JType resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    //Upper interfaceBounds imply alpha_l <: T[P_1 = alpha_1... P_n = alpha_n], for each classBound T
                    branch.add(JTypeConstraints.Subtype.createBound(mvt, resolved), Constraint.Status.ASSUMED);
                }

                if (!foundProperUpper) {
                    //If there is no proper upper classBound, alpha_l <: Object
                    branch.add(JTypeConstraints.Subtype.createBound(mvt, vt.typeSystem().constants().object()), Constraint.Status.ASSUMED);
                }
            }
        });
    }
}
