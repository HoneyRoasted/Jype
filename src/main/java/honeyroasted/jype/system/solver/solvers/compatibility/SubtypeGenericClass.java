package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.List;
import java.util.Optional;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeGenericClass implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ClassType && st.right() instanceof ClassType ct && ct.hasTypeArguments();
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.AND);

        ClassType l = (ClassType) bound.left();
        ParameterizedClassType pcr = (ParameterizedClassType) bound.right();

        Optional<ClassType> superTypeOpt = (l instanceof ParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                .relativeSupertype(pcr.classReference());
        if (superTypeOpt.isPresent()) {
            bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), pcr.classReference()), constraint).setSatisfied(true));

            ClassType relative = superTypeOpt.get();
            TypeBound.Result.Builder argsMatch = TypeBound.Result.builder(new TypeBound.TypeArgumentsMatch(relative, pcr), TypeBound.Result.Propagation.AND, constraint);
            if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                for (int i = 0; i < relative.typeArguments().size(); i++) {
                    Type ti = relative.typeArguments().get(i);
                    Type si = pcr.typeArguments().get(i);

                    if (si instanceof WildType || si instanceof VarType || si instanceof MetaVarType) {
                        TypeBound.Result.Builder argMatch = TypeBound.Result.builder(new TypeBound.GenericParameter(ti, si), TypeBound.Result.Propagation.AND, argsMatch);
                        if (si instanceof WildType.Upper siwtu) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(ti, argBound), argMatch)));
                            siwtu.upperBounds()
                                    .forEach(wildBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(ti, wildBound), argMatch)));
                        } else if (si instanceof WildType.Lower siwtl) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(ti, argBound), argMatch)));
                            siwtl.lowerBounds()
                                    .forEach(wildBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(wildBound, ti), argMatch)));
                        } else if (si instanceof MetaVarType mvt) {
                            pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                    .forEach(argBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(ti, argBound), argMatch)));
                            mvt.upperBounds()
                                    .forEach(wildBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(ti, wildBound), argMatch)));
                            mvt.lowerBounds()
                                    .forEach(wildBound -> constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(wildBound, ti), argMatch)));
                        }
                    } else {
                        TypeBound.Result.builder(new TypeBound.Equal(ti, si), argsMatch)
                                .setSatisfied(ti.typeEquals(si));
                    }
                }

                if (l.hasRelevantOuterType() && pcr.hasRelevantOuterType()) {
                    constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(l.outerType(), pcr.outerType()), constraint));
                }
            } else {
                argsMatch.setSatisfied(false);
            }
        } else {
            bounds.add(TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), pcr.classReference()), constraint).setSatisfied(false));
        }
    }
}
