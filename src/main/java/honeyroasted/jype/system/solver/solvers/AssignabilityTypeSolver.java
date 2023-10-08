package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.exception.TypeSolverUnsolvableException;
import honeyroasted.jype.type.*;

import java.util.*;
import java.util.stream.Collectors;

public class AssignabilityTypeSolver extends AbstractTypeSolver {
    private static final Map<String, Set<String>> PRIM_SUPERS =
            Map.of(
                    "boolean", Set.of("boolean"),
                    "byte", Set.of("byte", "short", "int", "long", "float", "double"),
                    "short", Set.of("short", "int", "long", "float", "double"),
                    "char", Set.of("char", "int", "long", "float", "double"),
                    "int", Set.of("int", "long", "float", "double"),
                    "long", Set.of("long", "float", "double"),
                    "float", Set.of("float", "double"),
                    "double", Set.of("double")
            );

    public AssignabilityTypeSolver() {
        super(TypeBound.Equal.class,
                TypeBound.Subtype.class,
                TypeBound.NonCyclic.class,
                TypeBound.Not.class,
                TypeBound.And.class,
                TypeBound.Or.class);
    }

    private Set<TypeBound.Result.Builder> workingBounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> results = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> insights = new LinkedHashSet<>();

    private Set<TypeBound> satisfiedCache = new HashSet<>();

    @Override
    public Result solve(TypeSystem system) {
        return this.solve(system, Long.MAX_VALUE);
    }

