package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class SubtypePrimitive implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
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

    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return node.isLeaf() && (left instanceof PrimitiveType || right instanceof PrimitiveType);
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = instanceContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left instanceof PrimitiveType && right instanceof PrimitiveType) {
            node.overrideStatus(PRIM_SUPERS.get(((PrimitiveType) left).name()).contains(((PrimitiveType) right).name()));
        } else {
            node.overrideStatus(false);
        }
    }
}
