package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.Optional;
import java.util.function.Function;

public class SubtypeGenericClass extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof ClassType && right instanceof ClassType ct && ct.hasTypeArguments();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ClassType l = (ClassType) left;
        ParameterizedClassType pcr = (ParameterizedClassType) right;

        Optional<ClassType> superTypeOpt = (l instanceof ParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                .relativeSupertype(pcr.classReference());
        if (superTypeOpt.isPresent()) {
            ClassType relative = superTypeOpt.get();

            if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                for (int i = 0; i < relative.typeArguments().size(); i++) {
                    Type ti = relative.typeArguments().get(i);
                    Type si = pcr.typeArguments().get(i);

                    if (si instanceof WildType || si instanceof VarType || si instanceof MetaVarType) {
                        if (si instanceof WildType.Upper siwtu) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new TypeConstraints.Subtype(ti, argBound)));
                            siwtu.upperBounds()
                                    .forEach(wildBound -> branch.add(new TypeConstraints.Subtype(ti, wildBound)));
                        } else if (si instanceof WildType.Lower siwtl) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new TypeConstraints.Subtype(ti, argBound)));
                            siwtl.lowerBounds()
                                    .forEach(wildBound -> branch.add(new TypeConstraints.Subtype(wildBound, ti)));
                        } else if (si instanceof MetaVarType mvt) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> branch.add(new TypeConstraints.Subtype(ti, argBound)));
                            mvt.upperBounds()
                                    .forEach(wildBound -> branch.add(new TypeConstraints.Subtype(ti, wildBound)));
                            mvt.lowerBounds()
                                    .forEach(wildBound -> branch.add(new TypeConstraints.Subtype(wildBound, ti)));
                        }
                    } else {
                        branch.add(new TypeConstraints.Equal(ti, si));
                    }
                }

                if (l.hasRelevantOuterType() && pcr.hasRelevantOuterType()) {
                    branch.add(new TypeConstraints.Subtype(l.outerType(), pcr.outerType()));
                }
            } else {
                branch.setStatus(constraint, Constraint.Status.FALSE);
            }
        } else {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        }
    }
}
