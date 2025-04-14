package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.LinkedHashSet;
import java.util.Set;

public class JIncorporationCapture extends ConstraintMapper.Unary<JTypeConstraints.Capture> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Capture constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Capture constraint, Constraint.Status status) {
        JType leftMapped = constraint.left();
        JType rightMapped = constraint.right();

        //Bounds Involving Capture Conversion, 18.3.2
        JParameterizedClassType left = (JParameterizedClassType) leftMapped; //G<alpha_1...alpha_n>
        JParameterizedClassType right = (JParameterizedClassType) rightMapped; //G<A_1...A_n>
        //P_l = parameter l of G
        //B_l = classBound of P_l

        if (left.classReference().typeEquals(right.classReference()) &&
                left.typeArguments().size() == right.typeArguments().size()) { //Sanity check, should be true by convention
            Set<Constraint> varMap = new LinkedHashSet<>(); //Substitution P_l = alpha_i
            for (int i = 0; i < left.typeParameters().size() && i < left.typeArguments().size(); i++) {
                JType arg = left.typeArguments().get(i);
                if (arg instanceof JMetaVarType mvt) { //Sanity check, should be true by convention
                    JVarType varType = left.typeParameters().get(i);
                    varMap.add(new JTypeConstraints.Infer(mvt, varType));

                    branchContext.firstOrAttach(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.createEmpty())
                            .metaVars().put(varType, mvt);
                }
            }

            JVarTypeResolveVisitor theta = left.varTypeResolver(); //theta = [P_1 = alpha_1...P_n = alpha_n]

            //Initial interfaceBounds generated from G<P_1...P_n> per 18.1.3
            varMap.forEach(con -> branch.add(con, Constraint.Status.ASSUMED));

            for (int i = 0; i < left.typeArguments().size() && i < right.typeArguments().size(); i++) {
                JType alphaType = left.typeArguments().get(i);
                JType a = right.typeArguments().get(i);
                JVarType vi = right.typeParameters().get(i);
                Set<JType> bi = vi.upperBounds();

                if (alphaType instanceof JMetaVarType alpha) {
                    if (a instanceof JWildType) {
                        branch.constraints().forEach((other, otherStatus) -> {
                            if (constraint != other) {
                                if (other instanceof JTypeConstraints.Equal eq && ((eq.left().typeEquals(alpha) && !(eq.right() instanceof JMetaVarType))
                                        || (eq.right().typeEquals(alpha) && !(eq.left() instanceof JMetaVarType)))) {
                                    //Case where Ai is a wildcard and alpha_i = R => false (18.3.2 Bullets #2.1, 3.1, 4.1)
                                    branch.add(new JTypeConstraints.Contradiction(constraint, other), Constraint.Status.FALSE);
                                } else if (other instanceof JTypeConstraints.Subtype st) {
                                    if (st.left().typeEquals(alpha) && !(st.right() instanceof JMetaVarType)) {
                                        //alpha <: R
                                        JType r = st.right();
                                        if (a instanceof JWildType.Upper wtu) {
                                            if (wtu.hasDefaultBounds()) { //?
                                                //Case where A_i is a wildcard of form ? and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #2.2, 3.3)
                                                bi.forEach(bii -> branch.add(new JTypeConstraints.Subtype(theta.apply(bii), r)));
                                            } else {
                                                if (vi.hasDefaultBounds()) {
                                                    //Case where A_i is a wildcard of form ? extends T, alpha_i <: R, and B_i is Object => T <: R (18.3.2 Bullets #3.2)
                                                    wtu.upperBounds().forEach(ti -> branch.add(new JTypeConstraints.Subtype(ti, r)));
                                                }
                                            }
                                        } else if (a instanceof JWildType.Lower wtl) { //? super
                                            //Case where A_i is a wildcard of form ? super T and alpha_i <: R => B_i[theta] <: R (18.3.2 Bullets #4.2)
                                            bi.forEach(bii -> branch.add(new JTypeConstraints.Subtype(theta.apply(bii), r)));
                                        }
                                    } else if (st.right().typeEquals(alpha) && !(st.left() instanceof JMetaVarType)) {
                                        //r <: alpha
                                        JType r = st.left();
                                        if (a instanceof JWildType.Upper wtu) {
                                            //Case where A_i is a wildcard of form ? or ? extends T and R <: alpha_i => false (18.3.2 Bullets #2.3, 3.4)
                                            branch.add(new JTypeConstraints.Contradiction(constraint, other), Constraint.Status.FALSE);
                                        } else if (a instanceof JWildType.Lower wtl) { //? super
                                            //Case where A_I is a wildcard of form ? super T and R <: alpha_i => R <: T (18.3.2 Bullet #4.3)
                                            wtl.lowerBounds().forEach(ti -> branch.add(new JTypeConstraints.Subtype(r, ti)));
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        //Case where A_i is not a wildcard => alpha_i = A_i (18.3.2 Bullets #1)
                        addEqualBound(branch, alpha, a);
                    }
                }
            }
        }
    }

    private static void addEqualBound(ConstraintBranch branch, JType left, JType right) {
        if (left.isProperType() && right.isProperType()) {
            boolean equal = left.typeEquals(right);
            if (equal) {
                branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Equal(left, right), Constraint.Status.FALSE);
            }
        } else {
            branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.ASSUMED);
        }
    }
}
