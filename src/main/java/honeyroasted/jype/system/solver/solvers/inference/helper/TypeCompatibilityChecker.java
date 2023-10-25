package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.inference.expression.ExpressionInformation;
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

    public boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context context) {
        return this.check(new TypeBound.Compatible(subtype, supertype, context)).build().satisfied();
    }

    public TypeBound.Result.Builder check(TypeBound.Subtype subtype, TypeBound.Result.Builder... parents) {
        return strictSubtype(subtype.left(), subtype.right(), new HashSet<>(), parents);
    }

    public TypeBound.Result.Builder check(TypeBound.Compatible compatible, TypeBound.Result.Builder... parents) {
        TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(compatible, parents));
        switch (compatible.context()) {
            case SUBTYPE, STRICT_INVOCATION -> strictSubtype(compatible.left(), compatible.right(), new HashSet<>(), builder);
            case ASSIGNMENT, LOOSE_INVOCATION -> looseInvocation(compatible.left(), compatible.right(), builder);
            case EXPLICIT_CAST -> explicitCast(compatible.left(), compatible.right(), builder);
        }
        this.eventBoundSatisfiedOrUnsatisfied(builder);
        return builder;
    }

    public TypeBound.Result.Builder check(TypeBound.ExpressionCompatible compatible, TypeBound.Result.Builder... parents) {
        Type expr;

        if (compatible.left().isStandalone()) {
            expr = compatible.left().getStandaloneType(compatible.right().typeSystem()).get();
        } else {
            return this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Standalone(compatible.left()), parents))
                    .setSatisfied(false));
        }

        TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(compatible, parents));
        switch (compatible.context()) {
            case SUBTYPE, STRICT_INVOCATION -> strictSubtype(expr, compatible.right(), new HashSet<>(), builder);
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
        this.eventBoundSatisfiedOrUnsatisfied(builder);
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

        Type cnst = constantExpression.type();
        Object val = constantExpression.value();

        if (cnst.typeEquals(c.byteType()) || cnst.typeEquals(c.shortType()) || cnst.typeEquals(c.charType()) || cnst.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    this.eventBoundSatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(true));
                } else {
                    this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(true));
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    this.eventBoundSatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(true));
                } else {
                    this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(false));
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    this.eventBoundSatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(true));
                } else {
                    this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NarrowConstant(constantExpression, target), parent))
                            .setSatisfied(false));
                }
            }
        }

        looseInvocation(subtype, target, parent);
    }

    private static boolean fits(Object obj, int min, int max) {
        if (obj instanceof Number n) {
            return min <= n.intValue() && n.intValue() <= max;
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
        TypeBound.Subtype bound = new TypeBound.Subtype(subtype, subtype);
        TypeBound.Result.Builder builder = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Subtype(subtype, supertype), TypeBound.Result.Propagation.AND, parent));

        if (!subtype.typeEquals(supertype) && seen.contains(bound)) {
            //Subtype is cyclic, cannot handle without inference
            this.eventBoundUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.NonCyclicSubtype(subtype, subtype), builder)).setSatisfied(false));
        } else {
            seen = Type.concat(seen, bound);
            Set<TypeBound.Subtype> finalSeen = seen;

            if (supertype instanceof NoneType) {
                builder.setSatisfied(false);
                this.eventBoundSatisfiedOrUnsatisfied(builder);
            } else if (subtype instanceof NoneType l) {
                builder.setSatisfied(l.isNullType() && !(supertype instanceof PrimitiveType));
                this.eventBoundSatisfiedOrUnsatisfied(builder);
            } else if (subtype instanceof PrimitiveType l && supertype instanceof PrimitiveType r) {
                builder.setSatisfied(PRIM_SUPERS.get(l.name()).contains(r.name()));
                this.eventBoundSatisfiedOrUnsatisfied(builder);
            } else if (subtype.typeEquals(supertype)) {
                builder.setSatisfied(true);
                this.eventBoundSatisfiedOrUnsatisfied(builder);
            } else if (subtype instanceof ClassType l && supertype instanceof ClassType r) {
                if (!l.hasTypeArguments() && !r.hasTypeArguments()) {
                    if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
                        if (l.hasRelevantOuterType() && r.hasRelevantOuterType()) {
                            strictSubtype(l.outerType(), r.outerType(), seen, builder);
                            this.eventBoundSatisfiedOrUnsatisfied(builder);
                        } else {
                            builder.setSatisfied(false);
                            this.eventBoundUnsatisfied(builder);
                        }
                    } else {
                        builder.setSatisfied(l.hasSupertype(r.classReference()));
                        this.eventBoundSatisfiedOrUnsatisfied(builder);
                    }
                } else if (supertype instanceof ParameterizedClassType pcr) {
                    Optional<ClassType> superTypeOpt = (l instanceof ParameterizedClassType pcl ? pcl : l.classReference().parameterized())
                            .relativeSupertype(pcr.classReference());
                    if (superTypeOpt.isPresent()) {
                        ClassType relative = superTypeOpt.get();
                        if (relative.typeArguments().size() == pcr.typeArguments().size()) {
                            TypeBound.Result.Builder argsMatch = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.TypeArgumentsMatch(relative, pcr), TypeBound.Result.Propagation.AND, builder));
                            for (int i = 0; i < relative.typeArguments().size(); i++) {
                                Type ti = relative.typeArguments().get(i);
                                Type si = pcr.typeArguments().get(i);

                                if (si instanceof WildType) {
                                    TypeBound.Result.Builder argMatch = this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.GenericParameter(ti, si), TypeBound.Result.Propagation.AND, argsMatch));
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
                                    }
                                } else {
                                    this.eventBoundSatisfiedOrUnsatisfied(this.eventBoundCreated(TypeBound.Result.builder(new TypeBound.Equal(ti, si), argsMatch))
                                            .setSatisfied(ti.typeEquals(si)));
                                }
                            }

                            if (l.hasRelevantOuterType() && r.hasRelevantOuterType()) {
                                strictSubtype(l.outerType(), r.outerType(), seen, builder);
                                this.eventBoundSatisfiedOrUnsatisfied(builder);
                            }
                        } else {
                            builder.setSatisfied(false);
                            this.eventBoundSatisfiedOrUnsatisfied(builder);
                        }
                    } else {
                        builder.setSatisfied(false);
                        this.eventBoundSatisfiedOrUnsatisfied(builder);
                    }
                } else {
                    builder.setSatisfied(false);
                    this.eventBoundSatisfiedOrUnsatisfied(builder);
                }
            } else if (subtype instanceof ArrayType l) {
                if (supertype instanceof ArrayType r) {
                    if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                        builder.setSatisfied(r.component().typeEquals(l.component()));
                        this.eventBoundSatisfiedOrUnsatisfied(builder);
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
                l.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
            } else if (subtype instanceof MetaVarType mvt) {
                if (mvt.upperBounds().isEmpty()) {
                    this.eventBoundUnsatisfied(builder.setSatisfied(false));
                } else {
                    mvt.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
                }
            } else if (supertype instanceof MetaVarType mvt) {
                if (mvt.lowerBounds().isEmpty()) {
                    this.eventBoundUnsatisfied(builder.setSatisfied(false));
                } else {
                    builder.setPropagation(TypeBound.Result.Propagation.OR);
                    mvt.lowerBounds().forEach(t -> strictSubtype(subtype, t, finalSeen, builder));
                }
            } else if (subtype instanceof WildType.Upper l) {
                l.upperBounds().forEach(t -> strictSubtype(t, supertype, finalSeen, builder));
            } else if (supertype instanceof WildType.Lower r) {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
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
