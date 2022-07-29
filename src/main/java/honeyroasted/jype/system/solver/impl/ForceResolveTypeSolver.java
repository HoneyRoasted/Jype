package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.type.TypeParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForceResolveTypeSolver extends AbstractTypeSolver {

    @Override
    public TypeSolution solve() {
        List<TypeConstraint> constraints = this.constraints.stream().map(TypeConstraint::flatten).toList();

        if (constraints.isEmpty()) {
            return new TypeSolution(new TypeContext(), root().flatten(), Collections.emptyList(), true);
        } else {
            List<TypeConstraint> unverifiable = new ArrayList<>();
            for (TypeConstraint constraint : constraints) {
                TypeConstraint resolved = forceResolve(constraint);
                if (!(resolved instanceof TypeConstraint.True)) {
                    unverifiable.add(resolved);
                }
            }

            return new TypeSolution(new TypeContext(), root().flatten(), unverifiable, unverifiable.isEmpty());
        }
    }

    private static TypeConstraint forceResolve(TypeConstraint constraint) {
        if (constraint instanceof TypeConstraint.And and) {
            return forceResolve(and);
        } else if (constraint instanceof TypeConstraint.Or or) {
            return forceResolve(or);
        } else if (constraint instanceof TypeConstraint.Equal equal) {
            return forceResolve(equal);
        } else if (constraint instanceof TypeConstraint.Bound bound) {
            return forceResolve(bound);
        } else if (constraint instanceof TypeConstraint.Not not) {
            return forceResolve(not);
        } else {
            return constraint;
        }
    }

    private static TypeConstraint forceResolve(TypeConstraint.And and) {
        List<TypeConstraint> resolved = and.constraints().stream().map(ForceResolveTypeSolver::forceResolve).toList();
        if (resolved.stream().anyMatch(c -> c instanceof TypeConstraint.False)) {
            return TypeConstraint.FALSE;
        } else if (resolved.stream().allMatch(c -> c instanceof TypeConstraint.True)) {
            return TypeConstraint.TRUE;
        } else if (resolved.size() == 1) {
            return resolved.get(0);
        }

        return new TypeConstraint.And(resolved);
    }

    private static TypeConstraint forceResolve(TypeConstraint.Or or) {
        List<TypeConstraint> resolved = or.constraints().stream().map(ForceResolveTypeSolver::forceResolve).toList();
        if (resolved.stream().anyMatch(c -> c instanceof TypeConstraint.True)) {
            return TypeConstraint.TRUE;
        } else if (resolved.stream().allMatch(c -> c instanceof TypeConstraint.False)) {
            return TypeConstraint.FALSE;
        } else if (resolved.size() == 1) {
            return resolved.get(0);
        }

        return new TypeConstraint.Or(resolved);
    }

    private static TypeConstraint forceResolve(TypeConstraint.Equal equal) {
        return equal.left().equals(equal.right()) ? TypeConstraint.TRUE : TypeConstraint.FALSE;
    }

    private static TypeConstraint forceResolve(TypeConstraint.Bound bound) {
        if (bound.subtype() instanceof TypeParameter a && bound.parent() instanceof TypeParameter b) {
            return forceResolve(a.bound().assignabilityTo(b.bound()));
        } else if (bound.subtype() instanceof TypeParameter prm) {
            return forceResolve(prm.bound().assignabilityTo(bound.parent()));
        } else if (bound.parent() instanceof TypeParameter prm) {
            return forceResolve(bound.subtype().assignabilityTo(prm.bound()));
        } else {
            return forceResolve(bound.subtype().assignabilityTo(bound.parent()));
        }
    }

    private static TypeConstraint forceResolve(TypeConstraint.Not not) {
        return forceResolve(not.constraint()).not();
    }


}
