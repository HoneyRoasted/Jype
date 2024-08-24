package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.TrackedConstraint;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReduceSubtype implements ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    public void process(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(node);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left.isProperType() && right.isProperType()) {
            left.typeSystem().operations().compatibilityApplier()
                    .process(TrackedConstraint.of(constraint, node.trackedConstraint()).createLeaf());
        } else if (left.isNullType()) {
            node.overrideStatus(true);
        } else if (right.isNullType()) {
            node.overrideStatus(false);
        } else if (left instanceof MetaVarType || right instanceof MetaVarType) {
            node.overrideStatus(true);
        } else if (right instanceof VarType vt) {
            if (left instanceof IntersectionType it && it.typeContains(vt)) {
                node.overrideStatus(true);
            } else {
                node.overrideStatus(false);
            }
        } else if (right instanceof MetaVarType mvt) {
            if (left instanceof IntersectionType it && it.typeContains(mvt)) {
                node.overrideStatus(true);
            } else if (!mvt.lowerBounds().isEmpty()) {
                node.expand(ConstraintNode.Operation.AND, new TypeConstraints.Subtype(left, mvt.lowerBound()))
                        .overrideStatus(true);
            } else {
                node.overrideStatus(false);
            }
        } else if (right instanceof IntersectionType it) {
            node.expand(ConstraintNode.Operation.AND, it.children().stream().map(t -> new TypeConstraints.Subtype(left, t)).toArray(Constraint[]::new))
                    .overrideStatus(true);
        } else if (right instanceof ClassType ct) {
            if (ct.hasAnyTypeArguments()) {
                TrackedConstraint classTypeMatch = new TypeConstraints.Subtype(left, ct.classReference()).tracked(node.trackedConstraint());

                Optional<ClassType> supertypeOpt = identifySuperclass(ct.classReference(), left, classTypeMatch);
                if (supertypeOpt.isPresent()) {
                    ClassType supertype = supertypeOpt.get();
                    if (supertype.hasAnyTypeArguments() && supertype.typeArguments().size() == ct.typeArguments().size()) {
                        Set<ConstraintNode> newChildren = new LinkedHashSet<>();
                        for (int i = 0; i < ct.typeArguments().size(); i++) {
                            Type ti = supertype.typeArguments().get(i);
                            Type si = ct.typeArguments().get(i);

                            newChildren.add(new TypeConstraints.Contains(ti, si).tracked(node.trackedConstraint()).createLeaf());
                        }

                        node.expand(ConstraintNode.Operation.AND, newChildren);
                    } else {
                        node.overrideStatus(false);
                    }
                } else {
                    node.overrideStatus(false);
                }
            } else {
                node.expandInPlace(ConstraintNode.Operation.OR).attach(left.typeSystem().operations().compatibilityApplier()
                        .process(node.trackedConstraint().createLeaf(), new PropertySet().inheritUnique(context)));
            }
        } else if (right instanceof ArrayType at) {
            if (left instanceof ArrayType lat) {
                if (at.component() instanceof PrimitiveType && lat.component() instanceof PrimitiveType) {
                    node.expand(ConstraintNode.Operation.AND, new TypeConstraints.Equal(lat.component(), at.component()))
                            .overrideStatus(true);
                } else {
                    node.expand(ConstraintNode.Operation.AND, new TypeConstraints.Subtype(lat.component(), at.component()))
                            .overrideStatus(true);
                }
            } else {
                Set<Type> arr = findMostSpecificArrayTypes(left);
                if (arr.isEmpty()) {
                    node.overrideStatus(false);
                } else {
                    node.expand(ConstraintNode.Operation.OR, arr.stream().map(st -> new TypeConstraints.Subtype(st, at)).toArray(Constraint[]::new))
                            .overrideStatus(true);
                }
            }
        } else {
            node.overrideStatus(false);
        }
    }

    private Optional<ClassType> identifySuperclass(ClassReference target, Type type, TrackedConstraint constraint) {
        if (type instanceof ClassType ct) {
            Optional<ClassType> found = ct.relativeSupertype(ct);
            TrackedConstraint.of(new TypeConstraints.Equal(ct.classReference(), target), constraint);
            return found;
        } else {
            for (Type supertype : type.knownDirectSupertypes()) {
                if (supertype instanceof ClassType ct) {
                    Optional<ClassType> found = ct.relativeSupertype(target);
                    if (found.isPresent()) {
                        TrackedConstraint.of(new TypeConstraints.Equal(ct.classReference(), target), constraint);
                        return found;
                    }
                }
            }

            for (Type supertype : type.knownDirectSupertypes()) {
                TrackedConstraint classTypeMatch = TrackedConstraint.of(new TypeConstraints.Subtype(supertype, target), constraint);

                Optional<ClassType> found = identifySuperclass(target.classReference(), supertype, classTypeMatch);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
    }

    private Set<Type> findMostSpecificArrayTypes(Type type) {
        if (type instanceof ArrayType) {
            return Set.of(type);
        }

        Set<Type> current = new HashSet<>(type.knownDirectSupertypes());
        while (!current.isEmpty() && current.stream().allMatch(t -> t instanceof ArrayType)) {
            Set<Type> arrayTypes = current.stream().filter(t -> t instanceof ArrayType).collect(Collectors.toSet());
            if (!arrayTypes.isEmpty()) {
                current = arrayTypes;
            } else {
                Set<Type> next = new HashSet<>();
                current.forEach(t -> next.addAll(t.knownDirectSupertypes()));
                current = next;
            }
        }

        return type.typeSystem().operations().findMostSpecificTypes(current);
    }
}
