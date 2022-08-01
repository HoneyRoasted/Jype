package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ForceResolveTypeSolver extends AbstractTypeSolver {

    
    public ForceResolveTypeSolver(TypeSystem system) {
        super(system);
    }

    @Override
    public TypeSolution solve() {
        List<TypeConstraint> constraints = this.constraints.stream().map(TypeConstraint::flatten).toList();

        if (constraints.isEmpty()) {
            return new TypeSolution(new TypeContext(), root().flatten(), Collections.emptyList(), true);
        } else {
            List<TypeConstraint> unverifiable = new ArrayList<>();
            for (TypeConstraint constraint : constraints) {
                TypeConstraint resolved = forceResolve(constraint);
                if (!(resolved instanceof TypeConstraint.True)) {
                    unverifiable.add(resolved);
                }
            }

            return new TypeSolution(new TypeContext(), root().flatten(), unverifiable, unverifiable.isEmpty());
        }
    }

    private TypeConstraint forceResolve(TypeConstraint constraint) {
        if (constraint instanceof TypeConstraint.And and) {
            return forceResolve(and);
        } else if (constraint instanceof TypeConstraint.Or or) {
            return forceResolve(or);
        } else if (constraint instanceof TypeConstraint.Equal equal) {
            return forceResolve(equal);
        } else if (constraint instanceof TypeConstraint.Bound bound) {
            return forceResolve(bound);
        } else if (constraint instanceof TypeConstraint.Not not) {
            return forceResolve(not);
        } else {
            return constraint;
        }
    }

    private TypeConstraint forceResolve(TypeConstraint.And and) {
        List<TypeConstraint> resolved = and.constraints().stream().map(this::forceResolve).toList();
        if (resolved.stream().anyMatch(c -> c instanceof TypeConstraint.False)) {
            return TypeConstraint.FALSE;
        } else if (resolved.stream().allMatch(c -> c instanceof TypeConstraint.True)) {
            return TypeConstraint.TRUE;
        } else if (resolved.size() == 1) {
            return resolved.get(0);
        }

        return new TypeConstraint.And(resolved);
    }

    private TypeConstraint forceResolve(TypeConstraint.Or or) {
        List<TypeConstraint> resolved = or.constraints().stream().map(this::forceResolve).toList();
        if (resolved.stream().anyMatch(c -> c instanceof TypeConstraint.True)) {
            return TypeConstraint.TRUE;
        } else if (resolved.stream().allMatch(c -> c instanceof TypeConstraint.False)) {
            return TypeConstraint.FALSE;
        } else if (resolved.size() == 1) {
            return resolved.get(0);
        }

        return new TypeConstraint.Or(resolved);
    }

    private TypeConstraint forceResolve(TypeConstraint.Equal equal) {
        return equal.left().flatten().equals(equal.right().flatten()) ? TypeConstraint.TRUE : TypeConstraint.FALSE;
    }

    private TypeConstraint forceResolve(TypeConstraint.Bound bound) {
        if (bound.subtype().flatten().equals(bound.parent().flatten())) {
            return TypeConstraint.TRUE;
        } else if (bound.subtype() instanceof TypeParameter a && bound.parent() instanceof TypeParameter b) {
            return forceResolve(a.bound().assignabilityTo(b.bound(), this.system));
        } else if (bound.subtype() instanceof TypeParameter prm) {
            return forceResolve(prm.bound().assignabilityTo(bound.parent(), this.system));
        } else if (bound.parent() instanceof TypeParameter prm) {
            return forceResolve(bound.subtype().assignabilityTo(prm.bound(), this.system));
        } else if (bound.subtype() instanceof TypeClass cls) {
            return forceResolve(cls, bound.parent());
        } else {
            return forceResolve(bound.subtype().assignabilityTo(bound.parent(), this.system));
        }
    }

    private TypeConstraint forceResolve(TypeClass self, TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            Optional<TypePrimitive> unbox = TypePrimitive.unbox(self.declaration().namespace());
            if (unbox.isPresent()) {
                return unbox.get().assignabilityTo(prim, this.system);
            }
        } else if (other instanceof TypeClass otherClass) {
            if (!self.declaration().equals(otherClass.declaration())) {
                Optional<TypeClass> parent = self.parent(otherClass.declaration());
                if (parent.isPresent()) {
                    self = parent.get();
                } else {
                    return TypeConstraint.FALSE;
                }
            }

            if (self.arguments().isEmpty() || otherClass.arguments().isEmpty()) {
                return TypeConstraint.TRUE; //Unchecked conversion
            } else if (self.arguments().size() != otherClass.arguments().size()) {
                return TypeConstraint.FALSE;
            } else {
                List<TypeConstraint> constraints = new ArrayList<>();
                for (int i = 0; i < self.arguments().size(); i++) {
                    TypeConcrete ti = self.arguments().get(i);
                    TypeConcrete si = otherClass.arguments().get(i);

                    if (si instanceof TypeOut typeOut) { //? extends X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        constraints.add(ti.assignabilityTo(bound, this.system)
                                .and(ti.assignabilityTo(typeOut.bound(), this.system)));
                    } else if (si instanceof TypeIn typeIn) { //? super X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        constraints.add(ti.assignabilityTo(bound, this.system)
                                .and(typeIn.bound().assignabilityTo(ti, this.system)));
                    } else {
                        constraints.add(new TypeConstraint.Equal(ti, si));
                    }
                }
                return new TypeConstraint.And(constraints);
            }
        }

        return TypeConcrete.defaultTests(self, other, this.system, TypeConstraint.FALSE);
    }

    private TypeConstraint forceResolve(TypeConstraint.Not not) {
        return forceResolve(not.constraint()).not();
    }


}
