package honeyroasted.jype.system.solver.erasure;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

public class ErasureTypeSolver extends AbstractTypeSolver implements TypeSolver {

    public ErasureTypeSolver(TypeSystem system) {
        super(system,
                ErasureConstraint.Erasure.class);
    }

    @Override
    public TypeSolution solve() {
        TypeContext context = new TypeContext();

        TypeVerification.Builder builder = TypeVerification.builder();

        this.constraints.forEach(t -> {
            if (t instanceof ErasureConstraint.Erasure erasure) {
                builder.children(erase(erasure, context));
            }
        });

        builder.and();
        builder.constraint(builder.isSuccessful() ? TypeConstraint.TRUE : TypeConstraint.FALSE);

        return new TypeSolution(context, this.constraints(), builder.build());
    }

    private TypeVerification erase(ErasureConstraint.Erasure constraint, TypeContext context) {
        TypeConcrete type = constraint.type();

        if (type instanceof TypePrimitive || type instanceof TypeNone) {
            context.put(type, type);
            return TypeVerification.success(constraint);
        } else if (type instanceof TypeIn || type instanceof TypeNull) {
            context.put(type, system.OBJECT);
            return TypeVerification.success(constraint);
        } else if (type instanceof TypeClass cls) {
          context.put(cls, cls.declaration().withArguments());
          return TypeVerification.success(constraint);
        } else if (type instanceof TypeParameter parameter) {
            TypeVerification bound = erase(new ErasureConstraint.Erasure(parameter.bound()), context);
            if (bound.success()) {
                context.put(parameter, context.get(parameter.bound()).get());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(bound)
                    .and()
                    .build();
        } else if (type instanceof TypeOut out) {
            TypeVerification bound = erase(new ErasureConstraint.Erasure(out.bound()), context);
            if (bound.success()) {
                context.put(out, context.get(out.bound()).get());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(bound)
                    .and()
                    .build();
        } else if (type instanceof TypeArray arr) {
            TypeVerification element = erase(new ErasureConstraint.Erasure(arr.element()), context);
            if (element.success()) {
                context.put(arr, this.system.newArray(arr.element()));
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(element)
                    .and()
                    .build();
        } else if (type instanceof TypeAnd and) {
            if (and.types().isEmpty()) {
                return TypeVerification.failure(constraint);
            } else {
                TypeConcrete first = and.types().iterator().next();
                TypeVerification bound = erase(new ErasureConstraint.Erasure(first), context);
                if (bound.success()) {
                    context.put(and, context.get(first).get());
                }

                return TypeVerification.builder()
                        .constraint(constraint)
                        .children(bound)
                        .and()
                        .build();
            }
        } else if (type instanceof TypeOr or) {
            return null;
        } else {
            return TypeVerification.failure(constraint);
        }
    }

}
