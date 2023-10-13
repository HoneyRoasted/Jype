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

        Set<TypeBound> bounds = new LinkedHashSet<>();
        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object())));
            } else {
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(mvt, bound)));
                }

                if (!foundProperUpper) {
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
