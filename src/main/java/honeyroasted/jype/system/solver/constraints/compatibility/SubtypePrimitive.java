package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class SubtypePrimitive extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
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
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && (left instanceof PrimitiveType || right instanceof PrimitiveType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left instanceof PrimitiveType && right instanceof PrimitiveType) {
            branch.setStatus(constraint, Constraint.Status.known(PRIM_SUPERS.get(((PrimitiveType) left).name()).contains(((PrimitiveType) right).name())));
        } else {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        }
    }
}
