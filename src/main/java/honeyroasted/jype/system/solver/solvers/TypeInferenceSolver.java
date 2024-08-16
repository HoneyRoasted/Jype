package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TypeInferenceSolver extends AbstractTypeSolver {
    private boolean applyCompatibility;

    public TypeInferenceSolver() {
        this(true);
    }

    public TypeInferenceSolver(boolean applyCompatibility) {
        super(Set.of(TypeBound.Infer.class,
                TypeBound.Replaced.class,

                TypeBound.Equal.class,
                TypeBound.Compatible.class,
                TypeBound.ExpressionCompatible.class,
                TypeBound.Subtype.class,

                TypeBound.Contains.class,
                TypeBound.LambdaThrows.class));
        this.applyCompatibility = applyCompatibility;
    }

    @Override
    public Result solve(TypeSystem system) {
        List<TypeBound.Result.Builder> compatibility = new ArrayList<>();

        List<TypeBound.Result.Builder> constraints = new ArrayList<>();
        List<TypeBound.Result.Builder> bounds = new ArrayList<>();

        Map<VarType, MetaVarType> metas = new HashMap<>();
        Map<MetaVarType, VarType> metasReversed = new HashMap<>();
        this.constraints.forEach(tb -> {
            if (tb instanceof TypeBound.Infer inf) {
                if (inf.value() instanceof VarType vt && !metas.containsKey(vt)) {
                    MetaVarType mvt = system.typeFactory().newMetaVarType(System.identityHashCode(vt), vt.name());
                    metas.put(vt, mvt);
                    metasReversed.put(mvt, vt);
                }
            } else if (tb instanceof TypeBound.Replaced rep) {
                if (rep.left() instanceof MetaVarType mvt && rep.right() instanceof VarType vt) {
                    metas.put(vt, mvt);
                    metasReversed.put(mvt, vt);
                } else if (rep.left() instanceof VarType vt && rep.right() instanceof MetaVarType mvt) {
                    metas.put(vt, mvt);
                    metasReversed.put(mvt, vt);
                }
            } else {
                constraints.add(TypeBound.Result.builder(tb));
                compatibility.add(TypeBound.Result.builder(tb));
            }
        });

        VarTypeResolveVisitor varTypeResolveVisitor = new VarTypeResolveVisitor(metas);
        TypeCache<Type, Type> varTypeCache = new InMemoryTypeCache<>();

        List<Function<Type, Type>> reductionTypeModifiers = List.of((type) -> varTypeResolveVisitor.visit(type, varTypeCache));
        bounds.addAll(system.operations().buildInitialBounds(metas));


        Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> reduced = system.operations().reductionApplier().process(system, reductionTypeModifiers, bounds, constraints);
        Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>> resolution = system.operations().resolveBounds(new LinkedHashSet<>(reduced.left()));

        Map<MetaVarType, Type> resolutions = new HashMap<>(resolution.left());

        Set<TypeBound.Result.Builder> results = new LinkedHashSet<>();

        resolutions.forEach((mvt, type) -> {
            results.add(TypeBound.Result.builder(new TypeBound.Instantiation(mvt, type)).setSatisfied(true));

            VarType vt = metasReversed.get(mvt);
            if (vt != null) {
                results.add(TypeBound.Result.builder(new TypeBound.Instantiation(vt, type)).setSatisfied(true));
            }
        });

        if (applyCompatibility) {
            MetaVarTypeResolver metaVarTypeResolver = new MetaVarTypeResolver(resolutions);
            TypeCache<Type, Type> metaVarTypeCache = new InMemoryTypeCache<>();
            List<Function<Type, Type>> compatTypeModifiers = List.of(
                    (type) -> varTypeResolveVisitor.visit(type, varTypeCache),
                    (type) -> metaVarTypeResolver.visit(type, metaVarTypeCache));
            system.operations().compatibilityApplier().process(system, compatTypeModifiers, new ArrayList<>(), compatibility);
            results.addAll(compatibility);
        } else {
            results.addAll(resolution.right());
        }

        return Result.build(results);
    }
}
