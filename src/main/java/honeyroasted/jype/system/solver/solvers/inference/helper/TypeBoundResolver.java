package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TypeBoundResolver extends AbstractInferenceHelper {
    private TypeBoundIncorporater boundIncorporater;

    public TypeBoundResolver() {
        this(TypeSolver.NO_OP);
    }

    public TypeBoundResolver(TypeSolver solver) {
        super(solver);
        this.boundIncorporater = new TypeBoundIncorporater(solver);
    }

    private Map<MetaVarType, Type> instantiations = new LinkedHashMap<>();
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();

    public void reset() {
        this.bounds.clear();
        this.instantiations.clear();
        this.boundIncorporater.reset();
    }

    public TypeBoundResolver resolve(Set<TypeBound.Result.Builder> bounds) {
        this.bounds.addAll(bounds);

        Set<TypeBound.Result.Builder> current;
        do {
            current = new HashSet<>(this.bounds);
            this.resolveOnce(current);
            this.boundIncorporater.reset().incorporate(current);
            this.bounds.addAll(this.boundIncorporater.bounds());
            this.constraints.addAll(this.boundIncorporater.constraints());
        } while (!this.bounds.equals(current) && this.bounds.stream().noneMatch(b -> b.bound().equals(TypeBound.False.INSTANCE)));
        return this;
    }

    public void resolveOnce(Set<TypeBound.Result.Builder> bounds) {

    }

    public Map<MetaVarType, Set<MetaVarType>> discoverDependencies(Set<TypeBound.Result.Builder> bounds) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = new LinkedHashMap<>();

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Binary<?, ?> bin && bin.left() instanceof Type lt && bin.right() instanceof Type rt) {
                initDependencies(lt, dependencies);
                initDependencies(rt, dependencies);
            } else if (bound instanceof TypeBound.Unary<?> un && un.value() instanceof Type t) {
                initDependencies(t, dependencies);
            }
        }

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Equal || bound instanceof TypeBound.Subtype) {
                TypeBound.Binary<Type, Type> bin = (TypeBound.Binary<Type, Type>) bound;
                MetaVarType mvt = getMetaVarType(bin);
                Type otherType = getOtherType(bin);

                boolean foundInCapture = false;
                for (TypeBound.Result.Builder otherBuilder : bounds) {
                    if (boundBuilder == otherBuilder) continue;

                    TypeBound other = otherBuilder.bound();
                    if (other instanceof TypeBound.Capture capture) {
                        if (discoverMetaVars(capture.left()).contains(mvt)) {
                            foundInCapture = true;
                            break;
                        }
                    }
                }

                MetaVarType alpha = mvt;
                if (foundInCapture) {
                    discoverMetaVars(otherType).forEach(beta -> dependencies.get(beta).add(alpha));
                } else {
                    discoverMetaVars(otherType).forEach(beta -> dependencies.get(alpha).add(beta));
                }
            } else if (bound instanceof TypeBound.Capture capture) {
                Set<MetaVarType> mvts = discoverMetaVars(capture.left());
                mvts.addAll(discoverMetaVars(capture.right()));
                mvts.forEach(mvt -> dependencies.get(mvt).addAll(mvts));
            }
        }

        Map<MetaVarType, Set<MetaVarType>> previous = dependencies;
        Map<MetaVarType, Set<MetaVarType>> current;
        do {
            current = new LinkedHashMap<>();

            Map<MetaVarType, Set<MetaVarType>> finalCurrent = current;
            Map<MetaVarType, Set<MetaVarType>> finalPrevious = previous;
            previous.forEach((mvt, deps) -> {
                Set<MetaVarType> newDeps = new LinkedHashSet<>(deps);
                deps.forEach(dep -> {
                    if (dep != mvt) {
                        newDeps.addAll(finalPrevious.get(dep));
                    }
                });
                finalCurrent.put(mvt, newDeps);
            });
            previous = current;
        } while (!current.equals(previous));

        return dependencies;
    }

    private static void initDependencies(Type visit, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        discoverMetaVars(visit).forEach(mv -> dependencies.computeIfAbsent(mv, k -> {
            Set<MetaVarType> deps = new LinkedHashSet<>();
            deps.add(k);
            return deps;
        }));
    }

    private static Set<MetaVarType> discoverMetaVars(Type visit) {
        return new HashSet<>(new RecursiveTypeVisitor<MetaVarType, Void>((TypeVisitor.Default) (type, context) -> type instanceof MetaVarType mvt ? mvt : null,
                null, false).visit(visit));
    }

    private static MetaVarType getMetaVarType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        if (bound.left() instanceof MetaVarType m) {
            return m;
        } else if (bound.right() instanceof MetaVarType m) {
            return m;
        } else {
            return null;
        }
    }

    private static boolean hasMetaVarType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        return getMetaVarType(bound) != null;
    }

    private static Type getOtherType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        if (bound.left() instanceof MetaVarType) {
            return bound.right();
        } else {
            return bound.left();
        }
    }

}
