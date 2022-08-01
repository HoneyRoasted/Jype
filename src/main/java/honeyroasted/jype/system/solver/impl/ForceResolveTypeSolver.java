package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeVerification;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ForceResolveTypeSolver extends AbstractTypeSolver {

    
    public ForceResolveTypeSolver(TypeSystem system) {
        super(system);
    }

    @Override
    public TypeSolution solve() {
        if (this.constraints().isEmpty()) {
            return new TypeSolution(new TypeContext(), root(), TypeVerification.success(TypeVerification.Kind.NONE, root()));
        } else {
            TypeVerification verification = forceResolve(root());
            return new TypeSolution(verification.success() ? new TypeContext() : null, root(), verification);
        }
    }

    private TypeVerification forceResolve(TypeConstraint constraint) {
        if (constraint instanceof TypeConstraint.And and) {
            return forceResolve(and);
        } else if (constraint instanceof TypeConstraint.Or or) {
            return forceResolve(or);
        } else if (constraint instanceof TypeConstraint.Equal equal) {
            return forceResolve(equal);
        } else if (constraint instanceof TypeConstraint.Bound bound) {
            return forceResolve(bound);
        } else {
            return null;
        }
    }

    private TypeVerification forceResolve(TypeConstraint.And and) {
        return TypeVerification.builder()
                .kind(TypeVerification.Kind.AND)
                .constraint(and)
                .children(and.constraints().stream().map(this::forceResolve).toList())
                .and()
                .build();
    }

    private TypeVerification forceResolve(TypeConstraint.Or or) {
        return TypeVerification.builder()
                .kind(TypeVerification.Kind.OR)
                .constraint(or)
                .children(or.constraints().stream().map(this::forceResolve).toList())
                .or()
                .build();
    }

    private TypeVerification forceResolve(TypeConstraint.Equal equal) {
        return TypeVerification.builder()
                .kind(TypeVerification.Kind.EQUAL)
                .constraint(equal)
                .success(equal.left().flatten().equals(equal.right().flatten()))
                .build();
    }

    private TypeVerification forceResolve(TypeConstraint.Bound bound) {
        if (bound.subtype().flatten().equals(bound.parent().flatten())) {
            return TypeVerification.success(TypeVerification.Kind.SUBTYPE, bound);
        } else if (bound.subtype() instanceof TypeClass cls) {
            return forceResolve(bound, cls, bound.parent());
        }

        TypeVerification.Builder builder = TypeVerification.builder()
                .kind(TypeVerification.Kind.SUBTYPE)
                .constraint(bound);

        if (bound.subtype() instanceof TypeParameter a && bound.parent() instanceof TypeParameter b) {
            return builder.children(forceResolve(a.bound().assignabilityTo(b.bound(), this.system)))
                    .and().build();
        } else if (bound.subtype() instanceof TypeParameter prm) {
            return builder.children(forceResolve(prm.bound().assignabilityTo(bound.parent(), this.system)))
                    .and().build();
        } else if (bound.parent() instanceof TypeParameter prm) {
            return builder.children(forceResolve(bound.subtype().assignabilityTo(prm.bound(), this.system)))
                    .and().build();
        } else {
            return builder.children(forceResolve(bound.subtype().assignabilityTo(bound.parent(), this.system)))
                    .and().build();
        }
    }

    private TypeVerification forceResolve(TypeConstraint constraint, TypeClass self, TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            Optional<TypePrimitive> unbox = TypePrimitive.unbox(self.declaration().namespace());
            if (unbox.isPresent()) {
                return forceResolve(unbox.get().assignabilityTo(prim, this.system));
            }
        } else if (other instanceof TypeClass otherClass) {
            if (!self.declaration().equals(otherClass.declaration())) {
                Optional<TypeClass> parent = self.parent(otherClass.declaration());
                if (parent.isPresent()) {
                    self = parent.get();
                } else {
                    return TypeVerification.builder()
                            .kind(TypeVerification.Kind.SUBTYPE)
                            .children(TypeVerification.failure(TypeVerification.Kind.SUBTYPE,
                                    new TypeConstraint.Bound(self.declaration().withArguments(), otherClass.declaration().withArguments())))
                            .failure()
                            .constraint(constraint)
                            .build();
                }
            }

            if (self.arguments().isEmpty() || otherClass.arguments().isEmpty()) {
                return TypeVerification.success(TypeVerification.Kind.SUBTYPE, constraint);
            } else if (self.arguments().size() != otherClass.arguments().size()) {
                return TypeVerification.failure(TypeVerification.Kind.SUBTYPE, constraint);
            } else {
                TypeVerification.Builder builder = TypeVerification.builder()
                        .kind(TypeVerification.Kind.SUBTYPE)
                        .constraint(constraint);

                for (int i = 0; i < self.arguments().size(); i++) {
                    TypeConcrete ti = self.arguments().get(i);
                    TypeConcrete si = otherClass.arguments().get(i);

                    if (si instanceof TypeOut typeOut) { //? extends X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        builder.children(forceResolve(ti.assignabilityTo(bound, this.system)
                                .and(ti.assignabilityTo(typeOut.bound(), this.system))));
                    } else if (si instanceof TypeIn typeIn) { //? super X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        builder.children(forceResolve(ti.assignabilityTo(bound, this.system)
                                .and(typeIn.bound().assignabilityTo(ti, this.system))));
                    } else {
                        builder.children(forceResolve(new TypeConstraint.Equal(ti, si)));
                    }
                }

                return builder.and().build();
            }
        }

        return TypeVerification.builder()
                .kind(TypeVerification.Kind.SUBTYPE)
                .constraint(constraint)
                .children(forceResolve(TypeConcrete.defaultTests(self, other, this.system, TypeConstraint.FALSE)))
                .and()
                .build();
    }

}
