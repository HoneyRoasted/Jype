package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JFieldLookup;
import honeyroasted.jype.system.solver.JOperatorTypes;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JReduceAssignment extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && (constraint.left() instanceof JExpressionInformation.Assignment || constraint.left() instanceof JExpressionInformation.IncrementDecrement);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JTypeSystem system = constraint.right().typeSystem();
        JExpressionInformation variable = constraint.left() instanceof JExpressionInformation.Assignment a ? a.variable() : ((JExpressionInformation.IncrementDecrement) constraint.left()).variable();
        if (!isAssignableVariable(variable, system)) {
            // JLS §15.26 the left-hand operand must be a non-final variable
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        List<ConstraintBranch.Snapshot> children = new ArrayList<>();
        for (JStandaloneType var : variableTypes(variable, system)) {
            JType varType = var.type();
            if (constraint.left() instanceof JExpressionInformation.IncrementDecrement incdec) {
                // JLS §15.14.2, §15.15.1 the operand must be convertible to a numeric type
                if (JOperatorTypes.asPrimitive(varType, system).filter(p -> !JOperatorTypes.isBoolean(p, system)).isEmpty()) {
                    continue;
                }
                children.add(JReduceOperands.snapshot(List.of(var), incdec, varType, constraint));
                continue;
            }
            JExpressionInformation.Assignment assignment = (JExpressionInformation.Assignment) constraint.left();
            if (assignment.operator() == null) {
                // JLS §15.26.1 ‹e → T› in an assignment context
                children.add(JReduceOperands.snapshot(List.of(var), List.of(new JTypeConstraints.ExpressionCompatible(assignment.value(), JTypeConstraints.Compatible.Context.ASSIGNMENT, varType)), assignment, varType, constraint));
                continue;
            }
            for (JStandaloneType value : JStandaloneTyping.resolve(assignment.value(), system)) {
                Optional<JType> opType = JExpressionInformation.BinaryOperator.resultType(assignment.operator(), varType, value.type(), system);
                // JLS §15.26.2 E1 op= E2 is E1 = (T) ((E1) op (E2))
                if (opType.isPresent() && JTypeRelations.isCastable(opType.get(), varType)) {
                    children.add(JReduceOperands.snapshot(List.of(var, value), assignment, varType, constraint));
                }
            }
        }
        if (children.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).divergeBranches(children);
        }
    }

    private static List<JStandaloneType> variableTypes(JExpressionInformation variable, JTypeSystem system) {
        List<JStandaloneType> result = new ArrayList<>();
        if (variable instanceof JExpressionInformation.LocalVariable local) {
            result.add(new JStandaloneType(null, null, local.type()));
        } else if (variable instanceof JExpressionInformation.GetField<?> gf && gf.source() instanceof JClassReference ref) {
            JFieldLookup.staticAccess(ref, gf.name(), gf.declaring()).ifPresent(f -> result.add(new JStandaloneType(null, null, f.field().type())));
        } else if (variable instanceof JExpressionInformation.GetField<?> gf && gf.source() instanceof JExpressionInformation expr) {
            for (JStandaloneType receiver : JStandaloneTyping.resolve(expr, system)) {
                JType site = receiver.type() instanceof honeyroasted.jype.type.JClassType ct ? JTypeRelations.capture(ct) : receiver.type();
                for (honeyroasted.jype.type.JClassType ct : honeyroasted.jype.system.solver.JMemberLookup.findClassTypes(site)) {
                    Optional<JFieldReference> field = JFieldLookup.member(ct.classReference(), gf.name());
                    if (field.isPresent()) {
                        // JLS §15.26.1 the variable has its declared (uncaptured) member type
                        result.add(new JStandaloneType(receiver.branch(), receiver.extra(), JFieldLookup.memberType(ct, field.get())));
                        break;
                    }
                }
            }
        } else if (variable instanceof JExpressionInformation.ArrayAccess access) {
            for (JStandaloneType array : JStandaloneTyping.resolve(access.array(), system)) {
                if (array.type() instanceof honeyroasted.jype.type.JArrayType at) {
                    JTypeConstraints.ExpressionCompatible index = new JTypeConstraints.ExpressionCompatible(access.index(), JTypeConstraints.Compatible.Context.ASSIGNMENT, system.constants().intType());
                    result.add(new JStandaloneType(array.branch(), index, at.component()));
                }
            }
        }
        return result;
    }

    private static boolean isAssignableVariable(JExpressionInformation variable, JTypeSystem system) {
        if (variable instanceof JExpressionInformation.LocalVariable local) {
            return !local.isFinal();
        } else if (variable instanceof JExpressionInformation.ArrayAccess) {
            return true;
        } else if (variable instanceof JExpressionInformation.GetField<?> gf) {
            Optional<JFieldReference> field = Optional.empty();
            if (gf.source() instanceof JClassReference ref) {
                field = JFieldLookup.staticAccess(ref, gf.name(), gf.declaring()).map(JFieldLookup.Found::field);
            } else if (gf.source() instanceof JExpressionInformation expr) {
                for (JStandaloneType receiver : JStandaloneTyping.resolve(expr, system)) {
                    field = JFieldLookup.instanceAccess(receiver.type(), gf.name(), gf.declaring()).map(JFieldLookup.Found::field);
                    if (field.isPresent() || receiver.type() instanceof honeyroasted.jype.type.JArrayType) {
                        break;
                    }
                }
                if (field.isEmpty()) {
                    // JLS §10.7 an array length is final
                    return false;
                }
            }
            return field.isPresent() && !field.get().hasModifier(AccessFlag.FINAL);
        }
        return false;
    }
}
