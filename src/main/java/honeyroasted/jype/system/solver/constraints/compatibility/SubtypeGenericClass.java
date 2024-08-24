package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class SubtypeGenericClass implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public boolean filter(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return left.isProperType() && right.isProperType() &&
                left instanceof ClassType && right instanceof ClassType ct && ct.hasTypeArguments();
    }

    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ClassType l = (ClassType) left;
        ParameterizedClassType pcr = (ParameterizedClassType) right;

        Optional<ClassType> superTypeOpt = (l instanceof ParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                .relativeSupertype(pcr.classReference());
        if (superTypeOpt.isPresent()) {
            Set<ConstraintNode> newChildren = new LinkedHashSet<>();

            TrackedConstraint.of(new TypeConstraints.Subtype(l.classReference(), pcr.classReference()), node.trackedConstraint());

            ClassType relative = superTypeOpt.get();

            if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                ConstraintTree argsMatch = new ConstraintTree(new TypeConstraints.TypeArgumentsMatch(relative, pcr).tracked(node.trackedConstraint()), ConstraintNode.Operation.AND);
                newChildren.add(argsMatch);
                for (int i = 0; i < relative.typeArguments().size(); i++) {
                    Type ti = relative.typeArguments().get(i);
                    Type si = pcr.typeArguments().get(i);

                    if (si instanceof WildType || si instanceof VarType || si instanceof MetaVarType) {
                        TrackedConstraint argMatch = TrackedConstraint.of(new TypeConstraints.GenericParameter(ti, si), argsMatch.trackedConstraint());
                        if (si instanceof WildType.Upper siwtu) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> argsMatch.attach(new TypeConstraints.Subtype(ti, argBound).tracked(argMatch)));
                            siwtu.upperBounds()
                                    .forEach(wildBound -> argsMatch.attach(new TypeConstraints.Subtype(ti, wildBound).tracked(argMatch)));
                        } else if (si instanceof WildType.Lower siwtl) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> argsMatch.attach(new TypeConstraints.Subtype(ti, argBound).tracked(argMatch)));
                            siwtl.lowerBounds()
                                    .forEach(wildBound -> argsMatch.attach(new TypeConstraints.Subtype(wildBound, ti).tracked(argMatch)));
                        } else if (si instanceof MetaVarType mvt) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> argsMatch.attach(new TypeConstraints.Subtype(ti, argBound).tracked(argMatch)));
                            mvt.upperBounds()
                                    .forEach(wildBound -> argsMatch.attach(new TypeConstraints.Subtype(ti, wildBound).tracked(argMatch)));
                            mvt.lowerBounds()
                                    .forEach(wildBound -> argsMatch.attach(new TypeConstraints.Subtype(wildBound, ti).tracked(argMatch)));
                        }
                    } else {
                        argsMatch.attach(new TypeConstraints.Equal(ti, si));
                    }
                }

                if (l.hasRelevantOuterType() && pcr.hasRelevantOuterType()) {
                    newChildren.add(new TypeConstraints.Subtype(l.outerType(), pcr.outerType()).tracked(node.trackedConstraint()).createLeaf());
                }
            } else {
                newChildren.add(new TypeConstraints.TypeArgumentsMatch(relative, pcr).tracked(node.trackedConstraint()).createLeaf().setStatus(ConstraintNode.Status.FALSE));
            }

            node.expand(ConstraintNode.Operation.AND, newChildren);
        } else {
            TrackedConstraint.of(new TypeConstraints.Subtype(l.classReference(), pcr.classReference()), node.trackedConstraint());
            node.overrideStatus(false);
        }
    }
}
