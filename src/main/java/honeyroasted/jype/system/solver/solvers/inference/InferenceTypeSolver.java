package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AbstractTypeSolver;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InferenceTypeSolver extends AbstractTypeSolver {
    private ExpressionResolver expressionResolver;

    public InferenceTypeSolver(ExpressionResolver expressionResolver) {
        super(Set.of(TypeBound.Equal.class,
                        TypeBound.Compatible.class, TypeBound.ExpressionCompatible.class,
                        TypeBound.Contains.class, TypeBound.LambdaThrows.class,
                        TypeBound.Throws.class, TypeBound.Captures.class,
                        TypeBound.Subtype.class));
        this.expressionResolver = expressionResolver;
    }

    private Set<TypeBound.Result.Builder> workingBounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> workingConstrains = new LinkedHashSet<>();
    private boolean terminate = false;
    private boolean success = true;

    @Override
    public Result solve(TypeSystem system) {
        return null;
    }

    private Set<TypeBound.Result.Builder> performIncorporation(Set<TypeBound.Result.Builder> bounds) {
        Set<TypeBound.Result.Builder> impliedConstraints = new HashSet<>();
        for (TypeBound.Result.Builder boundBuilder : bounds) {
            for (TypeBound.Result.Builder otherBuilder : bounds) {
                TypeBound bound = boundBuilder.bound();
                TypeBound other = otherBuilder.bound();

                if (bound instanceof TypeBound.Equal eq && hasMetaVarType(eq)) {
                    MetaVarType mvt = getMetaVarType(eq);
                    Type otherType = getOtherType(eq);
                    MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));
                    if (other != bound) {
                        if (other instanceof TypeBound.Equal otherEq) {
                            if (otherEq.left().equals(mvt)) {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherEq.right().equals(mvt)) {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.left()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(subResolver.visit(otherEq.left()), subResolver.visit(otherEq.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        } else if (other instanceof TypeBound.Subtype otherSub) {
                            if (otherSub.left().equals(mvt)) {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherType, otherSub.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherSub.right().equals(mvt)) {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherSub.left(), otherType), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subResolver.visit(otherSub.left()), subResolver.visit(otherSub.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        }
                    }
                } else if (bound instanceof TypeBound.Subtype st && st.left() instanceof MetaVarType mvt) {
                    if (other instanceof TypeBound.Subtype otherSub && mvt.equals(otherSub.left())) {
                        commonSupertypes(st.right(), otherSub.right()).forEach(pair -> {
                            if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                                for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                                    Type left = pair.left().typeArguments().get(i);
                                    Type right = pair.right().typeArguments().get(i);

                                    if (!(left instanceof WildType) && !(right instanceof WildType)) {
                                        impliedConstraints.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(pair.left().typeArguments().get(i), pair.right().typeArguments().get(i)),
                                                TypeBound.Result.Propagation.AND, boundBuilder, otherBuilder)));
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
        return impliedConstraints;
    }

    private List<Pair<ParameterizedClassType, ParameterizedClassType>> commonSupertypes(Type left, Type right) {
        List<Pair<ParameterizedClassType, ParameterizedClassType>> result = new ArrayList<>();

        Set<Type> leftSupers = allSupertypes(left);
        Set<Type> rightSupers = allSupertypes(right);

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

    private Set<Type> allSupertypes(Type type) {
        Set<Type> result = new LinkedHashSet<>();
        allSupertypes(result, type);
        return result;
    }

    private void allSupertypes(Set<Type> building, Type type) {
        if (!building.contains(type)) {
            building.add(type);
            if (type instanceof MetaVarType mvt) {
                this.workingBounds.forEach(t -> {
                    if (t.bound() instanceof TypeBound.Subtype st && st.left().equals(mvt)) {
                        building.add(st.right());
                        st.right().knownDirectSupertypes().forEach(k -> allSupertypes(building, k));
                    }
                });
            } else {
                type.knownDirectSupertypes().forEach(t -> allSupertypes(building, t));
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

    public static Set<TypeBound> buildInitialBounds(Map<VarType, MetaVarType> metaVars) {
        VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);

        Set<TypeBound> bounds = new LinkedHashSet<>();
        metaVars.forEach((vt, mvt) -> {
            if (vt.upperBounds().isEmpty()) {
                bounds.add(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object()));
            } else {
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    bounds.add(new TypeBound.Subtype(mvt, bound));
                }

                if (!foundProperUpper) {
                    bounds.add(new TypeBound.Subtype(mvt, vt.typeSystem().constants().object()));
                }
            }
        });
        return bounds;
    }

    public static void discoverVarTypes(Type visit, Map<VarType, MetaVarType> metaVars) {
        new RecursiveTypeVisitor<>((TypeVisitor.Default) (type, context) -> {
            if (type instanceof VarType vt && !metaVars.containsKey(vt)) {
                metaVars.put(vt, new MetaVarTypeImpl(vt.typeSystem(), System.identityHashCode(vt), vt.name()));
            }
            return null;
        }, null, false).visit(visit);
    }

}
