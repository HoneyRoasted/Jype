package honeyroasted.jype.system;

import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JInstantiableType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface JExpressionInformation {

    static SimplyTyped of(JType type) {
        return new SimplyTyped.Simple(type);
    }

    String simpleName();

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
        return Optional.empty();
    }

    interface SimplyTyped extends JExpressionInformation {
        record Simple(JType type) implements SimplyTyped {
            @Override
            public JType type(JTypeSystem system) {
                return this.type;
            }

            @Override
            public String simpleName() {
                return this.type.simpleName();
            }

            @Override
            public String toString() {
                return "simply-typed(" + this.type + ")";
            }
        }

        JType type(JTypeSystem system);

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(this.type(system)));
        }
    }

    interface Constant extends SimplyTyped {
        record Simple(Object value) implements Constant {
            @Override
            public String simpleName() {
                return this.value instanceof String str ? "\"" + escape(str) + "\"" :
                        String.valueOf(this.value);
            }

            @Override
            public String toString() {
                return "const(" + this.value + ")";
            }

            private static char esc = '\\';
            private static Map<Character, String> escapes = Map.of(
                    '0', "\0",
                    'b', "\b",
                    't', "\t",
                    'n', "\n",
                    'f', "\f",
                    'r', "\r",
                    '"', "\"",
                    '\'', "'",
                    '\\', "\\");
            private static String escape(String string) {
                if (string == null) {
                    return "";
                }

                if(escapes.entrySet().stream().anyMatch(c -> c.getKey() == esc && c.getValue().equals(String.valueOf(esc)))) {
                    string = string.replace(String.valueOf(esc), esc + "" + esc);
                }

                for (Map.Entry<Character, String> entry : escapes.entrySet()) {
                    if (entry.getKey() != esc || !entry.getValue().equals(String.valueOf(esc))) {
                        String re = esc + "" + entry.getKey();
                        string = string.replace(entry.getValue(), re);
                    }
                }

                return string;
            }

        }

        Object value();

        @Override
        default JType type(JTypeSystem system) {
            if (value() == null) {
                return system.constants().nullType();
            } else {
                JType type = system.tryResolve(value().getClass());
                if (type instanceof JClassType ct) {
                    JType unboxed = system.constants().primitiveByBox().get(ct.namespace());
                    if (unboxed != null) {
                        return unboxed;
                    }
                }
                return type;
            }
        }
    }

    interface Multi extends JExpressionInformation {
        record Simple(List<? extends JExpressionInformation> children) implements Multi {
            @Override
            public String simpleName() {
                return this.children().stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(" & "));
            }

            @Override
            public String toString() {
                return "multi(" + this.children.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ")";
            }
        }

        List<? extends JExpressionInformation> children();

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(JExpressionInformation::isSimplyTyped);
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().voidType());
            } else if (this.children().size() == 1) {
                return this.children().get(0).getSimpleType(system, Function.identity());
            } else {
                Set<JType> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    JType childType = c.getSimpleType(system, Function.identity()).get();

                    if (childType instanceof JIntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                return Optional.of(mapper.apply(JIntersectionType.of(childrenTypes, system)));
            }
        }
    }


    interface Invocation extends JExpressionInformation {
        JClassReference declaring();

        JMethodReference declaringMethod();

        List<JExpressionInformation> parameters();

        List<JArgumentType> explicitTypeArguments();
    }

    interface Instantiation extends Invocation {
        record Simple(JClassReference declaring, JMethodReference declaringMethod, JClassReference type, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements Instantiation {
            @Override
            public String simpleName() {
                return "new " + this.type.simpleName() +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
            }

            @Override
            public String toString() {
                return "instantiation(" + this.type +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::toString).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ") IN " + (this.declaringMethod == null ? this.declaring : this.declaringMethod) + ")";
            }
        }

        JClassReference type();
    }

    interface MethodInvocation<T> extends Invocation {
        record Simple<T>(JClassReference declaring, JMethodReference declaringMethod, T source, String name, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements MethodInvocation<T> {
            @Override
            public String simpleName() {
                return (this.source instanceof JExpressionInformation expr ? expr.simpleName() : this.source instanceof JType type ? type.simpleName() : this.source) +
                        "." + this.name +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
            }

            @Override
            public String toString() {
                return "invocation(" + this.source + "." + this.name +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::toString).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ") IN " + (this.declaringMethod == null ? this.declaring : this.declaringMethod) + ")";
            }
        }

        T source();

        String name();
    }

    interface GetField<T> extends JExpressionInformation {
        record Simple<T>(JClassReference declaring, JMethodReference declaringMethod, T source, String name) implements GetField<T> {
            @Override
            public String simpleName() {
                return (this.source instanceof JExpressionInformation expr ? expr.simpleName() : this.source instanceof JType type ? type.simpleName() : this.source) +
                        "." + this.name;
            }

            @Override
            public String toString() {
                return "getfield(" + this.source + "." + this.name + ") IN " +
                        (this.declaringMethod == null ? this.declaring : this.declaringMethod);
            }
        }

        JClassReference declaring();

        JMethodReference declaringMethod();

        T source();

        String name();
    }

    interface Lambda extends JExpressionInformation {
        JExpressionInformation body();

        List<JType> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends JExpressionInformation {
        JInstantiableType type();

        List<JArgumentType> explicitTypeArguments();
    }

    interface InvocationReference extends JExpressionInformation {
        JExpressionInformation source();

        String methodName();

        List<JArgumentType> explicitTypeArguments();
    }

}
