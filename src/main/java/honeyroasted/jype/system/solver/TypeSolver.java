package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypeParameterReference;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.declaration.TypeParameter;
import honeyroasted.jype.system.TypeConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeSolver {
    private List<TypeConstraint> constraints = new ArrayList<>();

    public TypeSolver constrain(TypeConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public TypeSolver subtype(TypeConcrete subtype, TypeConcrete parent) {
        return this.constrain(subtype.assignabilityTo(parent));
    }

    public TypeSolution solve() {
        TypeConstraint.And root = new TypeConstraint.And(this.constraints);

        Map<TypePlaceholder, List<TypeConstraint>> placeholders = new HashMap<>();
        Map<TypeParameter, List<TypeConstraint>> parameters = new HashMap<>();

        gatherPlaceholders(root, placeholders, parameters);

        //TODO

        return new TypeSolution(null, root, List.of(root));
    }

    private static void gatherPlaceholders(TypeConstraint constraint, Map<TypePlaceholder, List<TypeConstraint>> placeholders, Map<TypeParameter, List<TypeConstraint>> parameters) {
        if (constraint instanceof TypeConstraint.And and) {
            and.constraints().forEach(tc -> gatherPlaceholders(tc, placeholders, parameters));
        } else if (constraint instanceof TypeConstraint.Or or) {
            or.constraints().forEach(tc -> gatherPlaceholders(tc, placeholders, parameters));
        } else if (constraint instanceof TypeConstraint.Equal eq) {
            tryAdd(eq.left(), constraint, placeholders, parameters);
            tryAdd(eq.left(), constraint, placeholders, parameters);
        } else if (constraint instanceof TypeConstraint.Bound bnd) {
            tryAdd(bnd.subtype(), constraint, placeholders, parameters);
            tryAdd(bnd.parent(), constraint, placeholders, parameters);
        }
    }

    private static void tryAdd(TypeConcrete type, TypeConstraint constraint, Map<TypePlaceholder, List<TypeConstraint>> placeholders, Map<TypeParameter, List<TypeConstraint>> parameters) {
        if (type instanceof TypePlaceholder plc) {
            placeholders.computeIfAbsent(plc, key -> new ArrayList<>()).add(constraint);
        } else if (type instanceof TypeParameter prm) {
            parameters.computeIfAbsent(prm, key -> new ArrayList<>()).add(constraint);
        } else if (type instanceof TypeParameterReference ref) {
            parameters.computeIfAbsent(ref.variable(), key -> new ArrayList<>()).add(constraint);
        }
    }

}
