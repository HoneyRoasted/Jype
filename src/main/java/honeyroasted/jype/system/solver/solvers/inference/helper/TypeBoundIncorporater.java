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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Implements incorporation as defined in 18.3
public class TypeBoundIncorporater extends AbstractInferenceHelper {
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();

    private TypeInitialBoundBuilder initialBoundBuilder;
    private TypeSetOperations setOperations;

    public TypeBoundIncorporater(TypeSolver solver) {
        super(solver);
        this.initialBoundBuilder = new TypeInitialBoundBuilder(solver);
        this.setOperations = new TypeSetOperations(solver);
    }

    public TypeBoundIncorporater() {
        this(TypeSolver.NO_OP);
    }

    public TypeBoundIncorporater reset() {
        this.bounds.clear();
        this.constraints.clear();
        this.initialBoundBuilder.reset();

        return this;
    }

    public Set<TypeBound.Result.Builder> bounds() {
        return this.bounds;
    }

    public Set<TypeBound.Result.Builder> constraints() {
        return this.constraints;
    }

    public TypeBoundIncorporater incorporate(Set<TypeBound.Result.Builder> bounds) {
        this.bounds.addAll(bounds);

        Set<TypeBound.Result.Builder> current;
        do {
            current = new HashSet<>(this.bounds);
            this.incorporateOnce(current);
            this.setOperations.updateMetaVars(this.bounds);
        } while (!this.bounds.equals(current) && this.bounds.stream().noneMatch(b -> b.bound().equals(TypeBound.False.INSTANCE)));
        return this;
    }

