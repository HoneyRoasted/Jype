package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import java.util.Set;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;
import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class CompatibleSubtype implements UnaryTypeBoundMapper<TypeBound.Compatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Compatible cmpt &&
                (cmpt.context() == SUBTYPE || cmpt.context() == STRICT_INVOCATION);
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        results.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), constraint));
    }
}
