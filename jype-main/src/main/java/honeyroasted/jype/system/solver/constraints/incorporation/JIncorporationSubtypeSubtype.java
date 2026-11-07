package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.List;

public class JIncorporationSubtypeSubtype extends ConstraintMapper.Binary<JTypeConstraints.Subtype, JTypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue() && JIncorporationSupport.isBound(constraint);
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue() && JIncorporationSupport.isBound(constraint);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype left, Constraint.Status leftStatus, JTypeConstraints.Subtype right, Constraint.Status rightStatus) {
        if (left.equals(right)) {
            return;
        }
        if (left.right() instanceof JMetaVarType alpha && right.left().typeEquals(alpha)) {
            // JLS §18.3.1 S <: α and α <: T imply ‹S <: T›
            JIncorporationSupport.deriveSubtype(branch, left, right, left.left(), right.right());
        }
        if (left.left() instanceof JMetaVarType alpha && right.left().typeEquals(alpha)) {
            // JLS §18.3.1 α <: S and α <: T with supertypes G<S1..Sn> and G<T1..Tn> imply ‹Si = Ti›
            for (Pair<JClassType, JClassType> pair : commonParameterizations(left.right(), right.right())) {
                List<JArgumentType> sa = pair.left().typeArguments();
                List<JArgumentType> ta = pair.right().typeArguments();
                for (int i = 0; i < sa.size() && i < ta.size(); i++) {
                    if (!(sa.get(i) instanceof JWildType) && !(ta.get(i) instanceof JWildType)) {
                        JIncorporationSupport.deriveEqual(branch, left, right, sa.get(i), ta.get(i));
                    }
                }
            }
        }
    }

    private static List<Pair<JClassType, JClassType>> commonParameterizations(JType s, JType t) {
        List<Pair<JClassType, JClassType>> result = new ArrayList<>();
        if (s instanceof JMetaVarType || t instanceof JMetaVarType) {
            return result;
        }
        for (JClassType sup : JTypeRelations.allSupertypeClasses(s)) {
            if (sup.hasTypeArguments() && !JTypeRelations.isRaw(sup)) {
                JClassType other = JTypeRelations.asSuper(t, sup.classReference());
                if (other != null && other.hasTypeArguments() && !JTypeRelations.isRaw(other)) {
                    result.add(Pair.of(sup, other));
                }
            }
        }
        return result;
    }
}
