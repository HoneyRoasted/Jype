package honeyroasted.jype.system.solver.constraints;

import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintResult;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JStatusConstraintTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JTreeConstraintTracker;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JTypeCompatibility {

    public static boolean isSubtype(JType left, JType right) {
        JConstraintTracker tracker = new JStatusConstraintTracker();
        checkSubtype(left, right, tracker);
        return tracker.status();
    }

    public static JConstraintResult checkSubtype(JType left, JType right) {
        JConstraintTracker tracker = new JTreeConstraintTracker(true);
        checkSubtype(left, right, tracker);
        return tracker.result();
    }

    public static void checkSubtype(JType left, JType right, JConstraintTracker tracker) {
        isSubtype(left, right, tracker);
    }

    public static boolean isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context) {
        JConstraintTracker tracker = new JStatusConstraintTracker();
        checkCompatible(left, right, context, tracker);
        return tracker.status();
    }

    public static void checkCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker) {
        isCompatible(new JTypeConstraints.Compatible(left, context, right), tracker);
    }

    private static void isCompatible(JTypeConstraints.Compatible constraint, JConstraintTracker tracker) {
        tracker.with(constraint);
        switch (constraint.middle()) {
            case STRICT_INVOCATION -> tracker.then(tr -> strictInvocation(constraint.left(), constraint.right(), tr));
            case EXPLICIT_CAST -> tracker.then(tr -> explicitCast(constraint.left(), constraint.right(), tr));
            case LOOSE_INVOCATION, ASSIGNMENT ->
                    tracker.then(tr -> looseInvocation(constraint.left(), constraint.right(), tr));

        }
    }

    private static void isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker) {
        isCompatible(new JTypeConstraints.Compatible(left, context, right), tracker);
    }

    private static void explicitCast(JType left, JType right, JConstraintTracker tracker) {
        tracker.or(tr -> isCompatible(left, right, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, tr),
                tr -> isCompatible(right, left, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, tr));
    }

    private static void strictInvocation(JType left, JType right, JConstraintTracker tracker) {
        tracker.and(tr -> isSubtype(left, right, tr));
    }

    private static void looseInvocation(JType left, JType right, JConstraintTracker tracker) {
        tracker.or(tr -> isSubtype(left, right, tr), tre -> {
            if (left instanceof JPrimitiveType l && !(right instanceof JPrimitiveType)) {
                tracker.then(tr -> isSubtype(l.box(), right, tr));
            } else if (right instanceof JPrimitiveType r && !(left instanceof JPrimitiveType)) {
                tracker.then(tr -> isSubtype(left, r.box(), tr));
            }
        });
    }

    private static void equal(JType left, JType right, JConstraintTracker tracker) {
        tracker.and(tr -> tr.with(new JTypeConstraints.Equal(left, right), left.typeEquals(right)));
    }

    private static void isSubtype(JType left, JType right, JConstraintTracker tracker) {
        tracker.and(tra -> tra.with(new JTypeConstraints.Subtype(left, right)).or(
                tr -> equal(left, right, tr),
                tr -> subtypePrimitive(left, right, tr),
                tr -> subtypeArray(left, right, tr),
                tr -> subtypeIntersection(left, right, tr),
                tr -> subtypeNone(left, right, tr),
                tr -> subtypeWild(left, right, tr),
                tr -> subtypeVar(left, right, tr),
                tr -> subtypeUnchecked(left, right, tr),
                tr -> subtypeRaw(left, right, tr),
                tr -> subtypeGenericClass(left, right, tr)));
    }

    private static void subtypeArray(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JArrayType l) {
            if (right instanceof JArrayType supertype) {
                if (supertype.component() instanceof JPrimitiveType || l.component() instanceof JPrimitiveType) {
                    tracker.and(tr -> equal(l.component(), supertype.component(), tracker));
                } else {
                    tracker.and(tr -> isSubtype(l.component(), supertype.component(), tr));
                }
            } else {
                JTypeConstants c = left.typeSystem().constants();
                tracker.or(
                        tr -> isSubtype(c.object(), right, tr),
                        tr -> isSubtype(c.cloneable(), right, tr),
                        tr -> isSubtype(c.serializable(), right, tr));
            }
        }
    }

    private static void subtypeIntersection(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JIntersectionType l) {
            tracker.or(tra -> l.children().forEach(t -> tra.then(tr -> isSubtype(t, right, tr))));
        } else if (right instanceof JIntersectionType r) {
            tracker.and(tra -> r.children().forEach(t -> tra.then(tr -> isSubtype(left, t, tr))));
        }
    }

    private static void subtypeNone(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JNoneType) {
            tracker.with(right.typeEquals(left));
        } else if (right instanceof JNoneType) {
            tracker.with(left.isNullType() && !(right instanceof JPrimitiveType));
        }
    }

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


    private static void subtypePrimitive(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JPrimitiveType l && right instanceof JPrimitiveType r) {
            tracker.with(PRIM_SUPERS.get(l.name()).contains(r.name()));
        } else if (left instanceof JPrimitiveType || right instanceof JPrimitiveType) {
            tracker.with(false);
        }
    }

    private static void subtypeWild(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JWildType.Upper l) {
            tracker.or(tra -> l.upperBounds().forEach(b -> tra.then(tr -> isSubtype(b, right, tr))));
        } else if (right instanceof JWildType.Lower r) {
            tracker.and(tra -> r.lowerBounds().forEach(b -> tra.then(tr -> isSubtype(left, b, tr))));
        }
    }

    private static void subtypeVar(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JVarType l) {
            tracker.and(tra -> l.upperBounds().forEach(b -> tra.then(tr -> isSubtype(b, right, tr))));
        }
    }

    private static void subtypeUnchecked(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) || (!l.hasAnyTypeArguments() && r.hasAnyTypeArguments()))) {
            tracker.and(tr -> isSubtype(l.classReference(), r.classReference(), tracker));
        }
    }

    private static void subtypeRaw(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JClassType r && !l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) {
            tracker.and(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), l.classReference().hasSupertype(r.classReference())));
        }
    }

    private static void subtypeGenericClass(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JParameterizedClassType r && r.hasAnyTypeArguments()) {
            Optional<JClassType> superTypeOpt = (left instanceof JParameterizedClassType pct ? pct : ((JClassType) left).classReference().parameterizedWithTypeVars())
                    .relativeSupertype(r.classReference());
            if (superTypeOpt.isPresent()) {
                JClassType relative = superTypeOpt.get();

                tracker.and(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), true),
                        tr -> typeArgumentsMatch(relative, r, tr),
                        tr -> outerTypesMatch(relative, r, tr));
            } else {
                tracker.and(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), false));
            }
        }
    }

    private static void outerTypesMatch(JClassType left, JClassType right, JConstraintTracker tracker) {
        if (left.hasRelevantOuterType() && right.hasRelevantOuterType()) {
            tracker.with(new JTypeConstraints.OuterTypesMatch(left, right))
                    .and(tr -> isSubtype(left.outerType(), right.outerType(), tracker));
        }
    }

    private static void typeArgumentsMatch(JClassType left, JClassType right, JConstraintTracker tracker) {
        tracker.with(new JTypeConstraints.TypeArgumentsMatch(left, right)).and(tr1 -> {
            if (left.typeArguments().isEmpty() && right.typeArguments().isEmpty()) {
                tracker.with(true);
            } else {
                if (right instanceof JParameterizedClassType pcr && left.typeArguments().size() == right.typeArguments().size()) {
                    for (int i = 0; i < left.typeArguments().size(); i++) {
                        JType ti = left.typeArguments().get(i);
                        JType si = right.typeArguments().get(i);

                        int finalI = i;
                        tracker.with(new JTypeConstraints.TypeArgumentMatch(ti, si)).and(tre -> {
                            if (si instanceof JWildType.Upper siwtu) {
                                pcr.typeParameters().get(finalI).upperBounds().stream().map(pcr.varTypeResolver())
                                        .forEach(argBound -> tracker.and(tr -> isSubtype(ti, argBound, tr)));
                                siwtu.upperBounds()
                                        .forEach(wildBound -> tracker.and(tr -> isSubtype(ti, wildBound, tr)));
                            } else if (si instanceof JWildType.Lower siwtl) {
                                pcr.typeParameters().get(finalI).upperBounds().stream().map(pcr.varTypeResolver())
                                        .forEach(argBound -> tracker.and(tr -> isSubtype(ti, argBound, tr)));
                                siwtl.lowerBounds()
                                        .forEach(wildBound -> tracker.and(tr -> isSubtype(wildBound, ti, tr)));
                            } else {
                                tracker.and(tr -> equal(ti, si, tr));
                            }
                        });
                    }
                } else {
                    tracker.with(false);
                }
            }
        });
    }
}
