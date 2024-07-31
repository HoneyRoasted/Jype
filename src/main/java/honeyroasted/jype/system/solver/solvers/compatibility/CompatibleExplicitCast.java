package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;
import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class CompatibleExplicitCast implements UnaryTypeBoundMapper<TypeBound.Compatible> {

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Compatible cmp &&
                cmp.context() == EXPLICIT_CAST;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.OR);
        addAll(constraints,
                TypeBound.Result.builder(new TypeBound.Compatible(bound.left(), bound.right(), LOOSE_INVOCATION), constraint),
                TypeBound.Result.builder(new TypeBound.Compatible(bound.right(), bound.left(), LOOSE_INVOCATION), constraint));
    }
}
