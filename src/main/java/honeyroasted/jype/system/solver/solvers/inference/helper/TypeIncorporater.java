package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.inference.MetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Implements incorporation per Java spec 18.3
public class TypeIncorporater extends AbstractInferenceHelper {
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();

    private InitialBoundBuilder initialBoundBuilder;

    public TypeIncorporater(TypeSolver solver) {
        super(solver);
        this.initialBoundBuilder = new InitialBoundBuilder(solver);
    }


    public void reset() {
        this.bounds.clear();
        this.constraints.clear();
    }

    public Set<TypeBound.Result.Builder> bounds() {
        return this.bounds;
    }

    public Set<TypeBound.Result.Builder> constraints() {
        return this.constraints;
    }

    public TypeIncorporater setBounds(Set<TypeBound.Result.Builder> bounds) {
        this.bounds = bounds;
        return this;
    }

    public TypeIncorporater setConstraints(Set<TypeBound.Result.Builder> constraints) {
        this.constraints = constraints;
        return this;
    }

    public void incorporate(Set<TypeBound.Result.Builder> bounds) {
        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            for (TypeBound.Result.Builder otherBuilder : bounds) {
                TypeBound other = otherBuilder.bound();

                if (bound instanceof TypeBound.Equal eq && hasMetaVarType(eq)) {
                    MetaVarType mvt = getMetaVarType(eq);
                    Type otherType = getOtherType(eq);
                    MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));
                    if (other != bound) {
                        if (other instanceof TypeBound.Equal otherEq) {
                            if (otherEq.left().equals(mvt)) {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherEq.right().equals(mvt)) {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.left()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(subResolver.visit(otherEq.left()), subResolver.visit(otherEq.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        } else if (other instanceof TypeBound.Subtype otherSub) {
                            if (otherSub.left().equals(mvt)) {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherType, otherSub.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherSub.right().equals(mvt)) {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherSub.left(), otherType), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subResolver.visit(otherSub.left()), subResolver.visit(otherSub.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        }
                    }
                } else if (bound instanceof TypeBound.Subtype st && st.left() instanceof MetaVarType mvt) {
                    if (other instanceof TypeBound.Subtype otherSub && mvt.equals(otherSub.left())) {
                        commonSupertypes(st.right(), otherSub.right(), bounds).forEach(pair -> {
                            if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                                for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                                    Type left = pair.left().typeArguments().get(i);
                                    Type right = pair.right().typeArguments().get(i);

                                    if (!(left instanceof WildType) && !(right instanceof WildType)) {
                                        bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(pair.left().typeArguments().get(i), pair.right().typeArguments().get(i)),
                                                TypeBound.Result.Propagation.AND, boundBuilder, otherBuilder)));
                                    }
                                }
                            }
                        });
                    }
                }
            }

            if (bound instanceof TypeBound.Capture capture) {
                //18.3.2
                ParameterizedClassType left = capture.left();
                ParameterizedClassType right = capture.right();
                if (left.classReference().equals(right.classReference()) &&
                        left.typeArguments().size() == right.typeArguments().size()) { //Sanity check
                    Map<VarType, MetaVarType> varMap = new LinkedHashMap<>();
                    for (int i = 0; i < left.typeParameters().size() && i < left.typeArguments().size(); i++) {
                        Type arg = left.typeArguments().get(i);
                        if (arg instanceof MetaVarType mvt) {
                            varMap.put(left.typeParameters().get(i), mvt);
                        }
                    }

                    VarTypeResolveVisitor theta = left.varTypeResolver();

                    this.initialBoundBuilder.reset();
                    this.initialBoundBuilder.buildInitialBounds(varMap);
                    Set<TypeBound.Result.Builder> initialBounds = this.initialBoundBuilder.bounds();
                    initialBounds.forEach(b -> b.addParents(boundBuilder));

                    for (int i = 0; i < left.typeArguments().size() && i < right.typeArguments().size(); i++) {
                        Type alphaType = left.typeArguments().get(i);
                        Type a = right.typeArguments().get(i);
                        VarType vi = right.typeParameters().get(i);
                        Set<Type> bi = vi.upperBounds();

                        if (alphaType instanceof MetaVarType alpha) {
                            if (a instanceof WildType) {
                                for (TypeBound.Result.Builder otherBuilder : bounds) {
                                    TypeBound other = otherBuilder.bound();
                                    if (other instanceof TypeBound.Equal eq && ((eq.left().equals(alpha) && !(eq.right() instanceof MetaVarType))
                                            || (eq.right().equals(alpha) && !(eq.left() instanceof MetaVarType)))) {
                                        //alpha = R
                                        this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.False(), boundBuilder, otherBuilder)));
                                    } else if (other instanceof TypeBound.Subtype st) {
                                        if (st.left().equals(alpha) && !(st.right() instanceof MetaVarType)) {
                                            //alpha <: R
                                            Type r = st.right();
                                            if (a instanceof WildType.Upper wtu) {
                                                if (wtu.hasDefaultBounds()) { //?
                                                    bi.forEach(bii -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                            new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder))));
                                                } else { //? extends
                                                    if (vi.hasDefaultBounds()) {
                                                        wtu.upperBounds().forEach(ti -> this.constraints.add(this.eventBoundCreated(
                                                                TypeBound.Result.builder(new TypeBound.Subtype(ti, r), boundBuilder, otherBuilder))));
                                                    }
                                                }
                                            } else if (a instanceof WildType.Lower wtl) { //? super
                                                bi.forEach(bii -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder))));
                                            }
                                        } else if (st.right().equals(alpha) && !(st.left() instanceof MetaVarType)) {
                                            //r <: alpha
                                            Type r = st.left();
                                            if (a instanceof WildType.Upper wtu) {
                                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        new TypeBound.False(), boundBuilder, otherBuilder)));
                                            } else if (a instanceof WildType.Lower wtl) { //? super
                                                wtl.lowerBounds().forEach(ti -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        new TypeBound.Subtype(r, ti), boundBuilder, otherBuilder))));
                                            }
                                        }
                                    }
                                }
                            } else {
                                initialBounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(alpha, a), boundBuilder)));
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<Pair<ParameterizedClassType, ParameterizedClassType>> commonSupertypes(Type left, Type right, Set<? extends TypeBound.ResultView> bounds) {
        List<Pair<ParameterizedClassType, ParameterizedClassType>> result = new ArrayList<>();

        Set<Type> leftSupers = allSupertypes(left, bounds);
        Set<Type> rightSupers = allSupertypes(right, bounds);

        for (Type leftSuper : leftSupers) {
            if (leftSuper instanceof ParameterizedClassType lct) {
                for (Type rightSuper : rightSupers) {
                    if (rightSuper instanceof ParameterizedClassType rct && lct.classReference().equals(rct.classReference())) {
                        result.add(Pair.of(lct, rct));
                    }
                }
            }
        }

        return result;
    }

    private static Set<Type> allSupertypes(Type type, Set<? extends TypeBound.ResultView> bounds) {
        Set<Type> result = new LinkedHashSet<>();
        allSupertypes(result, type, bounds);
        return result;
    }

    private static void allSupertypes(Set<Type> building, Type type, Set<? extends TypeBound.ResultView> bounds) {
        if (!building.contains(type)) {
            building.add(type);
            if (type instanceof MetaVarType mvt) {
                bounds.forEach(t -> {
                    if (t.bound() instanceof TypeBound.Subtype st && st.left().equals(mvt)) {
                        building.add(st.right());
                        st.right().knownDirectSupertypes().forEach(k -> allSupertypes(building, k, bounds));
                    }
                });
            } else {
                type.knownDirectSupertypes().forEach(t -> allSupertypes(building, t, bounds));
            }
        }
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
