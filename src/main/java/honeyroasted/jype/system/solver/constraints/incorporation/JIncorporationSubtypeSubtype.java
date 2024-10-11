package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JIncorporationSubtypeSubtype extends ConstraintMapper.Binary<JTypeConstraints.Subtype, JTypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype leftConstraint, Constraint.Status leftStatus, JTypeConstraints.Subtype rightConstraint, Constraint.Status rightStatus) {
        JType ll = leftConstraint.left(), lr = leftConstraint.right(),
                rl = rightConstraint.left(), rr = rightConstraint.right();

        if (ll instanceof JMetaVarType mvt && mvt.typeEquals(rr)) {
            //Case where S <: alpha and alpha <: T => S <: T (18.3.1, Bullet #4)
            addSubtypeBound(branch, leftConstraint, lr, rightConstraint, rl);
        }

        if (ll instanceof JMetaVarType mvt && mvt.typeEquals(rl)) {
            //Case where alpha <: T and alpha <: S and generic supertype G of T and S exists => generic parameters
            // that aren't wildcards are equal (18.3.1, Last Paragraph)
            commonSupertypes(lr, rr).forEach(pair -> {
                if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                    for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                        JType left = pair.left().typeArguments().get(i);
                        JType right = pair.right().typeArguments().get(i);

                        if (!(left instanceof JWildType) && !(right instanceof JWildType)) {
                            addEqualBound(branch, leftConstraint, left, rightConstraint, right);
                        }
                    }
                }
            });
        }
    }

    private List<Pair<JParameterizedClassType, JParameterizedClassType>> commonSupertypes(JType left, JType right) {
        List<Pair<JParameterizedClassType, JParameterizedClassType>> result = new ArrayList<>();

        Set<JType> leftSupers = left.typeSystem().operations().findAllKnownSupertypes(left);
        Set<JType> rightSupers = left.typeSystem().operations().findAllKnownSupertypes(right);

        for (JType leftSuper : leftSupers) {
            if (leftSuper instanceof JParameterizedClassType lct) {
                for (JType rightSuper : rightSupers) {
                    if (rightSuper instanceof JParameterizedClassType rct && lct.classReference().typeEquals(rct.classReference())) {
                        result.add(Pair.of(lct, rct));
                    }
                }
            }
        }

        return result;
    }

    private static void addEqualBound(ConstraintBranch branch, Constraint leftCons, JType left, Constraint rightCons, JType right) {
        if (left.isProperType() && right.isProperType()) {
            boolean equal = left.typeEquals(right);
            if (equal) {
                branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Equal(left, right), Constraint.Status.FALSE);
                branch.add(new JTypeConstraints.Contradiction(leftCons, rightCons), Constraint.Status.FALSE);
            }
        } else {
            branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.ASSUMED);
        }
    }

    private static void addSubtypeBound(ConstraintBranch branch, Constraint leftCons, JType left, Constraint rightCons, JType right) {
        if (left.isProperType() && right.isProperType()) {
            boolean subtype = left.isAssignableTo(right);
            if (subtype) {
                branch.add(JTypeConstraints.Subtype.createBound(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Subtype(left, right), Constraint.Status.FALSE);
                branch.add(new JTypeConstraints.Contradiction(leftCons, rightCons), Constraint.Status.FALSE);
            }
        } else {
            branch.add(JTypeConstraints.Subtype.createBound(left, right), Constraint.Status.ASSUMED);
        }
    }
}
