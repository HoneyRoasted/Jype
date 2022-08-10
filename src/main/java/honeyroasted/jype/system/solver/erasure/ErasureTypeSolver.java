package honeyroasted.jype.system.solver.erasure;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.system.solver.force.ForceResolveTypeSolver;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ErasureTypeSolver extends AbstractTypeSolver implements TypeSolver {

    public ErasureTypeSolver(TypeSystem system) {
        super(system,
                ErasureConstraint.Erasure.class,
                TypeConstraint.Bound.class, TypeConstraint.Equal.class,
                TypeConstraint.True.class, TypeConstraint.False.class);
    }

    @Override
    public TypeSolution solve() {
        TypeContext context = new TypeContext();

        TypeVerification.Builder builder = TypeVerification.builder();

        this.constraints.forEach(t -> {
            if (t instanceof ErasureConstraint.Erasure erasure) {
                builder.children(erase(erasure, context));
            } else if (t instanceof TypeConstraint.Equal equal) {
                TypeVerification.Builder equalBuilder = TypeVerification.builder()
                        .constraint(equal)
                        .children(erase(new ErasureConstraint.Erasure(equal.left()), context),
                                erase(new ErasureConstraint.Erasure(equal.right()), context))
                        .and();

                if (equalBuilder.isSuccessful()) {
                    TypeConcrete left = context.get(equal.left()).get();
                    TypeConcrete right = context.get(equal.right()).get();

                    if (left.equals(right)) {
                        equalBuilder.children(TypeVerification.success(new TypeConstraint.Equal(left, right)));
                    } else {
                        equalBuilder.children(TypeVerification.failure(new TypeConstraint.Equal(left, right)));
                        equalBuilder.failure();
                    }
                }

                builder.children(equalBuilder.build());
            } else if (t instanceof TypeConstraint.Bound bound) {
                TypeVerification.Builder boundBuilder = TypeVerification.builder()
                        .constraint(bound)
                        .children(erase(new ErasureConstraint.Erasure(bound.subtype()), context),
                                erase(new ErasureConstraint.Erasure(bound.parent()), context))
                        .and();

                if (boundBuilder.isSuccessful()) {
                    TypeConcrete subtype = context.get(bound.subtype()).get();
                    TypeConcrete parent = context.get(bound.parent()).get();
                    boundBuilder.children(new ForceResolveTypeSolver(this.system)
                            .constrain(new TypeConstraint.Bound(subtype, parent))
                            .solve()
                            .verification());
                }

                boundBuilder.and();
                builder.children(boundBuilder.build());
            } else if (t instanceof TypeConstraint.True) {
                builder.children(TypeVerification.success(t));
            } else if (t instanceof TypeConstraint.False) {
                builder.children(TypeVerification.failure(t));
            }
        });

        builder.and();
        builder.constraint(builder.isSuccessful() ? TypeConstraint.TRUE : TypeConstraint.FALSE);

        TypeVerification res = builder.build();
        if (res.children().size() == 1 && res.success() == res.children().get(0).success()) {
            res = res.children().get(0);
        }

        return new TypeSolution(context, this.constraints(), res);
    }

    private TypeVerification erase(ErasureConstraint.Erasure constraint, TypeContext context) {
        TypeConcrete type = constraint.type();

        if (context.get(type).isPresent()) {
            return TypeVerification.success(constraint);
        }

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
            return commonParent(constraint, or, or.types(), context);
        } else {
            return TypeVerification.failure(constraint);
        }
    }

    private TypeVerification commonParent(TypeConstraint constraint, TypeOr or, Set<TypeConcrete> types, TypeContext context) {
        List<TypeVerification> verifications = types.stream().map(t -> erase(new ErasureConstraint.Erasure(t), context)).toList();

        if (verifications.stream().allMatch(TypeVerification::success)) {
            List<TypeConcrete> erased = types.stream().map(t -> context.get(t).get()).toList();

            List<TypeConcrete> parents = new ArrayList<>(erased);
            while (parents.stream().noneMatch(parent -> erased.stream().allMatch(sub -> this.system.isAssignableTo(sub, parent)))) {
                List<TypeConcrete> newParents = new ArrayList<>();
                parents.forEach(t -> newParents.addAll(parents(t)));
                parents = newParents;
            }

            List<TypeConcrete> commonParents = parents.stream().filter(parent -> erased.stream().allMatch(sub -> this.system.isAssignableTo(sub, parent))).toList();
            if (commonParents.isEmpty()) {
                context.put(or, this.system.OBJECT);
            } else {
                Optional<TypeConcrete> classParent = commonParents.stream().filter(t -> t instanceof TypeClass cls && cls.declaration().isInterface()).findFirst();
                if (classParent.isPresent()) {
                    context.put(or, classParent.get());
                } else {
                    context.put(or, commonParents.get(0));
                }
            }
        }

        return TypeVerification.builder()
                .constraint(constraint)
                .children(verifications)
                .and()
                .build();
    }

    private List<? extends TypeConcrete> parents(TypeConcrete type) {
        if (type instanceof TypePrimitive prim) {
            List<TypeConcrete> parents = new ArrayList<>();
            parents.add(this.system.box(prim));
            this.system.ALL_PRIMITIVES.stream().filter(t -> this.system.isAssignableTo(prim, t))
                    .forEach(parents::add);
            return parents;
        } else if (type instanceof TypeClass cls) {
            return cls.declaration().parents().stream().map(t -> t.declaration().withArguments()).toList();
        } else {
            return Collections.emptyList();
        }
    }

}
