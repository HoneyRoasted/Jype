package honeyroasted.jype.system.solver.impl.force;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.system.solver.impl.AbstractTypeSolver;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ForceResolveTypeSolver extends AbstractTypeSolver {

    public ForceResolveTypeSolver(TypeSystem system) {
        super(system);
    }

    @Override
    public TypeSolution solve() {
        TypeVerification.Builder builder = TypeVerification.builder();

        List<TypeConstraint> constraints = List.copyOf(this.constraints);
        constraints.forEach(t -> builder.children(forceResolve(t)));

        builder.and();
        builder.constraint(builder.isSuccessful() ? TypeConstraint.TRUE : TypeConstraint.FALSE);

        return new TypeSolution(new TypeContext(), constraints, builder.build());
    }

    private TypeVerification forceResolve(TypeConstraint constraint) {
        if (constraint instanceof TypeConstraint.Equal equal) {
            return forceResolve(equal);
        } else if (constraint instanceof TypeConstraint.Bound bound) {
            return forceResolve(bound);
        } else if (constraint instanceof TypeConstraint.True) {
            return TypeVerification.success(constraint);
        } else {
            return TypeVerification.failure(constraint);
        }
    }

    private TypeVerification forceResolve(TypeConstraint.Equal equal) {
        return TypeVerification.builder()
                .constraint(equal)
                .success(equal.left().flatten().equals(equal.right().flatten()))
                .build();
    }

    private TypeVerification forceResolve(TypeConstraint.Bound bound) {
        return assignability(bound, bound.subtype(), bound.parent());
    }

    private TypeVerification assignability(TypeConcrete a, TypeConcrete b) {
        return assignability(new TypeConstraint.Bound(a, b), a, b);
    }

    private TypeVerification assignability(TypeConstraint constraint, TypeConcrete a, TypeConcrete b) {
        if (a instanceof TypeIn || a instanceof TypeNone ||
                b instanceof TypeOut || b instanceof TypeNone) {
            return TypeVerification.failure(constraint);
        } else if (Objects.equals(a, b)) {
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(TypeVerification.success(new TypeConstraint.Equal(a, b)))
                    .build();
        } else if (a instanceof TypeOr or) {
            if (or.types().size() == 1) {
                return assignability(a, or.types().iterator().next());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(or.types().stream().map(t -> assignability(t, b)).toList())
                    .and()
                    .build();
        } else if (a instanceof TypeAnd and) {
            if (and.types().size() == 1) {
                return assignability(a, and.types().iterator().next());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(and.types().stream().map(t -> assignability(t, b)).toList())
                    .or()
                    .build();
        } else if (a instanceof TypeOut out) {
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(assignability(out.bound(), b))
                    .and()
                    .build();
        } else if (a instanceof TypeParameter ref) {
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(assignability(ref.bound(), b))
                    .and()
                    .build();
        } else if (a instanceof TypeClass cls) {
            return assignability(constraint, cls, b);
        } else if (a instanceof TypeArray arr) {
            return assignability(constraint, arr, b);
        } else if (a instanceof TypePrimitive prim) {
            return assignability(constraint, prim, b);
        } else {
            throw new IllegalArgumentException("Unknown type: " + (a == null ? "null" : a.getClass()));
        }
    }

    private TypeVerification assignability(TypeConstraint constraint, TypeArray self, TypeConcrete other) {
        if (other instanceof TypeArray arr) {
            return TypeVerification.builder()
                    .children(assignability(constraint, self.element(), arr.element()))
                    .and()
                    .build();
        } else {
            return TypeVerification.builder()
                    .children(assignability(this.system.OBJECT, other),
                            defaultAssignability(constraint, self, other))
                    .or()
                    .build();
        }
    }

    private static final Map<String, Set<String>> PRIM_SUPERS = Map.of(
            "Z", Set.of("Z"),
            "B", Set.of("B", "S", "C", "I", "J", "F", "D"),
            "S", Set.of("S", "C", "I", "J", "F", "D"),
            "C", Set.of("C", "S", "I", "J", "F", "D"),
            "I", Set.of("I", "J", "F", "D"),
            "J", Set.of("J", "F", "D"),
            "F", Set.of("F", "D"),
            "D", Set.of("D")
    );

    private TypeVerification assignability(TypeConstraint constraint, TypePrimitive self, TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            return PRIM_SUPERS.get(self.descriptor()).contains(prim.descriptor()) ?
                    TypeVerification.success(constraint) :
                    TypeVerification.failure(constraint);
        } else {
            return TypeVerification.builder()
                    .children(assignability(this.system.box(self), other),
                            defaultAssignability(constraint, self, other))
                    .or()
                    .build();
        }
    }

    private TypeVerification assignability(TypeConstraint constraint, TypeClass in, TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            Optional<TypePrimitive> unbox = this.system.unbox(in);
            if (unbox.isPresent()) {
                return assignability(unbox.get(), prim);
            }
        } else if (other instanceof TypeClass otherClass) {
            TypeClass self = in;
            if (!in.declaration().equals(otherClass.declaration())) {
                Optional<TypeClass> parent = in.parent(otherClass.declaration());
                if (parent.isPresent()) {
                    self = parent.get();
                } else {
                    return TypeVerification.builder()
                            .children(TypeVerification.failure(new TypeConstraint.Bound(in.declaration().withArguments(), otherClass.declaration().withArguments())))
                            .failure()
                            .constraint(constraint)
                            .build();
                }
            }

            if (self.arguments().isEmpty() || otherClass.arguments().isEmpty()) {
                return TypeVerification.success(constraint);
            } else if (self.arguments().size() != otherClass.arguments().size()) {
                return TypeVerification.failure(constraint);
            } else {
                TypeVerification.Builder builder = TypeVerification.builder()
                        .constraint(constraint)
                        .children(TypeVerification.success(new TypeConstraint.Bound(in.declaration().withArguments(), otherClass.declaration().withArguments())));

                for (int i = 0; i < self.arguments().size(); i++) {
                    TypeConcrete ti = self.arguments().get(i);
                    TypeConcrete si = otherClass.arguments().get(i);

                    if (si instanceof TypeOut typeOut) { //? extends X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().map(t -> t instanceof TypeParameter ref ? otherClass.argument(ref).orElse(ref) : t);

                        builder.children(
                                TypeVerification.builder()
                                        .constraint(new ForceConstraint.Capture(si, ti))
                                        .children(assignability(ti, bound),
                                                assignability(ti, typeOut.bound()))
                                        .and()
                                        .build()
                        );
                    } else if (si instanceof TypeIn typeIn) { //? super X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().map(t -> t instanceof TypeParameter ref ? otherClass.argument(ref).orElse(ref) : t);

                        builder.children(
                                TypeVerification.builder()
                                        .constraint(new ForceConstraint.Capture(si, ti))
                                        .children(assignability(ti, bound),
                                                assignability(typeIn.bound(), ti))
                                        .and()
                                        .build()
                        );
                    } else {
                        builder.children(forceResolve(new TypeConstraint.Equal(ti, si)));
                    }
                }

                return builder.and().build();
            }
        }

        return defaultAssignability(constraint, in, other);
    }

    private TypeVerification defaultAssignability(TypeConstraint constraint, TypeConcrete a, TypeConcrete b) {
        if (b instanceof TypeOr or) {
            if (or.types().size() == 1) {
                return assignability(a, or.types().iterator().next());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(or.types().stream().map(t -> assignability(a, t)).toList())
                    .or()
                    .build();
        } else if (b instanceof TypeAnd and) {
            if (and.types().size() == 1) {
                return assignability(a, and.types().iterator().next());
            }

            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(and.types().stream().map(t -> assignability(a, t)).toList())
                    .and()
                    .build();
        } else if (b instanceof TypeIn in) {
            return TypeVerification
                    .builder()
                    .constraint(constraint)
                    .children(forceResolve(new TypeConstraint.Bound(a, in.bound())))
                    .and()
                    .build();
        } else if (b instanceof TypeParameter ref) {
            return TypeVerification
                    .builder()
                    .constraint(constraint)
                    .children(forceResolve(new TypeConstraint.Bound(a, ref.bound())))
                    .and()
                    .build();
        }

        return TypeVerification.failure(constraint);
    }

}
