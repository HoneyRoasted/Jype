package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class IncorporationCapture extends ConstraintMapper.Unary<TypeConstraints.Capture> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Capture constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Capture constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type leftMapped = mapper.apply(constraint.left());
        Type rightMapped = mapper.apply(constraint.right());

        //Bounds Involving Capture Conversion, 18.3.2
        ParameterizedClassType left = (ParameterizedClassType) leftMapped; //G<alpha_1...alpha_n>
        ParameterizedClassType right = (ParameterizedClassType) rightMapped; //G<A_1...A_n>
        //P_l = parameter l of G
        //B_l = bound of P_l

        if (left.classReference().typeEquals(right.classReference()) &&
                left.typeArguments().size() == right.typeArguments().size()) { //Sanity check, should be true by convention
            Set<Constraint> varMap = new LinkedHashSet<>(); //Substitution P_l = alpha_i
            for (int i = 0; i < left.typeParameters().size() && i < left.typeArguments().size(); i++) {
                Type arg = left.typeArguments().get(i);
                if (arg instanceof MetaVarType mvt) { //Sanity check, should be true by convention
                    varMap.add(new TypeConstraints.Infer(mvt, left.typeParameters().get(i)));
                }
            }

            VarTypeResolveVisitor theta = left.varTypeResolver(); //theta = [P_1 = alpha_1...P_n = alpha_n]

            //Initial bounds generated from G<P_1...P_n> per 18.1.3
            varMap.forEach(con -> branch.add(con, Constraint.Status.ASSUMED));
            left.typeSystem().operations().initialBoundsApplier().accept(branch);

            for (int i = 0; i < left.typeArguments().size() && i < right.typeArguments().size(); i++) {
                Type alphaType = left.typeArguments().get(i);
                Type a = right.typeArguments().get(i);
                VarType vi = right.typeParameters().get(i);
                Set<Type> bi = vi.upperBounds();

                if (alphaType instanceof MetaVarType alpha) {
                    if (a instanceof WildType) {
                        branch.constraints().forEach((other, otherStatus) -> {
                            if (constraint != other) {
                                if (other instanceof TypeConstraints.Equal eq && ((eq.left().typeEquals(alpha) && !(eq.right() instanceof MetaVarType))
                                        || (eq.right().typeEquals(alpha) && !(eq.left() instanceof MetaVarType)))) {
                                    //Case where Ai is a wildcard and alpha_i = R => false (18.3.2 Bullets #2.1, 3.1, 4.1)
                                    branch.setStatus(constraint, Constraint.Status.FALSE);
                                    branch.setStatus(other, Constraint.Status.FALSE);
                                } else if (other instanceof TypeConstraints.Subtype st) {
                                    if (st.left().typeEquals(alpha) && !(st.right() instanceof MetaVarType)) {
                                        //alpha <: R
                                        Type r = st.right();
                                        if (a instanceof WildType.Upper wtu) {
                                            if (wtu.hasDefaultBounds()) { //?
                                                //Case where A_i is a wildcard of form ? and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #2.2, 3.3)
                                                bi.forEach(bii -> branch.add(new TypeConstraints.Subtype(theta.apply(bii), r)));
                                            } else {
                                                if (vi.hasDefaultBounds()) {
                                                    //Case where A_i is a wildcard of form ? extends T, alpha_i <: R, and B_i is Object => T <: R (18.3.2 Bullets #3.2)
                                                    wtu.upperBounds().forEach(ti -> branch.add(new TypeConstraints.Subtype(ti, r)));
                                                }
                                            }
                                        } else if (a instanceof WildType.Lower wtl) { //? super
                                            //Case where A_i is a wildcard of form ? super T and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #4.2)
                                            bi.forEach(bii -> branch.add(new TypeConstraints.Subtype(theta.apply(bii), r)));
                                        }
                                    } else if (st.right().typeEquals(alpha) && !(st.left() instanceof MetaVarType)) {
                                        //r <: alpha
                                        Type r = st.left();
                                        if (a instanceof WildType.Upper wtu) {
                                            //Case where A_i is a wildcard of form ? or ? extends T and R <: alpha_i => false (18.3.2 Bullets #2.3, 3.4)
                                            branch.setStatus(constraint, Constraint.Status.FALSE);
                                            branch.setStatus(other, Constraint.Status.FALSE);
                                        } else if (a instanceof WildType.Lower wtl) { //? super
                                            //Case where A_I is a wildcard of form ? super T and R <: alpha_i => R <: T (18.3.2 Bullet #4.3)
                                            wtl.lowerBounds().forEach(ti -> branch.add(new TypeConstraints.Subtype(r, ti)));
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        //Case where A_i is not a wildcard => alpha_i = A_i (18.3.2 Bullets #1)
                        branch.add(new TypeConstraints.Equal(alpha, a), Constraint.Status.ASSUMED);
                    }
                }
            }
        }
    }
}
