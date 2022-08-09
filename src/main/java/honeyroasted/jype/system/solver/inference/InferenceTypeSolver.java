package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.system.solver.force.ForceResolveTypeSolver;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InferenceTypeSolver extends AbstractTypeSolver implements TypeSolver {

    public InferenceTypeSolver(TypeSystem system) {
        super(system,
                TypeConstraint.True.class, TypeConstraint.False.class,
                InferenceConstraint.Compatible.class, InferenceConstraint.Subtype.class,
                InferenceConstraint.Contains.class, InferenceConstraint.Equals.class,
                InferenceConstraint.Throws.class);
    }

    @Override
    public TypeSolution solve() {
        List<TypeConstraint> constraints = this.constraints()
                .stream().map(c -> c instanceof InferenceConstraint ic ? ic.flatten() : c).toList();

        Set<InferenceBounds> boundSet = new HashSet<>();

        Set<TypeParameter> parameters = new HashSet<>();
        constraints.forEach(c -> {
            if (c instanceof InferenceConstraint.Compatible cmp) {
                addParameters(parameters, cmp.left(), cmp.right());
            } else if (c instanceof InferenceConstraint.Subtype sub) {
                addParameters(parameters, sub.subtype(), sub.parent());
            } else if (c instanceof InferenceConstraint.Contains con) {
                addParameters(parameters, con.contained(), con.container());
            } else if (c instanceof InferenceConstraint.Equals eql) {
                addParameters(parameters, eql.left(), eql.right());
            } else if (c instanceof InferenceConstraint.Throws thr) {
                addParameters(parameters, thr.type());
            }
        });

        parameters.forEach(prm -> {
            boundSet.add(new InferenceBounds.Subtype(prm, prm.bound()));

            if (!prm.bound().isProperType()) {
                boundSet.add(new InferenceBounds.Subtype(prm, this.system.OBJECT));
            }
        });


        return null;
    }

    private void pass(List<TypeConstraint> constraints, List<TypeConstraint> newConstraints, Set<InferenceBounds> boundsSet) {
        for (TypeConstraint constraint : constraints) {
            if (constraint instanceof InferenceConstraint.Compatible cmp) {
                pass(cmp, newConstraints, boundsSet);
            } else if (constraint instanceof InferenceConstraint.Subtype sub) {
                pass(sub, newConstraints, boundsSet);
            }
        }
    }

    private TypeVerification pass(InferenceConstraint.Compatible compatible, List<TypeConstraint> newConstraints, Set<InferenceBounds> boundsSet) {
        if (compatible.left().isProperType() && compatible.right().isProperType()) {
            return new ForceResolveTypeSolver(this.system)
                    .constrain(new TypeConstraint.Bound(compatible.left(), compatible.right()))
                    .solve()
                    .verification();
        } else if (compatible.left() instanceof TypeClass left && compatible.right() instanceof TypeClass right) {
            TypeVerification verification = checkUnchecked(compatible, left, right);
            if (verification != null) {
                return verification;
            }
        } else if (compatible.left() instanceof TypeArray leftArr && compatible.right() instanceof TypeArray rightArr &&
                    leftArr.element() instanceof TypeClass left && rightArr.element() instanceof TypeClass right) {
            TypeVerification verification =  checkUnchecked(compatible, left, right);
            if (verification != null) {
                return verification;
            }
        }


        if (compatible.left() instanceof TypePrimitive prim) {
            newConstraints.add(new InferenceConstraint.Compatible(this.system.box(prim), compatible.right()));
        } else if (compatible.right() instanceof TypePrimitive prim) {
            newConstraints.add(new InferenceConstraint.Equals(compatible.left(), this.system.box(prim)));
        } else {
            newConstraints.add(new InferenceConstraint.Subtype(compatible.left(), compatible.right()));
        }
        return TypeVerification.success(compatible);
    }

    private TypeVerification checkUnchecked(InferenceConstraint.Compatible compatible, TypeClass left, TypeClass right) {
        Optional<TypeClass> parentOpt = left.parent(right.declaration());
        if (parentOpt.isPresent()) {
            TypeClass parent = parentOpt.get();

            if (parent.arguments().isEmpty()) {
                return new ForceResolveTypeSolver(this.system)
                        .constrain(new TypeConstraint.Bound(compatible.left(), compatible.right()))
                        .solve()
                        .verification();
            }
        }

        return null;
    }

    private TypeVerification pass(InferenceConstraint.Subtype subtype, List<TypeConstraint> newConstraints, Set<InferenceBounds> boundsSet) {
        if (subtype.subtype().isProperType() && subtype.parent().isProperType()) {
            return new ForceResolveTypeSolver(this.system)
                    .constrain(new TypeConstraint.Bound(subtype.subtype(), subtype.parent()))
                    .solve()
                    .verification();
        } else if (subtype.subtype() instanceof TypeNull) {
            return TypeVerification.success(subtype);
        } else if (subtype.parent() instanceof TypeNull) {
            return TypeVerification.failure(subtype);
        } else if (subtype.subtype() instanceof TypeParameter || subtype.parent() instanceof TypeParameter) {
            boundsSet.add(new InferenceBounds.Subtype(subtype.subtype(), subtype.parent()));
            return TypeVerification.success(subtype);
        } else {
            //TODO
            return null;
        }
    }

    private TypeVerification pass(InferenceConstraint.Contains constraint, List<TypeConstraint> newConstraints, Set<InferenceBounds> boundsSet) {
        if (!constraint.container().isWildcard()) {
            if (constraint.contained().isWildcard()) {
                return TypeVerification.failure(constraint);
            } else {
                newConstraints.add(new TypeConstraint.Equal(constraint.contained(), constraint.container()));
                return TypeVerification.success(constraint);
            }
        } else if (constraint.container() instanceof TypeOut out) {
            if (out.bound().equals(this.system.OBJECT)) { // ?
                return TypeVerification.success(constraint);
            } else if (constraint.contained().isWildcard()) {
                if (constraint.contained() instanceof TypeOut cout) {
                    newConstraints.add(new InferenceConstraint.Subtype(cout.bound(), out.bound()));
                } else if (constraint.contained() instanceof TypeIn cin) {
                    newConstraints.add(new TypeConstraint.Equal(this.system.OBJECT, out.bound()));
                }
            } else {
                newConstraints.add(new InferenceConstraint.Subtype(constraint.contained(), out.bound()));
            }
        } else if (constraint.container() instanceof TypeIn in) {
            if (constraint.contained().isWildcard()) {
                if (constraint.contained() instanceof TypeIn cin) {
                    newConstraints.add(new InferenceConstraint.Subtype(in.bound(), cin.bound()));
                } else {
                    return TypeVerification.failure(constraint);
                }
            } else {
                newConstraints.add(new InferenceConstraint.Subtype(in.bound(), constraint.contained()));
            }
        }

        return TypeVerification.success(constraint);
    }

    private void addParameters(Set<TypeParameter> parameters, TypeConcrete... types) {
        for (TypeConcrete type : types) {
            type.forEach(c -> {
                if (c instanceof TypeParameter prm) {
                    parameters.add(prm);
                }
            });
        }
    }

}
