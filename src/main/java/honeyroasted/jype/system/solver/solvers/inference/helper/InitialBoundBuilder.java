package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//Implements creating an initial bounds set as defined in 18.3.1
public class InitialBoundBuilder extends AbstractInferenceHelper {
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();

    public InitialBoundBuilder(TypeSolver solver) {
        super(solver);
    }

    public void reset() {
        this.bounds.clear();
    }

    public void buildInitialBounds(Map<VarType, MetaVarType> metaVars) {
        VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);

        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                //Case where P_l has no upper bound, alpha_l <: Object
                this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
            } else {
                //Case where P_l has upper bounds
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    //Upper bounds imply alpha_l <: T[P_1 = alpha_1... P_n = alpha_n], for each bound T
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, bound)));
                }

                if (!foundProperUpper) {
                    //If there is no proper upper bound, alpha_l <: Object
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
                }
            }
        });
    }

    public Set<TypeBound.Result.Builder> bounds() {
        return this.bounds;
    }

    public InitialBoundBuilder setBounds(Set<TypeBound.Result.Builder> bounds) {
        this.bounds = bounds;
        return this;
    }
}
