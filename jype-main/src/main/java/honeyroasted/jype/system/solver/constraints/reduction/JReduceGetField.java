package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JReduceGetField extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.GetField<?> get &&
                (get.source() instanceof JClassReference || get.source() instanceof JExpressionInformation);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.GetField<?> getField = (JExpressionInformation.GetField<?>) constraint.left();
        JType targetType = constraint.right();
        JTypeSystem system = targetType.typeSystem();

        JClassReference declaring = getField.declaring();
        String name = getField.name();

        JFieldReference field = null;
        if (getField.source() instanceof JClassReference ref) {
            field = getAllFields(ref, jfr -> jfr.hasModifier(AccessFlag.STATIC) && jfr.location().name().equals(name) && jfr.outerClass().accessFrom(declaring).canAccess(jfr.access()))
                    .stream().findFirst().orElse(null);
        } else if (getField.source() instanceof JExpressionInformation expr) {
            if (expr.isSimplyTyped()) {
                JType type = expr.getSimpleType(system, branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get();
                field = findClassTypes(type).stream()
                        .flatMap(ct -> getAllFields(ct.classReference(), jfr -> !jfr.hasModifier(AccessFlag.STATIC) && jfr.location().name().equals(name) && jfr.outerClass().accessFrom(declaring).canAccess(jfr.access())).stream())
                        .findFirst().orElse(null);

                if (field != null && type instanceof JParameterizedClassType pct) {
                    field = (JFieldReference) pct.varTypeResolver().visit(field);
                }
            } else {
                JMetaVarType callTarget = system.typeFactory().newMetaVarType("CALL_TARGET");
                ConstraintTree solved = system.operations().inferenceSolver()
                        .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, callTarget))
                        .solve();

                List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
                solved.branches().forEach(childBranch -> {
                    if (childBranch.status().isTrue()) {
                        JType inst = JResolveBounds.findInstantiation(callTarget, childBranch);
                        childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(callTarget)); //Drop variable since it is no longer needed
                        findClassTypes(inst).forEach(ct -> getAllFields(ct.classReference(), jfr -> !jfr.hasModifier(AccessFlag.STATIC) && jfr.location().name().equals(name) && jfr.outerClass().accessFrom(declaring).canAccess(jfr.access())).forEach(ref -> {
                            if (ct instanceof JParameterizedClassType pct) {
                                ref = (JFieldReference) pct.varTypeResolver().visit(ref);
                            }

                            newChildren.add(combine(childBranch, createGetField(constraint, getField, ref)));
                        }));
                    }
                });

                if (!newChildren.isEmpty()) {
                    branch.drop(constraint).divergeBranches(newChildren);
                } else {
                    branch.set(constraint, Constraint.Status.FALSE);
                }
                return;
            }
        }

        if (field == null) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).add(new JTypeConstraints.Compatible(field.type(), constraint.middle(), constraint.right()));
        }
    }

    private static ConstraintBranch.Snapshot createGetField(JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.GetField<?> getField, JFieldReference ref) {
        Map<Constraint, Constraint.Status> getfield = new LinkedHashMap<>();
        getfield.put(new JTypeConstraints.Compatible(ref.type(), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenField(getField, ref)), getfield);
    }

    public static Collection<JFieldReference> getAllFields(JClassReference ref, Predicate<JFieldReference> filter) {
        List<JFieldReference> result = new ArrayList<>();

        Set<JClassType> building = Set.of(ref);
        Set<JClassType> next = new LinkedHashSet<>();

        while (!building.isEmpty()) {
            for (JClassType curr : building) {
                curr.classReference().declaredFields()
                        .stream().filter(filter)
                        .forEach(result::add);

                if (curr.superClass() != null) next.add(curr.superClass());
                next.addAll(curr.interfaces());
            }

            building = next;
            next = new LinkedHashSet<>();
        }

        return result;
    }

    public static Set<JClassType> findClassTypes(JType type) {
        //TODO figure out the right way to do this
        if (type instanceof JClassType ct) {
            return Set.of(ct);
        } else {
            Set<JType> working = Set.of(type);
            Set<JClassType> attempt;

            do {
                attempt = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).filter(t -> t instanceof JClassType)
                        .map(t -> (JClassType) t).collect(Collectors.toCollection(LinkedHashSet::new));
                working = working.stream().flatMap(t -> t.knownDirectSupertypes().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
            } while (attempt.isEmpty() && !working.isEmpty());

            return attempt;
        }
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
