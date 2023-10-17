package honeyroasted.jype.system.solver.solvers.inference.expression;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExpressionInformation {

    String simpleName();

    default boolean isStandalone() {
        return false;
    }

    default Optional<Type> getStandaloneType(TypeSystem system) {
        return Optional.empty();
    }

    interface Standalone extends ExpressionInformation {
        Type type();

        @Override
        default boolean isStandalone() {
            return true;
        }

        @Override
        default Optional<Type> getStandaloneType(TypeSystem system) {
            return Optional.of(this.type());
        }
    }

    interface Constant extends Standalone {

        Object value();

    }

    interface Poly extends ExpressionInformation {
        List<ExpressionInformation> children();

        @Override
        default boolean isStandalone() {
            return this.children().stream().allMatch(e -> e instanceof Standalone || (e instanceof Poly p && p.isStandalone()));
        }

        @Override
        default Optional<Type> getStandaloneType(TypeSystem system) {
            if (!this.isStandalone()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().nullType());
            } else if (this.children().size() == 1) {
                return this.children().get(0) instanceof Standalone st ? Optional.of(st.type()) : this.children().get(0).getStandaloneType(system);
            } else {
                IntersectionType type = new IntersectionTypeImpl(system);
                Set<Type> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    Type childType = null;
                    if (c instanceof Standalone st) {
                        childType = st.type();
                    } else if (c instanceof Poly p) {
                        childType = p.getStandaloneType(system).get();
                    }

                    if (childType instanceof IntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                type.setChildren(childrenTypes);
                type.setUnmodifiable(true);
                return Optional.of(type);
            }
        }
    }

    interface Instantiation extends ExpressionInformation {
        InstantiableType type();

        List<ExpressionInformation> parameters();
    }

    interface Invocation extends ExpressionInformation {
        ExpressionInformation source();

        List<ExpressionInformation> parameters();
    }

    interface Lambda extends ExpressionInformation {
        ExpressionInformation body();

        List<Type> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InvocationReference extends ExpressionInformation {
        ExpressionInformation source();
        String methodName();
        List<Type> explicitParameterTypes();
    }

}
