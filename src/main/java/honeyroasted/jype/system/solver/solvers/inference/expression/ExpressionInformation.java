package honeyroasted.jype.system.solver.solvers.inference.expression;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface ExpressionInformation {

    String simpleName();

    interface Standalone extends ExpressionInformation {
        Type type();
    }

    interface Constant extends Standalone {

        Object value();

    }

    interface Poly extends ExpressionInformation {
        List<ExpressionInformation> children();

        default boolean isStandalone() {
            return this.children().stream().allMatch(e -> e instanceof Standalone || (e instanceof Poly p && p.isStandalone()));
        }

        default Type getStandaloneType(TypeSystem system) {
            if (this.children().isEmpty()) {
                return system.constants().nullType();
            } else if (this.children().size() == 1) {
                return this.children().get(0) instanceof Standalone st ? st.type() : ((Poly) this.children().get(0)).getStandaloneType(system);
            } else {
                IntersectionType type = new IntersectionTypeImpl(system);
                Set<Type> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    Type childType = null;
                    if (c instanceof Standalone st) {
                        childType = st.type();
                    } else if (c instanceof Poly p) {
                        childType = p.getStandaloneType(system);
                    }

                    if (childType instanceof IntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                type.setChildren(childrenTypes);
                type.setUnmodifiable(true);
                return type;
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
