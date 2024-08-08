package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BuildInitialBounds implements TypeOperation<Map<VarType, MetaVarType>, Set<TypeBound.Result.Builder>> {

    @Override
    public Set<TypeBound.Result.Builder> apply(TypeSystem system, Map<VarType, MetaVarType> metaVars) {
        Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
        VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);

        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                //Case where P_l has no upper bound, alpha_l <: Object
                bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
            } else {
                //Case where P_l has upper bounds
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    //Upper bounds imply alpha_l <: T[P_1 = alpha_1... P_n = alpha_n], for each bound T
                    bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, resolved)));
                }

                if (!foundProperUpper) {
                    //If there is no proper upper bound, alpha_l <: Object
                    bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
                }
            }
        });

        return bounds;
    }

}
