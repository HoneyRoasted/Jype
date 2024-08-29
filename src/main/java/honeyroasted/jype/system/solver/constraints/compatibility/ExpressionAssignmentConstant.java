package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintLeaf;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;


public class ExpressionAssignmentConstant implements ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type right = mapper.apply(constraint.right());

        return node.isLeaf() && constraint.middle() == ASSIGNMENT && constraint.left() instanceof ExpressionInformation.Constant && right.isProperType();
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.ExpressionCompatible constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type right = mapper.apply(constraint.right());

        Set<ConstraintLeaf> newChildren = new LinkedHashSet<>();

        ExpressionInformation.Constant constantExpression = (ExpressionInformation.Constant) constraint.left();
        Type subtype = constantExpression.getSimpleType(right.typeSystem(), mapper).get();

        TypeConstants c = subtype.typeSystem().constants();

        Type target = right;
        Object val = constantExpression.value();

        if (subtype.typeEquals(c.byteType()) || subtype.typeEquals(c.shortType()) || subtype.typeEquals(c.charType()) || subtype.typeEquals(c.intType())) {
            if (target.typeEquals(c.charType()) || target.typeEquals(c.charBox())) {
                if (fits(val, Character.MIN_VALUE, Character.MAX_VALUE)) {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.TRUE));
                } else {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.FALSE));
                }
            } else if (target.typeEquals(c.byteType()) || target.typeEquals(c.byteBox())) {
                if (fits(val, Byte.MIN_VALUE, Byte.MAX_VALUE)) {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.TRUE));
                } else {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.FALSE));
                }
            } else if (target.typeEquals(c.shortType()) || target.typeEquals(c.shortBox())) {
                if (fits(val, Short.MIN_VALUE, Short.MAX_VALUE)) {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.TRUE));
                } else {
                    newChildren.add(new TypeConstraints.NarrowConstant(constantExpression, target).createLeaf().setStatus(ConstraintNode.Status.FALSE));
                }
            }
        }

        newChildren.add(new TypeConstraints.Compatible(subtype, LOOSE_INVOCATION, right).createLeaf());

        node.expand(ConstraintNode.Operation.OR, newChildren, false);

    }

    private static boolean fits(Object obj, long min, long max) {
        if (obj instanceof Number n) {
            return min <= n.longValue() && n.longValue() <= max;
        } else if (obj instanceof Character c) {
            return min <= c && c <= max;
        }
        return false;
    }
}
