package honeyroasted.jype.system.solver.solvers.incorporation;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IncorporationCapture implements UnaryTypeBoundMapper<TypeBound.Capture> {

    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.BOUND;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder boundBuilder, TypeBound.Capture capture) {
        context.defaultConsumer().accept(boundBuilder);

        //Bounds Involving Capture Conversion, 18.3.2
        ParameterizedClassType left = context.view(capture.left()); //G<alpha_1...alpha_n>
        ParameterizedClassType right = context.view(capture.right()); //G<A_1...A_n>
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
            Set<TypeBound.Result.Builder> initialBounds = context.system().operations().buildInitialBounds(varMap);
            initialBounds.forEach(b -> b.addParents(boundBuilder));
            addAll(context.bounds(), initialBounds);

            for (int i = 0; i < left.typeArguments().size() && i < right.typeArguments().size(); i++) {
                Type alphaType = left.typeArguments().get(i);
                Type a = right.typeArguments().get(i);
                VarType vi = right.typeParameters().get(i);
                Set<Type> bi = vi.upperBounds();

                if (alphaType instanceof MetaVarType alpha) {
                    if (a instanceof WildType) {
                        for (TypeBound.Result.Builder otherBuilder : context.currentBounds()) {
                            TypeBound other = otherBuilder.bound();
                            if (other instanceof TypeBound.Equal eq && ((context.view(eq.left()).typeEquals(alpha) && !(context.view(eq.right()) instanceof MetaVarType))
                                    || (context.view(eq.right()).typeEquals(alpha) && !(context.view(eq.left()) instanceof MetaVarType)))) {
                                //Case where Ai is a wildcard and alpha_i = R => false (18.3.2 Bullets #2.1, 3.1, 4.1)
                                context.bounds().accept(TypeBound.Result.builder(TypeBound.False.INSTANCE, boundBuilder, otherBuilder));
                            } else if (other instanceof TypeBound.Subtype st) {
                                if (context.view(st.left()).typeEquals(alpha) && !(context.view(st.right()) instanceof MetaVarType)) {
                                    //alpha <: R
                                    Type r = context.view(st.right());
                                    if (a instanceof WildType.Upper wtu) {
                                        if (wtu.hasDefaultBounds()) { //?
                                            //Case where A_i is a wildcard of form ? and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #2.2, 3.3)
                                            bi.forEach(bii -> context.constraints().accept(TypeBound.Result.builder(
                                                    new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder)));
                                        } else {
                                            if (vi.hasDefaultBounds()) {
                                                //Case where A_i is a wildcard of form ? extends T, alpha_i <: R, and B_i is Object => T <: R (18.3.2 Bullets #3.2)
                                                wtu.upperBounds().forEach(ti -> context.constraints().accept(TypeBound.Result.builder(
                                                        new TypeBound.Subtype(ti, r), boundBuilder, otherBuilder)));
                                            }
                                        }
                                    } else if (a instanceof WildType.Lower wtl) { //? super
                                        //Case where A_i is a wildcard of form ? super T and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #4.2)
                                        bi.forEach(bii -> context.constraints().accept(TypeBound.Result.builder(
                                                new TypeBound.Subtype(theta.apply(bii), r), boundBuilder, otherBuilder)));
                                    }
                                } else if (context.view(st.right()).typeEquals(alpha) && !(context.view(st.left()) instanceof MetaVarType)) {
                                    //r <: alpha
                                    Type r = context.view(st.left());
                                    if (a instanceof WildType.Upper wtu) {
                                        //Case where A_i is a wildcard of form ? or ? extends T and R <: alpha_i => false (18.3.2 Bullets #2.3, 3.4)
                                        context.bounds().accept(TypeBound.Result.builder(
                                                TypeBound.False.INSTANCE, boundBuilder, otherBuilder));
                                    } else if (a instanceof WildType.Lower wtl) { //? super
                                        //Case where A_I is a wildcard of form ? super T and R <: alpha_i => R <: T (18.3.2 Bullet #4.3)
                                        wtl.lowerBounds().forEach(ti -> context.constraints().accept(TypeBound.Result.builder(
                                                new TypeBound.Subtype(r, ti), boundBuilder, otherBuilder)));
                                    }
                                }
                            }
                        }
                    } else {
                        //Case where A_i is not a wildcard => alpha_i = A_i (18.3.2 Bullets #1)
                        context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(alpha, a), boundBuilder));
                    }
                }
            }
        }
    }
}
