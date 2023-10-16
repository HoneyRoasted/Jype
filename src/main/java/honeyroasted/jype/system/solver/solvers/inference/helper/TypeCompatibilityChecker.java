package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TypeCompatibilityChecker extends AbstractInferenceHelper {
    private static final Map<String, Set<String>> PRIM_SUPERS =
            Map.of(
                    "boolean", Set.of("boolean"),
                    "byte", Set.of("byte", "short", "int", "long", "float", "double"),
                    "short", Set.of("short", "int", "long", "float", "double"),
                    "char", Set.of("char", "int", "long", "float", "double"),
                    "int", Set.of("int", "long", "float", "double"),
                    "long", Set.of("long", "float", "double"),
                    "float", Set.of("float", "double"),
                    "double", Set.of("double")
            );

    public TypeCompatibilityChecker(TypeSolver solver) {
        super(solver);
    }

    private void strictSubtype(Type subtype, Type supertype, Set<TypeBound.Subtype> seen, TypeBound.Result.Builder... parents) {
        TypeBound.Subtype bound = new TypeBound.Subtype(subtype, subtype);
        if (!subtype.equals(supertype) && seen.contains(bound)) {
            //Subtype is cyclic, cannot handle without inference
            this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NonCyclicSubtype(subtype, subtype), parents)).setSatisfied(false));
        } else {
            seen = new HashSet<>(seen);
            seen.add(bound);
            Set<TypeBound.Subtype> finalSeen = seen;

            if (supertype instanceof NoneType) {
                setSatisfied(false, parents);
            } else if (subtype instanceof NoneType l) {
                setSatisfied(l.equals(l.typeSystem().constants().nullType()) && !(supertype instanceof PrimitiveType), parents);
            }  else if (subtype instanceof PrimitiveType l && supertype instanceof PrimitiveType r) {
                setSatisfied(PRIM_SUPERS.get(l.name()).contains(r.name()), parents);
            } else if (subtype.equals(supertype)) {
                setSatisfied(true, parents);
            } else if (subtype instanceof ClassType l && supertype instanceof ClassType r) {
                if (!l.hasTypeArguments() || !r.hasTypeArguments()) {
                    setSatisfied(l.hasSupertype(r.classReference()), parents);
                } else if (l instanceof ParameterizedClassType pcl && supertype instanceof ParameterizedClassType pcr) {
                    Optional<ClassType> superTypeOpt = pcl.relativeSupertype(pcr.classReference());
                    if (superTypeOpt.isPresent()) {
                        ClassType relative = superTypeOpt.get();
                        if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                            TypeBound.Result.Builder argsMatch = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.TypeArgumentsMatch(relative, supertype), TypeBound.Result.Propagation.AND));
                            for (int i = 0; i < relative.typeArguments().size(); i++) {
                                Type ti = relative.typeArguments().get(i);
                                Type si = pcr.typeParameters().get(i);

                                if (si instanceof WildType) {
                                    TypeBound.Result.Builder argMatch = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.GenericParameter(ti, si), TypeBound.Result.Propagation.AND, argsMatch));
                                    if (si instanceof WildType.Upper siwtu) {
                                        pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                                .forEach(argBound -> strictSubtypeWithBound(ti, argBound, finalSeen, argMatch));
                                        siwtu.upperBounds()
                                                .forEach(wildBound -> strictSubtypeWithBound(ti, wildBound, finalSeen, argMatch));
                                    } else if (si instanceof WildType.Lower siwtl) {
                                        pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                                .forEach(argBound -> strictSubtypeWithBound(ti, argBound, finalSeen, argMatch));
                                        siwtl.lowerBounds()
                                                .forEach(wildBound -> strictSubtypeWithBound(wildBound, ti, finalSeen, argMatch));
                                    }
                                } else {
                                    TypeBound.Result.builder(new TypeBound.Equal(ti, si), argsMatch)
                                            .setSatisfied(ti.equals(si));
                                }
                            }
                        } else {
                            setSatisfied(false, parents);
                        }
                    } else {
                        setSatisfied(false, parents);
                    }
                } else {
                    //Should be unreachable
                    setSatisfied(false, parents);
                }
            } else if (subtype instanceof ArrayType l) {
                if (supertype instanceof ArrayType r) {
                    if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                        setSatisfied(r.component().equals(l.component()), parents);
                    } else {
                        strictSubtypeWithBound(l.component(), r.component(), seen, parents);
                    }
                } else {
                    TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subtype, supertype), TypeBound.Result.Propagation.OR, parents));
                    strictSubtypeWithBound(subtype.typeSystem().constants().object(), supertype, seen, builder);
                    strictSubtypeWithBound(subtype.typeSystem().constants().cloneable(), supertype, seen, builder);
                    strictSubtypeWithBound(subtype.typeSystem().constants().serializable(), supertype, seen, builder);
                }
            } else if (subtype instanceof VarType l) {
                TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(l, subtype), TypeBound.Result.Propagation.AND, parents));
                l.upperBounds().forEach(t -> strictSubtypeWithBound(t, supertype, finalSeen, builder));
            } else if (subtype instanceof WildType.Upper l) {
                TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(l, subtype), TypeBound.Result.Propagation.AND, parents));
                l.upperBounds().forEach(t -> strictSubtypeWithBound(t, supertype, finalSeen, builder));
            } else if (subtype instanceof IntersectionType l) {
                TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(l, subtype), TypeBound.Result.Propagation.OR, parents));
                l.children().forEach(t -> strictSubtypeWithBound(t, supertype, finalSeen, builder));
            } else if (supertype instanceof IntersectionType r) {
                TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subtype, r), TypeBound.Result.Propagation.AND, parents));
                r.children().forEach(t -> strictSubtypeWithBound(subtype, t, finalSeen, builder));
            }
        }
    }

    private void strictSubtypeWithBound(Type subtype, Type supertype, TypeBound.Subtype bound, Set<TypeBound.Subtype> seen, TypeBound.Result.Builder... parents) {
        strictSubtype(subtype, supertype, seen, this.eventBoundCreated(TypeBound.Result.builder(bound, TypeBound.Result.Propagation.AND, parents)));
    }

    private void strictSubtypeWithBound(Type subtype, Type supertype, Set<TypeBound.Subtype> seen, TypeBound.Result.Builder... parents) {
        strictSubtype(subtype, supertype, seen, this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subtype, supertype), TypeBound.Result.Propagation.AND, parents)));
    }

    private void setSatisfied(boolean satisfied, TypeBound.Result.Builder... parents) {
        for (TypeBound.Result.Builder parent : parents) {
            parent.setSatisfied(satisfied);
            this.eventBoundSatisfiedOrUnsatisfied(parent);
        }
    }

}
