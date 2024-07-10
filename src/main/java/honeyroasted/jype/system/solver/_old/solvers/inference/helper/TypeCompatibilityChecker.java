package honeyroasted.jype.system.solver._old.solvers.inference.helper;

import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver._old.solvers.inference.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
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

    public TypeCompatibilityChecker() {
        super();
    }

    public boolean isSubtype(Type subtype, Type supertype) {
        return this.check(new TypeBound.Subtype(subtype, supertype)).build().satisfied();
    }

    public boolean isSubtype(Type subtype, Type supertype, TypeBound.Result.Builder... parents) {
        return this.check(new TypeBound.Subtype(subtype, supertype), parents).build().satisfied();
    }

    public boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context context) {
        return this.check(new TypeBound.Compatible(subtype, supertype, context)).build().satisfied();
    }

    public boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context context, TypeBound.Result.Builder... parents) {
        return this.check(new TypeBound.Compatible(subtype, supertype, context), parents).build().satisfied();
    }

    public TypeBound.Result.Builder check(TypeBound.Subtype subtype, TypeBound.Result.Builder... parents) {
        return strictSubtype(subtype.left(), subtype.right(), new HashSet<>(), parents);
    }

    public TypeBound.Result.Builder check(TypeBound.Compatible compatible, TypeBound.Result.Builder... parents) {
        TypeBound.Result.Builder builder = TypeBound.Result.builder(compatible, parents);
        switch (compatible.context()) {
            case STRICT_INVOCATION -> strictSubtype(compatible.left(), compatible.right(), new HashSet<>(), builder);
            case ASSIGNMENT, LOOSE_INVOCATION -> looseInvocation(compatible.left(), compatible.right(), builder);
            case EXPLICIT_CAST -> explicitCast(compatible.left(), compatible.right(), builder);
        }
        return builder;
    }

    public TypeBound.Result.Builder check(TypeBound.ExpressionCompatible compatible, TypeBound.Result.Builder... parents) {
        Type expr;

        if (compatible.left().isSimplyTyped()) {
            expr = compatible.left().getSimpleType(compatible.right().typeSystem()).get();
        } else {
            //TODO
            return TypeBound.Result.builder(new TypeBound.Standalone(compatible.left()), parents)
                    .setSatisfied(false);
        }

        TypeBound.Result.Builder builder = TypeBound.Result.builder(compatible, parents);
        switch (compatible.context()) {
            case STRICT_INVOCATION -> strictSubtype(expr, compatible.right(), new HashSet<>(), builder);
            case LOOSE_INVOCATION -> looseInvocation(expr, compatible.right(), builder);
            case EXPLICIT_CAST -> explicitCast(expr, compatible.right(), builder);
            case ASSIGNMENT -> {
                if (compatible.left() instanceof ExpressionInformation.Constant cnst) {
                    assignment(expr, compatible.right(), cnst, builder);
                } else {
                    looseInvocation(expr, compatible.right(), builder);
                }
            }
        }
        return builder;
    }

    private void explicitCast(Type value, Type cast, TypeBound.Result.Builder parent) {
        parent.setPropagation(TypeBound.Result.Propagation.OR);
        looseInvocation(value, cast, parent);
        looseInvocation(cast, value, parent);
    }

    private void assignment(Type subtype, Type target, ExpressionInformation.Constant constantExpression, TypeBound.Result.Builder parent) {
        parent.setPropagation(TypeBound.Result.Propagation.OR);

        TypeConstants c = subtype.typeSystem().constants();

        Type cnst = constantExpression.type(subtype.typeSystem());
        Object val = constantExpression.value();

        if (cnst.typeEquals(c.byteType()) || cnst.typeEquals(c.shortType()) || cnst.typeEquals(c.charType()) || cnst.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(true);
                } else {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(true);
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(true);
                } else {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(false);
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(true);
                } else {
                    TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent)
                            .setSatisfied(false);
                }
            }
        }

        looseInvocation(subtype, target, parent);
    }

    private static boolean fits(Object obj, long min, long max) {
        if (obj instanceof Number n) {
            return min <= n.longValue() && n.longValue() <= max;
        } else if (obj instanceof Character c) {
            return min <= c && c <= max;
        }
        return false;
    }

    private void looseInvocation(Type subtype, Type supertype, TypeBound.Result.Builder parent) {
        parent.setPropagation(TypeBound.Result.Propagation.OR);

        strictSubtype(subtype, supertype, new HashSet<>(), parent);

        if (subtype instanceof PrimitiveType l && !(supertype instanceof PrimitiveType)) {
            strictSubtype(l.box(), supertype, new HashSet<>(), parent);
        } else if (!(subtype instanceof PrimitiveType) && supertype instanceof PrimitiveType r) {
            strictSubtype(subtype, r.box(), new HashSet<>(), parent);
        }
    }

    private TypeBound.Result.Builder strictSubtype(Type subtype, Type supertype, Set<TypeBound.Subtype> seen, TypeBound.Result.Builder... parent) {
        TypeBound.Subtype bound = new TypeBound.Subtype(subtype, supertype);
        TypeBound.Result.Builder builder = TypeBound.Result.builder(new TypeBound.Subtype(subtype, supertype), TypeBound.Result.Propagation.AND, parent);

        if (seen.contains(bound)) {
            TypeBound.Result.builder(new TypeBound.CyclicSubtype(subtype, supertype), builder).setSatisfied(true);
        } else {
            seen = Type.concat(seen, bound);
            Set<TypeBound.Subtype> finalSeen = seen;

            if (supertype instanceof NoneType) {
                builder.setSatisfied(false);
            } else if (subtype instanceof NoneType l) {
                builder.setSatisfied(l.isNullType() && !(supertype instanceof PrimitiveType));
            } else if (subtype instanceof PrimitiveType l && supertype instanceof PrimitiveType r) {
                builder.setSatisfied(PRIM_SUPERS.get(l.name()).contains(r.name()));
            } else if (subtype.typeEquals(supertype)) {
                builder.setSatisfied(true);
            } else if (subtype instanceof ClassType l && supertype instanceof ClassType r) {
                if (!l.hasTypeArguments() && !r.hasTypeArguments()) {
                    if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
                        if (l.hasRelevantOuterType() && r.hasRelevantOuterType()) {
                            strictSubtype(l.outerType(), r.outerType(), seen, builder);
                        } else {
                            builder.setSatisfied(false);
                        }
                    } else {
                        builder.setSatisfied(l.classReference().hasSupertype(r.classReference()));
                    }
                } else if (supertype instanceof ParameterizedClassType pcr) {
                    Optional<ClassType> superTypeOpt = (l instanceof ParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                            .relativeSupertype(pcr.classReference());
                    if (superTypeOpt.isPresent()) {
                        TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), r.classReference()), builder).setSatisfied(true);

                        ClassType relative = superTypeOpt.get();
                        TypeBound.Result.Builder argsMatch = TypeBound.Result.builder(new TypeBound.TypeArgumentsMatch(relative, pcr), TypeBound.Result.Propagation.AND, builder);
                        if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                            for (int i = 0; i < relative.typeArguments().size(); i++) {
                                Type ti = relative.typeArguments().get(i);
                                Type si = pcr.typeArguments().get(i);

                                if (si instanceof WildType || si instanceof VarType || si instanceof MetaVarType) {
                                    TypeBound.Result.Builder argMatch = TypeBound.Result.builder(new TypeBound.GenericParameter(ti, si), TypeBound.Result.Propagation.AND, argsMatch);
                                    if (si instanceof WildType.Upper siwtu) {
                                        pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                                .forEach(argBound -> strictSubtype(ti, argBound, finalSeen, argMatch));
                                        siwtu.upperBounds()
                                                .forEach(wildBound -> strictSubtype(ti, wildBound, finalSeen, argMatch));
                                    } else if (si instanceof WildType.Lower siwtl) {
                                        pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                                .forEach(argBound -> strictSubtype(ti, argBound, finalSeen, argMatch));
                                        siwtl.lowerBounds()
                                                .forEach(wildBound -> strictSubtype(wildBound, ti, finalSeen, argMatch));
                                    } else if (si instanceof MetaVarType mvt) {
                                        pcr.typeParameters().get(i).upperBounds().stream().map(pcr.varTypeResolver())
                                                .forEach(argBound -> strictSubtype(ti, argBound, finalSeen, argMatch));
                                        mvt.upperBounds()
                                                .forEach(wildBound -> strictSubtype(ti, wildBound, finalSeen, argMatch));
                                        mvt.lowerBounds()
                                                .forEach(wildBound -> strictSubtype(wildBound, ti, finalSeen, argMatch));
                                    }
                                } else {
                                    TypeBound.Result.builder(new TypeBound.Equal(ti, si), argsMatch)
                                            .setSatisfied(ti.typeEquals(si));
                                }
                            }

                            if (l.hasRelevantOuterType() && r.hasRelevantOuterType()) {
                                strictSubtype(l.outerType(), r.outerType(), seen, builder);
                            }
                        } else {
                            argsMatch.setSatisfied(false);
                        }
                    } else {
                        TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), r.classReference()), builder).setSatisfied(false);
                    }
                } else {
                    builder.setSatisfied(false);
                }
            } else if (subtype instanceof ArrayType l) {
                if (supertype instanceof ArrayType r) {
                    if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                        builder.setSatisfied(r.component().typeEquals(l.component()));
                    } else {
                        strictSubtype(l.component(), r.component(), seen, builder);
                    }
                } else {
                    builder.setPropagation(TypeBound.Result.Propagation.OR);
                    strictSubtype(subtype.typeSystem().constants().object(), supertype, seen, builder);
                    strictSubtype(subtype.typeSystem().constants().cloneable(), supertype, seen, builder);
                    strictSubtype(subtype.typeSystem().constants().serializable(), supertype, seen, builder);
                }
            } else if (subtype instanceof VarType l) {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
                l.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
            } else if (subtype instanceof MetaVarType mvt) {
                if (mvt.upperBounds().isEmpty()) {
                    builder.setSatisfied(false);
                } else {
                    builder.setPropagation(TypeBound.Result.Propagation.OR);
                    mvt.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
                }
            } else if (supertype instanceof MetaVarType mvt) {
                if (mvt.lowerBounds().isEmpty()) {
                    builder.setSatisfied(false);
                } else {
                    mvt.lowerBounds().forEach(t -> strictSubtype(subtype, t, finalSeen, builder));
                }
            } else if (subtype instanceof WildType.Upper l) {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
                l.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
            } else if (supertype instanceof WildType.Lower r) {
                r.lowerBounds().forEach(t -> strictSubtype(subtype, t, finalSeen, builder));
            } else if (subtype instanceof IntersectionType l) {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
                l.children().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
            } else if (supertype instanceof IntersectionType r) {
                r.children().forEach(t -> strictSubtype(subtype, t, finalSeen, builder));
            }
        }
        return builder;
    }

}
