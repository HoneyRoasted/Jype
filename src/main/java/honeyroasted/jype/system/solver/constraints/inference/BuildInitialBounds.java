package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.LinkedHashMap;
import java.util.Map;

public class BuildInitialBounds implements ConstraintMapper {
    @Override
    public void accept(ConstraintBranch branch) {
        Map<VarType, MetaVarType> metaVars = new LinkedHashMap<>();

        branch.constraints().forEach((con, status) -> {
            if (con instanceof TypeConstraints.Infer inf && status == Constraint.Status.UNKNOWN) {
                metaVars.put(inf.right(), inf.left());
                branch.set(inf, Constraint.Status.ASSUMED);
            }
        });

        VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);
        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                //Case where P_l has no upper bound, alpha_l <: Object
                branch.add(new TypeConstraints.Subtype(mvt, vt.typeSystem().constants().object()), Constraint.Status.ASSUMED);
            } else {
                //Case where P_l has upper bounds
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    //Upper bounds imply alpha_l <: T[P_1 = alpha_1... P_n = alpha_n], for each bound T
                    branch.add(new TypeConstraints.Subtype(mvt, resolved), Constraint.Status.ASSUMED);
                }

                if (!foundProperUpper) {
                    //If there is no proper upper bound, alpha_l <: Object
                    branch.add(new TypeConstraints.Subtype(mvt, vt.typeSystem().constants().object()), Constraint.Status.ASSUMED);
                }
            }
        });
    }
}
