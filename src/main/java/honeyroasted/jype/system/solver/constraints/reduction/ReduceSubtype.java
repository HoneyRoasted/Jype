package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReduceSubtype extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left.isProperType() && right.isProperType()) {
            branch.setStatus(constraint, Constraint.Status.known(left.typeSystem().operations().isSubtype(left, right)));
        } else if (left.isNullType()) {
            branch.setStatus(constraint, Constraint.Status.TRUE);
        } else if (right.isNullType()) {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        } else if (left instanceof MetaVarType || right instanceof MetaVarType) {
            branch.setStatus(constraint, Constraint.Status.ASSUMED);
        } else if (right instanceof VarType vt) {
            if (left instanceof IntersectionType it && it.typeContains(vt)) {
                branch.setStatus(constraint, Constraint.Status.TRUE);
            } else {
                branch.setStatus(constraint, Constraint.Status.FALSE);
            }
        } else if (right instanceof MetaVarType mvt) {
            if (left instanceof IntersectionType it && it.typeContains(mvt)) {
                branch.setStatus(constraint, Constraint.Status.TRUE);
            } else if (!mvt.lowerBounds().isEmpty()) {
                branch.drop(constraint).add(new TypeConstraints.Subtype(left, mvt.lowerBound()));
            } else {
                branch.setStatus(constraint, Constraint.Status.FALSE);
            }
        } else if (right instanceof IntersectionType it) {
            branch.drop(constraint);
            it.children().forEach(t -> branch.add(new TypeConstraints.Subtype(left, t)));
        } else if (right instanceof ClassType ct) {
            if (ct.hasAnyTypeArguments()) {
                Optional<ClassType> supertypeOpt = identifySuperclass(ct.classReference(), left);
                if (supertypeOpt.isPresent()) {
                    ClassType supertype = supertypeOpt.get();
                    if (supertype.hasAnyTypeArguments() && supertype.typeArguments().size() == ct.typeArguments().size()) {
                        branch.drop(constraint);
                        for (int i = 0; i < ct.typeArguments().size(); i++) {
                            Type ti = supertype.typeArguments().get(i);
                            Type si = ct.typeArguments().get(i);

                            branch.add(new TypeConstraints.Contains(ti, si));
                        }
                    } else {
                        branch.setStatus(constraint, Constraint.Status.FALSE);
                    }
                } else {
                    branch.setStatus(constraint, Constraint.Status.FALSE);
                }
            } else {
                branch.setStatus(constraint, Constraint.Status.known(left.typeSystem().operations().isSubtype(left, right)));
            }
        } else if (right instanceof ArrayType at) {
            if (left instanceof ArrayType lat) {
                if (at.component() instanceof PrimitiveType && lat.component() instanceof PrimitiveType) {
                    branch.drop(constraint).add(new TypeConstraints.Equal(lat.component(), at.component()), Constraint.Status.ASSUMED);
                } else {
                    branch.drop(constraint).add(new TypeConstraints.Subtype(lat.component(), at.component()), Constraint.Status.ASSUMED);
                }
            } else {
                Set<Type> arr = findMostSpecificArrayTypes(left);
                if (arr.isEmpty()) {
                    branch.setStatus(constraint, Constraint.Status.FALSE);
                } else {
                    branch.drop(constraint).diverge(arr.stream().map(st -> new TypeConstraints.Subtype(st, at)).toList());
                }
            }
        } else {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        }
    }

    private Optional<ClassType> identifySuperclass(ClassReference target, Type type) {
        if (type instanceof ClassType ct) {
            Optional<ClassType> found = ct.relativeSupertype(ct);
            return found;
        } else {
            for (Type supertype : type.knownDirectSupertypes()) {
                if (supertype instanceof ClassType ct) {
                    Optional<ClassType> found = ct.relativeSupertype(target);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }

            for (Type supertype : type.knownDirectSupertypes()) {
                Optional<ClassType> found = identifySuperclass(target.classReference(), supertype);
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
