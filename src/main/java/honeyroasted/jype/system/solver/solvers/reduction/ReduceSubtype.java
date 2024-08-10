package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReduceSubtype implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            context.system().operations().compatibilityApplier()
                    .apply(context.system(), bound, TypeBound.Classification.BOUND, builder);
        } else if (bound.left().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            builder.setSatisfied(true);
        } else if (bound.right().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            builder.setSatisfied(false);
        } else if (bound.left() instanceof MetaVarType || bound.right() instanceof MetaVarType) {
            context.bounds().accept(TypeBound.Result.builder(bound, builder));
        } else if (bound.right() instanceof VarType vt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(vt)) {
                builder.setSatisfied(true);
            } else {
                builder.setSatisfied(false);
            }
        } else if (bound.right() instanceof MetaVarType mvt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(mvt)) {
                builder.setSatisfied(true);
            } else if (!mvt.lowerBounds().isEmpty()) {
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), mvt.lowerBound()), builder));
            } else {
                builder.setSatisfied(false);
            }
        } else if (bound.right() instanceof IntersectionType it) {
            it.children().forEach(t -> context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), builder)));
        } else if (bound.right() instanceof ClassType ct) {
            if (ct.hasAnyTypeArguments()) {
                TypeBound.Result.Builder classTypeMatch = TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), ct.classReference()), TypeBound.Result.Propagation.OR, builder);

                Optional<ClassType> supertypeOpt = identifySuperclass(ct.classReference(), bound.left(), classTypeMatch);
                if (supertypeOpt.isPresent()) {
                    ClassType supertype = supertypeOpt.get();
                    if (supertype.hasAnyTypeArguments() && supertype.typeArguments().size() == ct.typeArguments().size()) {
                        for (int i = 0; i < ct.typeArguments().size(); i++) {
                            Type ti = supertype.typeArguments().get(i);
                            Type si = ct.typeArguments().get(i);

                            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Contains(ti, si), builder));
                        }
                    } else {
                        builder.setSatisfied(false);
                    }
                } else {
                    builder.setSatisfied(false);
                }
            } else {
                context.system().operations().compatibilityApplier()
                        .apply(context.system(), bound, TypeBound.Classification.BOUND, builder);
            }
        } else if (bound.right() instanceof ArrayType at) {
            if (bound.left() instanceof ArrayType lat) {
                if (at.component() instanceof PrimitiveType && lat.component() instanceof PrimitiveType) {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(lat.component(), at.component()), builder));
                } else {
                    context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(lat.component(), at.component()), builder));
                }
            } else {
                Set<Type> arr = findMostSpecificArrayTypes(bound.left());
                if (arr.isEmpty()) {
                    builder.setSatisfied(false);
                } else {
                    arr.forEach(st -> context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(st, at), builder)));
                }
            }
        } else {
            builder.setSatisfied(false);
        }
    }

    private Optional<ClassType> identifySuperclass(ClassReference target, Type type, TypeBound.Result.Builder builder) {
        if (type instanceof ClassType ct) {
            Optional<ClassType> found = ct.relativeSupertype(ct);
            TypeBound.Result.builder(new TypeBound.Equal(ct.classReference(), target), builder)
                    .setSatisfied(found.isPresent());
            return found;
        } else {
            for (Type supertype : type.knownDirectSupertypes()) {
                if (supertype instanceof ClassType ct) {
                    Optional<ClassType> found = ct.relativeSupertype(target);
                    if (found.isPresent()) {
                        TypeBound.Result.builder(new TypeBound.Equal(ct.classReference(), target), builder)
                                .setSatisfied(true);
                        return found;
                    }
                }
            }

            for (Type supertype : type.knownDirectSupertypes()) {
                TypeBound.Result.Builder classTypeMatch = TypeBound.Result.builder(new TypeBound.Subtype(supertype, target), TypeBound.Result.Propagation.OR, builder);
                Optional<ClassType> found = identifySuperclass(target.classReference(), supertype, classTypeMatch);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
    }

    private Set<Type> findMostSpecificArrayTypes(Type type) {
        if (type instanceof ArrayType) {
            return Set.of(type);
        }

        Set<Type> current = new HashSet<>(type.knownDirectSupertypes());
        while (!current.isEmpty() && current.stream().allMatch(t -> t instanceof ArrayType)) {
            Set<Type> arrayTypes = current.stream().filter(t -> t instanceof ArrayType).collect(Collectors.toSet());
            if (!arrayTypes.isEmpty()) {
                current = arrayTypes;
            } else {
                Set<Type> next = new HashSet<>();
                current.forEach(t -> next.addAll(t.knownDirectSupertypes()));
                current = next;
            }
        }

        return type.typeSystem().operations().findMostSpecificTypes(current);
    }
}