    public void incorporateOnce(Set<TypeBound.Result.Builder> bounds) {
        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            for (TypeBound.Result.Builder otherBuilder : bounds) {
                if (boundBuilder == otherBuilder) continue;

                //Complementary bounds, 18.3.1
                TypeBound other = otherBuilder.bound();

                if (bound instanceof TypeBound.Equal eq && eq.hasMetaVar()) {
                    //case where alpha = S
                    MetaVarType mvt = eq.getMetaVar().orElse(null);
                    Type otherType = eq.getOtherType().orElse(null);
                    MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));
                    if (other != bound) {
                        if (other instanceof TypeBound.Equal otherEq) {
                            if (otherEq.left().typeEquals(mvt)) {
                                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherEq.right().typeEquals(mvt)) {
                                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(otherType, otherEq.left()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                //Case where alpha = U and S = T => S[alpha=U] = T[alpha=U] (18.3.1, Bullet #5)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(subResolver.visit(otherEq.left()), subResolver.visit(otherEq.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        } else if (other instanceof TypeBound.Subtype otherSub) {
                            if (otherSub.left().typeEquals(mvt)) {
                                //Case where alpha = S and alpha <: T => S <: T (18.3.1, Bullet #2)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherType, otherSub.right()), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else if (otherSub.right().typeEquals(mvt)) {
                                //Case where alpha = S and T <: alpha => T <: S (18.3.1, Bullet #3)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(otherSub.left(), otherType), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            } else {
                                //Case where alpha = U and S <: T => S[alpha = U] <: T[alpha = U] (18.3.1, Bullet #6)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subResolver.visit(otherSub.left()), subResolver.visit(otherSub.right())), TypeBound.Result.Propagation.AND,
                                        boundBuilder, otherBuilder)));
                            }
                        }
                    }
                } else if (bound instanceof TypeBound.Subtype st && other instanceof TypeBound.Subtype otherSub) {
                    if (st.left() instanceof MetaVarType mvt && mvt.typeEquals(otherSub.right())) {
                        //Case where S <: alpha and alpha <: T => S <: T (18.3.1, Bullet #4)
                        this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(st.right(), otherSub.left()), boundBuilder, otherBuilder)));
                    }

                    if (st.left() instanceof MetaVarType mvt && mvt.typeEquals(otherSub.left())) {
                        //Case where alpha <: T and alpha <: S and generic supertype G of T and S exists => generic parameters
                        // that aren't wildcards are equal (18.3.1, Last Paragraph)
                        commonSupertypes(st.right(), otherSub.right(), bounds).forEach(pair -> {
                            if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                                for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                                    Type left = pair.left().typeArguments().get(i);
                                    Type right = pair.right().typeArguments().get(i);

                                    if (!(left instanceof WildType) && !(right instanceof WildType)) {
                                        this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(pair.left().typeArguments().get(i), pair.right().typeArguments().get(i)),
                                                TypeBound.Result.Propagation.AND, boundBuilder, otherBuilder)));
                                    }
                                }
                            }
                        });
                    }
                }
            }

            if (bound instanceof TypeBound.Capture capture) {
                //Bounds Involving Capture Conversion, 18.3.2
                ParameterizedClassType left = capture.left(); //G<alpha_1...alpha_n>
                ParameterizedClassType right = capture.right(); //G<A_1...A_n>
                //P_l = parameter l of G
                //B_l = bound of P_l

                if (left.classReference().typeEquals(right.classReference()) &&
                        left.typeArguments().size() == right.typeArguments().size()) { //Sanity check, should be true by convention
                    Map<VarType, MetaVarType> varMap = new LinkedHashMap<>(); //Substitution P_l = alpha_i
                    for (int i = 0; i < left.typeParameters().size() && i < left.typeArguments().size(); i++) {
                        Type arg = left.typeArguments().get(i);
                        if (arg instanceof MetaVarType mvt) { //Sanity check, should be true by convention
                            varMap.put(left.typeParameters().get(i), mvt);
                        }
                    }

                    VarTypeResolveVisitor theta = left.varTypeResolver(); //theta = [P_1 = alpha_1...P_n = alpha_n]

                    //Initial bounds generated from G<P_1...P_n> per 18.1.3
                    this.initialBoundBuilder.reset();
                    Set<TypeBound.Result.Builder> initialBounds = this.initialBoundBuilder.buildInitialBounds(varMap).bounds();
                    initialBounds.forEach(b -> b.addParents(boundBuilder));
                    this.bounds.addAll(initialBounds);

                    for (int i = 0; i < left.typeArguments().size() && i < right.typeArguments().size(); i++) {
                        Type alphaType = left.typeArguments().get(i);
                        Type a = right.typeArguments().get(i);
                        VarType vi = right.typeParameters().get(i);
                        Set<Type> bi = vi.upperBounds();

                        if (alphaType instanceof MetaVarType alpha) {
                            if (a instanceof WildType) {
                                for (TypeBound.Result.Builder otherBuilder : bounds) {
                                    TypeBound other = otherBuilder.bound();
                                    if (other instanceof TypeBound.Equal eq && ((eq.left().typeEquals(alpha) && !(eq.right() instanceof MetaVarType))
                                            || (eq.right().typeEquals(alpha) && !(eq.left() instanceof MetaVarType)))) {
                                        //Case where Ai is a wildcard and alpha_i = R => false (18.3.2 Bullets #2.1, 3.1, 4.1)
                                        this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(TypeBound.False.INSTANCE, boundBuilder, otherBuilder)));
                                    } else if (other instanceof TypeBound.Subtype st) {
                                        if (st.left().typeEquals(alpha) && !(st.right() instanceof MetaVarType)) {
                                            //alpha <: R
                                            Type r = st.right();
                                            if (a instanceof WildType.Upper wtu) {
                                                if (wtu.hasDefaultBounds()) { //?
                                                    //Case where A_i is a wildcard of form ? and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #2.2, 3.3)
                                                    bi.forEach(bii -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                            new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder))));
                                                } else {
                                                    if (vi.hasDefaultBounds()) {
                                                        //Case where A_i is a wildcard of form ? extends T, alpha_i <: R, and B_i is Object => T <: R (18.3.2 Bullets #3.2)
                                                        wtu.upperBounds().forEach(ti -> this.constraints.add(this.eventBoundCreated(
                                                                TypeBound.Result.builder(new TypeBound.Subtype(ti, r), boundBuilder, otherBuilder))));
                                                    }
                                                }
                                            } else if (a instanceof WildType.Lower wtl) { //? super
                                                //Case where A_i is a wildcard of form ? super T and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #4.2)
                                                bi.forEach(bii -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder))));
                                            }
                                        } else if (st.right().typeEquals(alpha) && !(st.left() instanceof MetaVarType)) {
                                            //r <: alpha
                                            Type r = st.left();
                                            if (a instanceof WildType.Upper wtu) {
                                                //Case where A_i is a wildcard of form ? or ? extends T and R <: alpha_i => false (18.3.2 Bullets #2.3, 3.4)
                                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        TypeBound.False.INSTANCE, boundBuilder, otherBuilder)));
                                            } else if (a instanceof WildType.Lower wtl) { //? super
                                                //Case where A_I is a wildcard of form ? super T and R <: alpha_i => R <: T (18.3.2 Bullet #4.3)
                                                wtl.lowerBounds().forEach(ti -> this.constraints.add(this.eventBoundCreated(TypeBound.Result.builder(
                                                        new TypeBound.Subtype(r, ti), boundBuilder, otherBuilder))));
                                            }
                                        }
                                    }
                                }
                            } else {
                                //Case where A_i is not a wildcard => alpha_i = A_i (18.3.2 Bullets #1)
                                this.bounds.add(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(alpha, a), boundBuilder)));
                            }
                        }
                    }
                }
            }
        }
    }

    //Generates all supertypes of left and right which share a class type. Do not need to share type arguments.
    private List<Pair<ParameterizedClassType, ParameterizedClassType>> commonSupertypes(Type left, Type right, Set<? extends TypeBound.ResultView> bounds) {
        List<Pair<ParameterizedClassType, ParameterizedClassType>> result = new ArrayList<>();

        Set<Type> leftSupers = this.setOperations.allKnownSupertypes(left);
        Set<Type> rightSupers = this.setOperations.allKnownSupertypes(right);

        for (Type leftSuper : leftSupers) {
            if (leftSuper instanceof ParameterizedClassType lct) {
                for (Type rightSuper : rightSupers) {
                    if (rightSuper instanceof ParameterizedClassType rct && lct.classReference().typeEquals(rct.classReference())) {
                        result.add(Pair.of(lct, rct));
                    }
                }
            }
        }

        return result;
    }

}