    public Result solve(TypeSystem system, long maxIters) {
        this.initialBounds.forEach(bound -> {
            TypeBound.Result.Builder builder = TypeBound.Result.builder(bound);
            workingBounds.add(builder);
            results.add(builder);
        });

        for (long c = 0; c < maxIters && !this.workingBounds.isEmpty(); c++) {
            this.iterate(system);
        }

        if (!this.workingBounds.isEmpty()) {
            throw new TypeSolverUnsolvableException("Failed to solve type bounds within " + maxIters + " iteration(s)", Set.copyOf(this.initialBounds));
        }

        Set<TypeBound.Result> built = new LinkedHashSet<>();
        boolean success = true;
        for (TypeBound.Result.Builder builder : this.results) {
            TypeBound.Result result = builder.build();
            success &= result.satisfied();
            built.add(result);
        }
        return new Result(success, built,
                this.insights.stream().map(TypeBound.Result.Builder::build)
                        .filter(TypeBound.Result::satisfied)
                        .map(TypeBound.Result::bound).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public void iterate(TypeSystem system) {
        Set<TypeBound.Result.Builder> current = this.workingBounds;
        this.workingBounds = new LinkedHashSet<>();
        current.forEach(t -> this.solve(system, t));
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder) {
        TypeBound bound = builder.bound();
        if (this.satisfiedCache.contains(bound)) {
            builder.setSatisfied(true);
            return;
        }

        if (bound instanceof TypeBound.Equal eq) {
            this.solve(system, builder, eq);
        } else if (bound instanceof TypeBound.Subtype st) {
            this.solve(system, builder, st);
        } else if (bound instanceof TypeBound.Not nt) {
            this.solve(system, builder, nt);
        } else if (bound instanceof TypeBound.And and) {
            this.solve(system, builder, and);
        } else if (bound instanceof TypeBound.Or or) {
            this.solve(system, builder, or);
        } else {
            throw new IllegalStateException("Unsupported TypeBound: " + (bound == null ? null : bound.getClass().getName()));
        }

        this.cacheResults(builder);
    }

    private void cacheResults(TypeBound.Result.Builder builder) {
        TypeBound bound = builder.bound();
        if (!builder.children().isEmpty()) {
            builder.propagate();
        }

        if (builder.satisfied()) {
            this.satisfiedCache.add(bound);
        } else if (builder.originator() != null) {
            cacheResults(builder.originator());
        }
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.Subtype subtype) {
        insights.add(builder);

        Type left = subtype.left();
        Type right = subtype.right();
        if (left.hasCyclicTypeVariables() || right.hasCyclicTypeVariables()) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            if (left.hasCyclicTypeVariables()) {
                TypeBound.Result.builder(new TypeBound.NonCyclic(left), builder).setSatisfied(false);
            }

            if (right.hasCyclicTypeVariables()) {
                TypeBound.Result.builder(new TypeBound.NonCyclic(right), builder).setSatisfied(false);
            }
        } else if (left instanceof NoneType || right instanceof NoneType) {
            if (left instanceof NoneType lnt && right instanceof NoneType rnt) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Equal(lnt, system.constants().nullType())));
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Equal(rnt, system.constants().nullType())));
            } else if (left instanceof NoneType lnt) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Equal(lnt, system.constants().nullType())));
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(right, system.constants().object())));
            } else {
                builder.setSatisfied(false);
            }
        } else if (left.equals(right)) {
            builder.setSatisfied(true);
        } else if (left instanceof PrimitiveType || right instanceof PrimitiveType) {
            if (left instanceof PrimitiveType lpt && right instanceof PrimitiveType rpt) {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
                PRIM_SUPERS.get(lpt.name()).stream().map(s -> system.constants().primitivesByName().get(s))
                        .forEach(pspr -> this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Equal(rpt, pspr), builder)));
            } else if (left instanceof PrimitiveType lpt) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(system.constants().boxByPrimitive().get(lpt), right), builder));
            } else if (right instanceof PrimitiveType rpt) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(left, system.constants().boxByPrimitive().get(rpt)), builder));
            }
        } else if (left instanceof WildType) {
            if (left instanceof WildType.Upper wtu) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                wtu.upperBounds().forEach(wbound ->
                        this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(wbound, right), builder)));
            } else if (left instanceof WildType.Lower) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(system.constants().object(), right), builder));
            }
        } else if (right instanceof WildType) {
            if (right instanceof WildType.Lower wtl) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                wtl.lowerBounds().forEach(wbound ->
                        this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(left, wbound), builder)));
            } else if (right instanceof WildType.Upper) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(left, system.constants().nullType()), builder));
            }
        } else if (left instanceof VarType vt) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            vt.upperBounds().forEach(vbound ->
                    this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(vbound, right), builder)));
        } else if (right instanceof VarType vt) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            vt.upperBounds().forEach(vbound ->
                    this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(right, vbound), builder)));
        } else if (left instanceof ArrayType lat && right instanceof ArrayType rat) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Subtype(lat.component(), rat.component())));
        } else if (left instanceof ClassType lcls && right instanceof ClassType rcls) {
            if (!lcls.hasTypeArguments() && !rcls.hasTypeArguments()) {
                builder.setSatisfied(lcls.hasSupertype(rcls.classReference()));
            } else if ((!rcls.hasTypeArguments() && lcls.hasTypeArguments()) ||
                    (rcls.hasTypeArguments() && !lcls.hasTypeArguments())) {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                this.insights.add(TypeBound.Result.builder(new TypeBound.Unchecked(left, rcls), builder)
                        .setSatisfied(lcls.hasSupertype(rcls.classReference())));
            } else if (left instanceof ParameterizedClassType lpct && right instanceof ParameterizedClassType rpct) {
                Optional<ClassType> relativeOpt = lpct.relativeSupertype(rpct.classReference());
                TypeBound.Result.builder(new TypeBound.Subtype(lpct.classReference(), rpct.classReference()), builder)
                        .setSatisfied(relativeOpt.isPresent());

                if (relativeOpt.isPresent() && relativeOpt.get() instanceof ParameterizedClassType relative) {
                    if (relative.typeArguments().size() == rpct.typeArguments().size()) {
                        builder.setPropagation(TypeBound.Result.Propagation.AND);
                        for (int i = 0; i < relative.typeArguments().size(); i++) {
                            Type ti = relative.typeArguments().get(i);
                            Type si = rpct.typeArguments().get(i);

                            TypeBound.Result.Builder argMatch = TypeBound.Result.builder(new TypeBound.GenericParameter(ti, si), builder, TypeBound.Result.Propagation.AND);

                            if (si instanceof WildType.Upper siwtu) {
                                rpct.typeParameters().get(i).upperBounds().stream().map(rpct.varTypeResolver())
                                        .forEach(bound -> TypeBound.Result.builder(new TypeBound.Subtype(ti, bound), argMatch));
                                siwtu.upperBounds()
                                        .forEach(bound -> TypeBound.Result.builder(new TypeBound.Subtype(ti, bound), argMatch));
                                this.workingBounds.addAll(argMatch.children());
                            } else if (si instanceof WildType.Lower siwtl) {
                                rpct.typeParameters().get(i).upperBounds().stream().map(rpct.varTypeResolver())
                                        .forEach(bound -> TypeBound.Result.builder(new TypeBound.Subtype(ti, bound), argMatch));
                                siwtl.lowerBounds()
                                        .forEach(bound -> TypeBound.Result.builder(new TypeBound.Subtype(bound, ti), argMatch));
                                this.workingBounds.addAll(argMatch.children());
                            } else {
                                this.workingBounds.add(TypeBound.Result.builder(new TypeBound.Equal(ti, si), argMatch));
                            }
                        }
                    }
                }
            }
        }
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.Equal equal) {
        insights.add(builder);
        builder.setSatisfied(equal.left().equals(equal.right()));
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.NonCyclic nonCyclic) {
        insights.add(builder);
        builder.setSatisfied(nonCyclic.type().hasCyclicTypeVariables());
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.Not not) {
        builder.setPropagation(TypeBound.Result.Propagation.NAND);
        workingBounds.add(TypeBound.Result.builder(not.child(), builder));
    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.And and) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);
        and.children().forEach(c -> workingBounds.add(TypeBound.Result.builder(c, builder)));

    }

    private void solve(TypeSystem system, TypeBound.Result.Builder builder, TypeBound.Or or) {
        builder.setPropagation(TypeBound.Result.Propagation.OR);
        or.children().forEach(c -> workingBounds.add(TypeBound.Result.builder(c, builder)));
    }

}
