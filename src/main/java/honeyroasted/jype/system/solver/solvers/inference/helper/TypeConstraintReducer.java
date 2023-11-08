package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.LinkedHashSet;
import java.util.Set;

public class TypeConstraintReducer extends AbstractInferenceHelper {
    private TypeCompatibilityChecker compatibilityChecker;
    private TypeLubFinder lubFinder;
    private TypeInitialBoundBuilder initialBoundBuilder;

    public TypeConstraintReducer() {
        this(TypeSolver.NO_OP);
    }

    public TypeConstraintReducer(TypeSolver solver) {
        super(solver);
        this.compatibilityChecker = new TypeCompatibilityChecker(solver);
        this.lubFinder = new TypeLubFinder(solver);
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
        } else if (bound.left() instanceof ParameterizedClassType pct) {
            //TODO
        } else if (bound.left() instanceof ArrayType at && at.deepComponent() instanceof ParameterizedClassType pct) {
            //TODO
        } else {
            this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder)));
        }
    }

    private void reduce(TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            this.compatibilityChecker.check(bound, builder);
        } else if (bound.left().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            this.eventBoundSatisfied(builder.setSatisfied(true));
        } else if (bound.right().isNullType()) {
            builder.setPropagation(TypeBound.Result.Propagation.NONE);
            this.eventBoundUnsatisfied(builder.setSatisfied(false));
        } else if (bound.left() instanceof MetaVarType || bound.right() instanceof MetaVarType) {
            this.bounds.add(TypeBound.Result.builder(bound, builder));
        }
    }

}
