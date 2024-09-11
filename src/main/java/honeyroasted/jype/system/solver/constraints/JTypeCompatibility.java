package honeyroasted.jype.system.solver.constraints;

import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintResult;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintResultTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintStatusTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
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
        JConstraintTracker tracker = new JConstraintStatusTracker();
        checkSubtype(left, right, tracker);
        return tracker.status().isTruthy();
    }

    public static JConstraintResult checkSubtype(JType left, JType right) {
        JConstraintTracker tracker = new JConstraintResultTracker(true);
        checkSubtype(left, right, tracker);
        return tracker.result();
    }

    public static void checkSubtype(JType left, JType right, JConstraintTracker tracker) {
        isSubtype(left, right, tracker);
    }

    public static boolean isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context) {
        JConstraintTracker tracker = new JConstraintStatusTracker();
        checkCompatible(left, right, context, tracker);
        return tracker.status().isTruthy();
    }

    public static void checkCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker) {
        isCompatible(new JTypeConstraints.Compatible(left, context, right), tracker);
    }

    private static void isCompatible(JTypeConstraints.Compatible constraint, JConstraintTracker tracker) {
        switch (constraint.middle()) {
            case STRICT_INVOCATION -> tracker.then(tr -> strictInvocation(constraint, tr));
            case EXPLICIT_CAST -> tracker.then(tr -> explicitCast(constraint, tr));
            case LOOSE_INVOCATION, ASSIGNMENT -> tracker.then(tr -> looseInvocation(constraint, tr));

        }
    }

    private static void isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker) {
        isCompatible(new JTypeConstraints.Compatible(left, context, right), tracker);
    }

    private static void explicitCast(JTypeConstraints.Compatible constraint, JConstraintTracker tracker) {
        tracker.or(constraint,
                tr -> isCompatible(constraint.left(), constraint.right(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, tr),
                tr -> isCompatible(constraint.right(), constraint.left(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, tr));
    }

    private static void strictInvocation(JTypeConstraints.Compatible constraint, JConstraintTracker tracker) {
        tracker.and(constraint, tr -> isSubtype(constraint.left(), constraint.right(), tr));
    }

    private static void looseInvocation(JTypeConstraints.Compatible constraint, JConstraintTracker tracker) {
        JType left = constraint.left();
        JType right = constraint.right();
        tracker.or(constraint, tr -> isSubtype(left, right, tr),
                tr -> {
                    if (left instanceof JPrimitiveType l && !(right instanceof JPrimitiveType)) {
                        tr.then(tr2 -> isSubtype(l.box(), right, tr2));
                    }
                },
                tr -> {
                    if (right instanceof JPrimitiveType r && !(left instanceof JPrimitiveType)) {
                        tr.then(tr2 -> isSubtype(left, r.box(), tr2));
                    }
                });
    }

    private static void equal(JType left, JType right, JConstraintTracker tracker) {
        tracker.with(new JTypeConstraints.Equal(left, right), left.typeEquals(right));
    }

    private static void isSubtype(JType left, JType right, JConstraintTracker tracker) {
        tracker.or(new JTypeConstraints.Subtype(left, right),
                tr -> equal(left, right, tr),
                tr -> subtypePrimitive(left, right, tr),
                tr -> subtypeArray(left, right, tr),
                tr -> subtypeIntersection(left, right, tr),
                tr -> subtypeNone(left, right, tr),
                tr -> subtypeWild(left, right, tr),
                tr -> subtypeVar(left, right, tr),
                tr -> subtypeUnchecked(left, right, tr),
                tr -> subtypeRaw(left, right, tr),
                tr -> subtypeGenericClass(left, right, tr));
    }

    private static void subtypeArray(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JArrayType l) {
            if (right instanceof JArrayType supertype) {
                if (supertype.component() instanceof JPrimitiveType || l.component() instanceof JPrimitiveType) {
                    tracker.then(tr -> equal(l.component(), supertype.component(), tracker));
                } else {
                    tracker.then(tr -> isSubtype(l.component(), supertype.component(), tr));
                }
            } else {
                JTypeConstants c = left.typeSystem().constants();
                tracker.then(
                        tr -> isSubtype(c.object(), right, tr),
                        tr -> isSubtype(c.cloneable(), right, tr),
                        tr -> isSubtype(c.serializable(), right, tr));
            }
        }
    }

    private static void subtypeIntersection(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JIntersectionType l) {
           l.children().forEach(t -> tracker.then(tr -> isSubtype(t, right, tr)));
        } else if (right instanceof JIntersectionType r) {
            tracker.inheritAnd(tra -> r.children().forEach(t -> tra.then(tr -> isSubtype(left, t, tr))));
        }
    }

    private static void subtypeNone(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JNoneType) {
            tracker.set(false);
        } else if (right instanceof JNoneType) {
            tracker.set(left.isNullType() && !(right instanceof JPrimitiveType));
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
            tracker.set(PRIM_SUPERS.get(l.name()).contains(r.name()));
        } else if (left instanceof JPrimitiveType || right instanceof JPrimitiveType) {
            tracker.set(false);
        }
    }

    private static void subtypeWild(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JWildType.Upper l) {
            l.upperBounds().forEach(b -> tracker.then(tr -> isSubtype(b, right, tr)));
        } else if (right instanceof JWildType.Lower r) {
            tracker.inheritAnd(tra -> r.lowerBounds().forEach(b -> tra.then(tr -> isSubtype(left, b, tr))));
        }
    }

    private static void subtypeVar(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JVarType l) {
            tracker.inheritAnd(tra -> l.upperBounds().forEach(b -> tra.then(tr -> isSubtype(b, right, tr))));
        }
    }

    private static void subtypeUnchecked(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) || (!l.hasAnyTypeArguments() && r.hasAnyTypeArguments()))) {
            tracker.then(tr -> isSubtype(l.classReference(), r.classReference(), tracker));
        }
    }

    private static void subtypeRaw(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JClassType r && !l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) {
            tracker.then(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), l.classReference().hasSupertype(r.classReference())));
        }
    }

    private static void subtypeGenericClass(JType left, JType right, JConstraintTracker tracker) {
        if (left instanceof JClassType l && right instanceof JParameterizedClassType r && r.hasAnyTypeArguments()) {
            Optional<JClassType> superTypeOpt = (left instanceof JParameterizedClassType pct ? pct : ((JClassType) left).classReference().parameterizedWithTypeVars())
                    .relativeSupertype(r.classReference());
            if (superTypeOpt.isPresent()) {
                JClassType relative = superTypeOpt.get();

                tracker.inheritAnd(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), true),
                        tr -> typeArgumentsMatch(relative, r, tr),
                        tr -> outerTypesMatch(relative, r, tr));
            } else {
                tracker.then(tr -> tr.with(new JTypeConstraints.Subtype(l.classReference(), r.classReference()), false));
            }
        }
    }

    private static void outerTypesMatch(JClassType left, JClassType right, JConstraintTracker tracker) {
        if (left.hasRelevantOuterType() && right.hasRelevantOuterType()) {
            tracker.and(new JTypeConstraints.OuterTypesMatch(left, right),
                    tr -> isSubtype(left.outerType(), right.outerType(), tracker));
        }
    }

    private static void typeArgumentsMatch(JClassType left, JClassType right, JConstraintTracker tracker) {
        tracker.and(new JTypeConstraints.TypeArgumentsMatch(left, right), tr1 -> {
            if (left.typeArguments().isEmpty() && right.typeArguments().isEmpty()) {
                tracker.set(true);
            } else {
                if (right instanceof JParameterizedClassType pcr && left.typeArguments().size() == right.typeArguments().size()) {
                    for (int i = 0; i < left.typeArguments().size(); i++) {
                        JType ti = left.typeArguments().get(i);
                        JType si = right.typeArguments().get(i);

                        int finalI = i;
                        tracker.and(new JTypeConstraints.TypeArgumentMatch(ti, si), tre -> {
                            if (si instanceof JWildType.Upper siwtu) {
                                pcr.typeParameters().get(finalI).upperBounds().stream().map(pcr.varTypeResolver())
                                        .forEach(argBound -> tracker.then(tr -> isSubtype(ti, argBound, tr)));
                                siwtu.upperBounds()
                                        .forEach(wildBound -> tracker.then(tr -> isSubtype(ti, wildBound, tr)));
                            } else if (si instanceof JWildType.Lower siwtl) {
                                pcr.typeParameters().get(finalI).upperBounds().stream().map(pcr.varTypeResolver())
                                        .forEach(argBound -> tracker.then(tr -> isSubtype(ti, argBound, tr)));
                                siwtl.lowerBounds()
                                        .forEach(wildBound -> tracker.then(tr -> isSubtype(wildBound, ti, tr)));
                            } else {
                                tracker.then(tr -> equal(ti, si, tr));
                            }
                        });
                    }
                } else {
                    tracker.set(false);
                }
            }
        });
    }
}
