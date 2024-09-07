package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JReduceSubtype extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        if (left.isProperType() && right.isProperType()) {
            branch.set(constraint, Constraint.Status.known(left.typeSystem().operations().isSubtype(left, right)));
        } else if (left.isNullType()) {
            branch.set(constraint, Constraint.Status.TRUE);
        } else if (right.isNullType()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else if (left instanceof JMetaVarType || right instanceof JMetaVarType) {
            branch.set(constraint, Constraint.Status.ASSUMED);
        } else if (right instanceof JVarType vt) {
            if (left instanceof JIntersectionType it && it.typeContains(vt)) {
                branch.set(constraint, Constraint.Status.TRUE);
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        } else if (right instanceof JMetaVarType mvt) {
            if (left instanceof JIntersectionType it && it.typeContains(mvt)) {
                branch.set(constraint, Constraint.Status.TRUE);
            } else if (!mvt.lowerBounds().isEmpty()) {
                branch.drop(constraint).add(new JTypeConstraints.Subtype(left, mvt.lowerBound()));
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        } else if (right instanceof JIntersectionType it) {
            branch.drop(constraint);
            it.children().forEach(t -> branch.add(new JTypeConstraints.Subtype(left, t)));
        } else if (right instanceof JClassType ct) {
            if (ct.hasAnyTypeArguments()) {
                Optional<JClassType> supertypeOpt = identifySuperclass(ct.classReference(), left);
                if (supertypeOpt.isPresent()) {
                    JClassType supertype = supertypeOpt.get();
                    if (supertype.hasAnyTypeArguments() && supertype.typeArguments().size() == ct.typeArguments().size()) {
                        branch.drop(constraint);
                        for (int i = 0; i < ct.typeArguments().size(); i++) {
                            JType ti = supertype.typeArguments().get(i);
                            JType si = ct.typeArguments().get(i);

                            branch.add(new JTypeConstraints.Contains(ti, si));
                        }
                    } else {
                        branch.set(constraint, Constraint.Status.FALSE);
                    }
                } else {
                    branch.set(constraint, Constraint.Status.FALSE);
                }
            } else {
                branch.set(constraint, Constraint.Status.known(left.typeSystem().operations().isSubtype(left, right)));
            }
        } else if (right instanceof JArrayType at) {
            if (left instanceof JArrayType lat) {
                if (at.component() instanceof JPrimitiveType && lat.component() instanceof JPrimitiveType) {
                    branch.drop(constraint).add(new JTypeConstraints.Equal(lat.component(), at.component()), Constraint.Status.ASSUMED);
                } else {
                    branch.drop(constraint).add(new JTypeConstraints.Subtype(lat.component(), at.component()), Constraint.Status.ASSUMED);
                }
            } else {
                Set<JType> arr = findMostSpecificArrayTypes(left);
                if (arr.isEmpty()) {
                    branch.set(constraint, Constraint.Status.FALSE);
                } else {
                    branch.drop(constraint).diverge(arr.stream().map(st -> new JTypeConstraints.Subtype(st, at)).toList());
                }
            }
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private Optional<JClassType> identifySuperclass(JClassReference target, JType type) {
        if (type instanceof JClassType ct) {
            Optional<JClassType> found = ct.relativeSupertype(ct);
            return found;
        } else {
            for (JType supertype : type.knownDirectSupertypes()) {
                if (supertype instanceof JClassType ct) {
                    Optional<JClassType> found = ct.relativeSupertype(target);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }

            for (JType supertype : type.knownDirectSupertypes()) {
                Optional<JClassType> found = identifySuperclass(target.classReference(), supertype);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
    }

    private Set<JType> findMostSpecificArrayTypes(JType type) {
        if (type instanceof JArrayType) {
            return Set.of(type);
        }

        Set<JType> current = new HashSet<>(type.knownDirectSupertypes());
        while (!current.isEmpty() && current.stream().allMatch(t -> t instanceof JArrayType)) {
            Set<JType> arrayTypes = current.stream().filter(t -> t instanceof JArrayType).collect(Collectors.toSet());
            if (!arrayTypes.isEmpty()) {
                current = arrayTypes;
            } else {
                Set<JType> next = new HashSet<>();
                current.forEach(t -> next.addAll(t.knownDirectSupertypes()));
                current = next;
            }
        }

        return type.typeSystem().operations().findMostSpecificTypes(current);
    }
}
