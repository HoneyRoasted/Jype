package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JConstantEvaluator;
import honeyroasted.jype.system.solver.JOperatorTypes;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JReduceSwitch extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Switch;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Switch sw = (JExpressionInformation.Switch) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        if (sw.results().isEmpty()) {
            // JLS §15.28.1 a switch expression must have at least one result expression
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        List<ConstraintBranch.Snapshot> children = new ArrayList<>();
        for (JStandaloneType selector : JStandaloneTyping.resolve(sw.selector(), system)) {
            if (!validSelector(selector.type(), sw.patternLabels(), system)) {
                continue;
            }
            if (constraint.middle().isPoly()) {
                // JLS §15.28.1 a switch expression in an assignment or invocation context is a poly expression
                List<Constraint> each = new ArrayList<>();
                for (JExpressionInformation result : sw.results()) {
                    each.add(new JTypeConstraints.ExpressionCompatible(result, constraint.middle(), constraint.right()));
                }
                ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet(), new java.util.LinkedHashMap<>());
                each.forEach(c -> snapshot.constraints().put(c, Constraint.Status.UNKNOWN));
                if (selector.extra() != null) {
                    snapshot.constraints().put(selector.extra(), Constraint.Status.UNKNOWN);
                }
                children.add(selector.branch() == null ? snapshot : JStandaloneTyping.combine(selector.branch(), snapshot));
                continue;
            }
            List<List<JStandaloneType>> combos = combinations(sw.results(), system);
            for (List<JStandaloneType> combo : combos) {
                standaloneType(sw.results(), combo, system).ifPresent(type -> {
                    List<JStandaloneType> operands = new ArrayList<>(combo);
                    operands.add(selector);
                    children.add(JReduceOperands.snapshot(operands, sw, type, constraint));
                });
            }
        }
        if (children.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).divergeBranches(children);
        }
    }

    private static boolean validSelector(JType type, boolean patterns, JTypeSystem system) {
        JTypeConstants c = system.constants();
        if (type instanceof JPrimitiveType p) {
            // JLS §14.11.1 a primitive selector is char, byte, short or int and has no pattern labels
            return !patterns && (p.typeEquals(c.charType()) || p.typeEquals(c.byteType()) || p.typeEquals(c.shortType()) || p.typeEquals(c.intType()));
        }
        // JLS §14.11.1 any reference type
        return !type.typeEquals(c.voidType());
    }

    private static List<List<JStandaloneType>> combinations(List<JExpressionInformation> results, JTypeSystem system) {
        List<List<JStandaloneType>> combos = new ArrayList<>();
        combos.add(new ArrayList<>());
        for (JExpressionInformation r : results) {
            List<JStandaloneType> options = JStandaloneTyping.resolve(r, system);
            List<List<JStandaloneType>> next = new ArrayList<>();
            for (List<JStandaloneType> prefix : combos) {
                for (JStandaloneType option : options) {
                    List<JStandaloneType> extended = new ArrayList<>(prefix);
                    extended.add(option);
                    next.add(extended);
                }
            }
            combos = next;
        }
        return combos;
    }

    private static Optional<JType> standaloneType(List<JExpressionInformation> exprs, List<JStandaloneType> resolved, JTypeSystem system) {
        JTypeConstants c = system.constants();
        List<JType> types = resolved.stream().map(JStandaloneType::type).toList();
        if (types.stream().allMatch(t -> t.typeEquals(types.getFirst()))) {
            // JLS §15.28.1 every result expression has the same type
            return Optional.of(types.getFirst());
        }
        List<Optional<JPrimitiveType>> prims = types.stream().map(t -> JOperatorTypes.asPrimitive(t, system)).toList();
        if (prims.stream().allMatch(p -> p.isPresent() && JOperatorTypes.isBoolean(p.get(), system))) {
            // JLS §15.28.1 boolean results
            return Optional.of(c.booleanType());
        }
        if (prims.stream().allMatch(p -> p.isPresent() && !JOperatorTypes.isBoolean(p.get(), system))) {
            // JLS §5.6 numeric choice context
            return Optional.of(numericChoice(exprs, prims.stream().map(Optional::get).toList(), system));
        }
        Set<JType> boxed = new LinkedHashSet<>();
        for (JType t : types) {
            boxed.add(t instanceof JPrimitiveType p ? p.box() : t);
        }
        // JLS §15.28.1 capture conversion of the lub of the boxed result types
        JType lub = system.operations().findLeastUpperBound(boxed);
        return Optional.of(lub instanceof JClassType ct && lub.isProperType() ? JTypeRelations.capture(ct) : lub);
    }

    private static JType numericChoice(List<JExpressionInformation> exprs, List<JPrimitiveType> prims, JTypeSystem system) {
        JTypeConstants c = system.constants();
        if (prims.stream().anyMatch(p -> p.typeEquals(c.doubleType()))) {
            return c.doubleType();
        } else if (prims.stream().anyMatch(p -> p.typeEquals(c.floatType()))) {
            return c.floatType();
        } else if (prims.stream().anyMatch(p -> p.typeEquals(c.longType()))) {
            return c.longType();
        }
        for (int i = 0; i < prims.size(); i++) {
            if (prims.get(i).typeEquals(c.intType()) && JConstantEvaluator.evaluate(exprs.get(i), system).isEmpty()) {
                // JLS §5.6 a non-constant int expression gives int
                return c.intType();
            }
        }
        for (JPrimitiveType narrow : List.of(c.charType(), c.shortType(), c.byteType())) {
            if (prims.stream().anyMatch(p -> p.typeEquals(narrow)) && fitsAll(exprs, prims, narrow, system)) {
                // JLS §5.6 char, short or byte with representable int constants
                return narrow;
            }
        }
        return c.intType();
    }

    private static boolean fitsAll(List<JExpressionInformation> exprs, List<JPrimitiveType> prims, JPrimitiveType narrow, JTypeSystem system) {
        JTypeConstants c = system.constants();
        for (int i = 0; i < prims.size(); i++) {
            JPrimitiveType p = prims.get(i);
            if (p.typeEquals(narrow) || (narrow.typeEquals(c.shortType()) && p.typeEquals(c.byteType()))) {
                continue;
            }
            if (!p.typeEquals(c.intType())) {
                return false;
            }
            Optional<JConstantEvaluator.Value> v = JConstantEvaluator.evaluate(exprs.get(i), system);
            if (v.isEmpty() || !(v.get().value() instanceof Integer x)) {
                return false;
            }
            boolean fits = narrow.typeEquals(c.charType()) ? x >= Character.MIN_VALUE && x <= Character.MAX_VALUE
                    : narrow.typeEquals(c.shortType()) ? x >= Short.MIN_VALUE && x <= Short.MAX_VALUE : x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE;
            if (!fits) {
                return false;
            }
        }
        return true;
    }
}
