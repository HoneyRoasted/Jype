package honeyroasted.jype.system.solver._old.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.solvers.NoOpTypeSolver;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeConstraintReducer extends AbstractInferenceHelper {
    private TypeCompatibilityChecker compatibilityChecker;
    private TypeSetOperations setOperations;
    private TypeInitialBoundBuilder initialBoundBuilder;
    private TypeBoundIncorporater boundIncorporater;

    public TypeConstraintReducer() {
        this(new NoOpTypeSolver());
    }

    public TypeConstraintReducer(TypeSolver solver) {
        super(solver);
        this.compatibilityChecker = new TypeCompatibilityChecker(solver);
        this.setOperations = new TypeSetOperations(solver);
        this.initialBoundBuilder = new TypeInitialBoundBuilder(solver);
        this.boundIncorporater = new TypeBoundIncorporater(solver);
    }

    private Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();

    public Set<TypeBound.Result.Builder> bounds() {
        return this.bounds;
    }

    public Set<TypeBound.Result.Builder> constraints() {
        return this.constraints;
    }

    public void reset() {
        this.bounds.clear();
        this.constraints.clear();
        this.initialBoundBuilder.reset();
        this.boundIncorporater.reset();
    }

    public TypeConstraintReducer addBounds(Set<TypeBound.Result.Builder> bounds) {
        this.bounds.addAll(bounds);
        return this;
    }

    public TypeConstraintReducer reduce(Set<TypeBound.Result.Builder> constraints) {
        this.constraints.addAll(constraints);

        do {
            this.reduceOnce();
        } while (!this.constraints.isEmpty() &&
            this.bounds.stream().noneMatch(b -> b.bound().equals(TypeBound.False.INSTANCE)));
        return this;
    }

    public void reduceOnce() {
        Set<TypeBound.Result.Builder> current = new HashSet<>(this.constraints);
        this.constraints.clear();

        for (TypeBound.Result.Builder constraint : current) {
            this.reduce(constraint);

            this.boundIncorporater.incorporate(this.bounds);
            this.constraints.addAll(this.boundIncorporater.constraints());
            this.bounds.addAll(this.boundIncorporater.bounds());

            this.boundIncorporater.reset();
        }
    }

    private void reduce(TypeBound.Result.Builder constraint) {
        TypeBound bound = constraint.bound();
        if (bound instanceof TypeBound.Subtype st) {
            reduce(constraint, st);
        } else if (bound instanceof TypeBound.Compatible cmp) {
            reduce(constraint, cmp);
        } else if (bound instanceof TypeBound.ExpressionCompatible ecm) {
            reduce(constraint, ecm);
        } else if (bound instanceof TypeBound.Contains con) {
            reduce(constraint, con);
        } else if (bound instanceof TypeBound.Equal eq) {
            reduce(constraint, eq);
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Equal equal) {
        Type s = equal.left();
        Type t = equal.right();

        if (s.isProperType() && t.isProperType()) {
            if (s.typeEquals(t)) {
                builder.setSatisfied(true);
            } else {
                this.failOnConstraint(builder);
            }
        } else if (s.isNullType() || t.isNullType()) {
            this.failOnConstraint(builder);
        } else if (s instanceof MetaVarType && !(t instanceof PrimitiveType)) {
            this.bounds.add(TypeBound.Result.builder(new TypeBound.Equal(s, t), builder));
        } else if (t instanceof MetaVarType && !(s instanceof PrimitiveType)) {
            this.bounds.add(TypeBound.Result.builder(new TypeBound.Equal(t, s), builder));
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        if (bound.left().isSimplyTyped()) {
            builder.setPropagation(TypeBound.Result.Propagation.INHERIT);
            this.constraints.add(TypeBound.Result.builder(new TypeBound.Compatible(bound.left().getSimpleType(bound.right().typeSystem()).get(), bound.right()), builder));
        } else {
            //TODO
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Contains contains) {
        Type s = contains.left();
        Type t = contains.right();

        if (t instanceof WildType) {
            if (t instanceof WildType.Upper wtu) {
                if (wtu.hasDefaultBounds()) {
                    builder.setSatisfied(true);
                } else {
                    if (s instanceof WildType) {
                        if (s instanceof WildType.Upper swtu) {
                            if (swtu.hasDefaultBounds()) {
                                this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(s.typeSystem().constants().object(), wtu.upperBound()), builder));
                            } else {
                                this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(swtu.upperBound(), wtu.upperBound()), builder));
                            }
                        } else if (s instanceof WildType.Lower swtl) {
                            this.constraints.add(TypeBound.Result.builder(new TypeBound.Equal(s.typeSystem().constants().object(), wtu.upperBound()), builder));
                        }
                    } else {
                        this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(s, wtu.upperBound()), builder));
                    }
                }
            } else if (t instanceof WildType.Lower wtl) {
                if (s instanceof WildType) {
                    if (s instanceof WildType.Lower swtl) {
                        this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(wtl.lowerBound(), swtl.lowerBound()), builder));
                    } else {
                        this.failOnConstraint(builder);
                    }
                } else {
                    this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(wtl.lowerBound(), s), builder));
                }
            }
        } else {
            if (s instanceof WildType) {
                this.failOnConstraint(builder);
            } else {
                this.constraints.add(TypeBound.Result.builder(new TypeBound.Equal(s, t), builder));
            }
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            this.compatibilityChecker.check(bound, builder);
        } else if (bound.left() instanceof PrimitiveType pt) {
            this.constraints.add(TypeBound.Result.builder(new TypeBound.Compatible(pt.box(), bound.right(), bound.context()), builder));
        } else if (bound.right() instanceof PrimitiveType pt) {
            this.constraints.add(TypeBound.Result.builder(new TypeBound.Equal(bound.left(), pt.box()), builder));
        } else if (bound.left() instanceof ClassType pct && pct.hasAnyTypeArguments() && bound.right() instanceof ClassType ct && !ct.hasAnyTypeArguments() &&
                this.compatibilityChecker.isSubtype(pct.classReference(), ct.classReference(), builder)) {
            builder.setSatisfied(true);
        } else if (bound.left() instanceof ArrayType at && at.deepComponent() instanceof ClassType pct && pct.hasAnyTypeArguments() &&
                bound.right() instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() && this.compatibilityChecker.isSubtype(pct.classReference(), rpct.classReference(), builder)) {
            builder.setSatisfied(true);
        } else {
            this.constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder));
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            this.bounds.add(this.compatibilityChecker.check(bound, builder));
        } else if (bound.left().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            builder.setSatisfied(true);
        } else if (bound.right().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            this.failOnConstraint(builder);
        } else if (bound.left() instanceof MetaVarType || bound.right() instanceof MetaVarType) {
            this.bounds.add(TypeBound.Result.builder(bound, builder));
        } else if (bound.right() instanceof VarType vt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(vt)) {
                builder.setSatisfied(true);
            } else {
                this.failOnConstraint(builder);
            }
        } else if (bound.right() instanceof MetaVarType mvt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(mvt)) {
                builder.setSatisfied(true);
            } else if (!mvt.lowerBounds().isEmpty()) {
                this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), mvt.lowerBound()), builder));
            } else {
                this.failOnConstraint(builder);
            }
        } else if (bound.right() instanceof IntersectionType it) {
            it.children().forEach(t -> this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), builder)));
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

                            this.constraints.add(TypeBound.Result.builder(new TypeBound.Contains(ti, si), builder));
                        }
                    } else {
                        this.failOnConstraint(builder);
                    }
                } else {
                    this.failOnConstraint(builder);
                }
            } else {
                this.compatibilityChecker.check(bound, builder);
            }
        } else if (bound.right() instanceof ArrayType at) {
            if (bound.left() instanceof ArrayType lat) {
                if (at.component() instanceof PrimitiveType && lat.component() instanceof PrimitiveType) {
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Equal(lat.component(), at.component()), builder));
                } else {
                    this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(lat.component(), at.component()), builder));
                }
            } else {
                Set<Type> arr = findMostSpecificArrayTypes(bound.left());
                if (arr.isEmpty()) {
                    this.failOnConstraint(builder);
                } else {
                    arr.forEach(st -> this.bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(st, at), builder)));
                }
            }
        } else {
            this.failOnConstraint(builder);
        }
    }

    private void failOnConstraint(TypeBound.Result.Builder constraint) {
        constraint.setPropagation(TypeBound.Result.Propagation.NONE);
        constraint.setSatisfied(false);
        this.bounds.add(TypeBound.Result.builder(TypeBound.False.INSTANCE, constraint).setSatisfied(false));
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

        return this.setOperations.findMostSpecificTypes(current);
    }

}
