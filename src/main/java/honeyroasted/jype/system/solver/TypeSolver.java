package honeyroasted.jype.system.solver;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TypeSolver {
    private List<TypeConstraint> constraints = new ArrayList<>();
    private Predicate<Type> consider;

    public TypeSolver(Predicate<Type> consider) {
        this.consider = consider;
    }

    public TypeSolver constrain(TypeConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public TypeSolver constrain(TypeConcrete subtype, TypeConcrete parent) {
        return this.constrain(subtype.assignabilityTo(parent));
    }

    public TypeSolution solve() {
        TypeConstraint.And root = new TypeConstraint.And(this.constraints);
        TypeContext context = new TypeContext();

        return new TypeSolution(null, root, List.of(root));
    }

    private static void walk(TypeConstraint constraint, Consumer<TypeConstraint> consumer) {
        consumer.accept(constraint);

        if (constraint instanceof TypeConstraint.And and) {
            and.constraints().forEach(t -> walk(t, consumer));
        } else if (constraint instanceof TypeConstraint.Or or) {
            or.constraints().forEach(t -> walk(t, consumer));
        }
    }


}
