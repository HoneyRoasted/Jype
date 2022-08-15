package honeyroasted.jype.system.solver.force;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.system.solver.erasure.ErasureTypeSolver;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;
import honeyroasted.jype.type.TypePrimitive;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * This class represents a {@link TypeSolver} capable of testing assignability between types where no inference
 * is required. Any {@link TypeParameter}s are evaluated as if they were equal to their bounds, and any {@link TypeParameter}s
 * which contain circular references are substituted with {@code ? extends Object}. Generally, if this {@link TypeSolver}
 * finds a solution, it can be considered a valid solution. However, if no solution is found, that does not guarantee that
 * a solution could not be found for some substitution of {@link TypeParameter}s. The accepted constraints for this
 * {@link TypeSolver} are:
 * <ul>
 *     <li>{@link TypeConstraint.Bound}</li>
 *     <li>{@link TypeConstraint.Equal}</li>
 *     <li>{@link TypeConstraint.True}</li>
 *     <li>{@link TypeConstraint.False}</li>
 * </ul>
 */
public class ForceResolveTypeSolver extends AbstractTypeSolver implements TypeSolver {

    /**
     * Creates a new {@link ForceResolveTypeSolver}.
     *
     * @param system The {@link TypeSystem} associated with this {@link ForceResolveTypeSolver}
     */
    public ForceResolveTypeSolver(TypeSystem system) {
        super(system,
                TypeConstraint.Bound.class, TypeConstraint.Equal.class,
                TypeConstraint.True.class, TypeConstraint.False.class);
    }

    @Override
    public TypeSolution solve() {
        TypeVerification.Builder builder = TypeVerification.builder();

        List<TypeConstraint> constraints = List.copyOf(this.constraints);
        constraints.forEach(t -> builder.children(forceResolve(t)));

        builder.and();
        builder.constraint(builder.isSuccessful() ? TypeConstraint.TRUE : TypeConstraint.FALSE);

        TypeVerification res = builder.build();
        if (res.children().size() == 1 && res.success() == res.children().get(0).success()) {
            res = res.children().get(0);
        }

        return new TypeSolution(new TypeContext(), constraints, res);
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
        if (a.isCircular()) {
            a = this.system.deCircularize(a);
        }

        if (b.isCircular()) {
            b = this.system.deCircularize(b);
        }

        if (a instanceof TypeNone || b instanceof TypeNone) {
            return TypeVerification.failure(constraint);
        } else if (Objects.equals(a, b)) {
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(TypeVerification.success(new TypeConstraint.Equal(a, b)))
                    .build();
        } else if (a instanceof TypeIn || b instanceof TypeOut) {
            return TypeVerification.failure(constraint);
        } else if (a instanceof TypeOr or) {
            if (or.types().size() == 1) {
                return assignability(a, or.types().iterator().next());
            }

            TypeConcrete finalB = b;
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(or.types().stream().map(t -> assignability(t, finalB)).toList())
                    .and()
                    .build();
        } else if (a instanceof TypeAnd and) {
            if (and.types().size() == 1) {
                return assignability(a, and.types().iterator().next());
            }

            TypeConcrete finalB1 = b;
            return TypeVerification.builder()
                    .constraint(constraint)
                    .children(and.types().stream().map(t -> assignability(t, finalB1)).toList())
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
        } else if (a instanceof TypeParameterized cls) {
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
        } else if (other instanceof TypeParameterized cls && this.system.unbox(cls).isPresent()) {
            TypePrimitive prim = this.system.unbox(cls).get();
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

    private TypeVerification assignability(TypeConstraint constraint, TypeParameterized in, TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            Optional<TypePrimitive> unbox = this.system.unbox(in);
            if (unbox.isPresent()) {
                return assignability(unbox.get(), prim);
            }
        } else if (other instanceof TypeParameterized otherClass) {
            TypeParameterized self = in;
            if (!in.declaration().equals(otherClass.declaration())) {
                Optional<TypeParameterized> parent = in.parent(otherClass.declaration());
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
