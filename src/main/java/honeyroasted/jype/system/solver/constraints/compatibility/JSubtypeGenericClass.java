package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.Optional;
import java.util.function.Function;

public class JSubtypeGenericClass extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof JClassType && right instanceof JClassType ct && ct.hasTypeArguments();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        JClassType l = (JClassType) left;
        JParameterizedClassType pcr = (JParameterizedClassType) right;

        Optional<JClassType> superTypeOpt = (l instanceof JParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                .relativeSupertype(pcr.classReference());
        if (superTypeOpt.isPresent()) {
            JClassType relative = superTypeOpt.get();

            if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                branch.drop(constraint);
                for (int i = 0; i < relative.typeArguments().size(); i++) {
                    JType ti = relative.typeArguments().get(i);
                    JType si = pcr.typeArguments().get(i);

                    if (si instanceof JWildType || si instanceof JVarType || si instanceof JMetaVarType) {
                        if (si instanceof JWildType.Upper siwtu) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new JTypeConstraints.Subtype(ti, argBound)));
                            siwtu.upperBounds()
                                    .forEach(wildBound -> branch.add(new JTypeConstraints.Subtype(ti, wildBound)));
                        } else if (si instanceof JWildType.Lower siwtl) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new JTypeConstraints.Subtype(ti, argBound)));
                            siwtl.lowerBounds()
                                    .forEach(wildBound -> branch.add(new JTypeConstraints.Subtype(wildBound, ti)));
                        } else if (si instanceof JMetaVarType mvt) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new JTypeConstraints.Subtype(ti, argBound)));
                            mvt.upperBounds()
                                    .forEach(wildBound -> branch.add(new JTypeConstraints.Subtype(ti, wildBound)));
                            mvt.lowerBounds()
                                    .forEach(wildBound -> branch.add(new JTypeConstraints.Subtype(wildBound, ti)));
                        }
                    } else {
                        branch.add(new JTypeConstraints.Equal(ti, si));
                    }
                }

                if (l.hasRelevantOuterType() && pcr.hasRelevantOuterType()) {
                    branch.add(new JTypeConstraints.Subtype(l.outerType(), pcr.outerType()));
                }
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }
}
