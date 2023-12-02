package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeConstraintReducer extends AbstractInferenceHelper {
    private TypeCompatibilityChecker compatibilityChecker;
    private TypeSetOperations setOperations;
    private TypeInitialBoundBuilder initialBoundBuilder;

    public TypeConstraintReducer() {
        this(TypeSolver.NO_OP);
    }

    public TypeConstraintReducer(TypeSolver solver) {
        super(solver);
        this.compatibilityChecker = new TypeCompatibilityChecker(solver);
        this.setOperations = new TypeSetOperations(solver);
        this.initialBoundBuilder = new TypeInitialBoundBuilder(solver);
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
    }

    public TypeConstraintReducer addBounds(Set<TypeBound.Result.Builder> bounds) {
        this.bounds.addAll(bounds);
        return this;
    }

    public TypeConstraintReducer reduce(Set<TypeBound.Result.Builder> constraints) {
        return this;
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            this.compatibilityChecker.check(bound, builder);
        } else if (bound.left() instanceof PrimitiveType pt) {
            this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Compatible(pt.box(), bound.right(), bound.context()), builder)));
        } else if (bound.right() instanceof PrimitiveType pt) {
            this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(bound.left(), pt.box()), builder)));
        } else if (bound.left() instanceof ParameterizedClassType pct && bound.right() instanceof ClassType ct && !ct.hasTypeArguments() &&
            this.compatibilityChecker.isSubtype(pct.classReference(), ct.classReference(), builder)) {
            this.eventBoundSatisfied(builder.setSatisfied(true));
        } else if (bound.left() instanceof ArrayType at && at.deepComponent() instanceof ParameterizedClassType pct &&
            bound.right() instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasTypeArguments() &&
            at.depth() == rat.depth() && this.compatibilityChecker.isSubtype(pct.classReference(), rpct.classReference(), builder)) {
            this.eventBoundSatisfied(builder.setSatisfied(true));
        } else {
            this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder)));
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            this.bounds.add(this.compatibilityChecker.check(bound, builder));
        } else if (bound.left().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            this.eventBoundSatisfied(builder.setSatisfied(true));
        } else if (bound.right().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            this.eventBoundUnsatisfied(builder.setSatisfied(false));
        } else if (bound.left() instanceof MetaVarType || bound.right() instanceof MetaVarType) {
            this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(bound, builder)));
        } else if (bound.right() instanceof VarType vt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(vt)) {
                this.eventBoundSatisfied(builder.setSatisfied(true));
            } else {
                this.eventBoundUnsatisfied(builder.setSatisfied(false));
            }
        } else if (bound.right() instanceof MetaVarType mvt) {
            if (bound.left() instanceof IntersectionType it && it.typeContains(mvt)) {
                this.eventBoundSatisfied(builder.setSatisfied(true));
            } else if (!mvt.lowerBounds().isEmpty()) {
                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), mvt.lowerBound()), builder)));
            } else {
                this.eventBoundUnsatisfied(builder.setSatisfied(false));
            }
        } else if (bound.right() instanceof IntersectionType it) {
            it.children().forEach(t -> this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), builder))));
        } else if (bound.right() instanceof ClassType ct) {

        } else if (bound.right() instanceof ArrayType at) {
            if (bound.left() instanceof ArrayType lat) {
                if (at.component() instanceof PrimitiveType && lat.component() instanceof PrimitiveType) {
                    this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(lat.component(), at.component()), builder)));
                } else {
                    this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(lat.component(), at.component()), builder)));
                }
            } else {
                Set<Type> arr = findMostSpecificArrayTypes(bound.left());
                if (arr.isEmpty()) {
                    builder.setPropagation(TypeBound.Result.Propagation.NONE);
                    this.eventBoundUnsatisfied(builder.setSatisfied(false));
                } else {
                    arr.forEach(st -> this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(st, at), builder))));
                }
            }
        } else {
            this.eventBoundUnsatisfied(builder.setSatisfied(false));
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
