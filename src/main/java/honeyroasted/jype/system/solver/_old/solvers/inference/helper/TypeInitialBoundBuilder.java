package honeyroasted.jype.system.solver._old.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//Implements creating an initial bounds set as defined in 18.3.1
public class TypeInitialBoundBuilder extends AbstractInferenceHelper {
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();

    public TypeInitialBoundBuilder(TypeSolver solver) {
        super(solver);
    }

    public TypeInitialBoundBuilder() {
        super();
    }

    public void reset() {
        this.bounds.clear();
    }

    public TypeInitialBoundBuilder buildInitialBounds(Map<VarType, MetaVarType> metaVars) {
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
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, resolved)));
                }

                if (!foundProperUpper) {
                    //If there is no proper upper bound, alpha_l <: Object
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
                }
            }
        });
        return this;
    }

    public Set<TypeBound.Result.Builder> bounds() {
        return this.bounds;
    }

    public TypeInitialBoundBuilder setBounds(Set<TypeBound.Result.Builder> bounds) {
        this.bounds = bounds;
        return this;
    }
}
