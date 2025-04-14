package honeyroasted.jype.system;

import honeyroasted.almonds.SimpleName;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JInstantiableType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface JExpressionInformation extends SimpleName {

    static SimplyTyped of(JType type) {
        return new SimplyTyped.Simple(type);
    }

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

    interface MethodInvocation<T extends SimpleName> extends Invocation {
        record Simple<T extends SimpleName>(JClassReference declaring, JMethodReference declaringMethod, T source, String name, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements MethodInvocation<T> {
            @Override
            public String simpleName() {
                return this.source.simpleName() + "." + this.name +
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

    interface GetField<T extends SimpleName> extends JExpressionInformation {
        record Simple<T extends SimpleName>(JClassReference declaring, JMethodReference declaringMethod, T source, String name) implements GetField<T> {
            @Override
            public String simpleName() {
                return this.source.simpleName() + "." + this.name;
            }

            @Override
            public String toString() {
                return "getfield(" + this.source + "." + this.name + ") IN " +
                        (this.declaringMethod == null ? this.declaring : this.declaringMethod);
            }
        }


        @Override
        default boolean isSimplyTyped() {
            return this.source() instanceof JClassReference || (this.source() instanceof JExpressionInformation expr && expr.isSimplyTyped());
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            if (this.source() instanceof JClassReference jcr) {
                return jcr.declaredFields().stream().filter(jfr -> jfr.hasModifier(AccessFlag.STATIC) && jfr.location().name().equals(this.name()))
                        .findFirst().map(mapper);
            } else if (this.source() instanceof JExpressionInformation expr && expr.isSimplyTyped()) {
                //TODO process expression
            }

            return Optional.empty();
        }

        JClassReference declaring();

        JMethodReference declaringMethod();

        T source();

        String name();
    }

    interface Lambda extends JExpressionInformation {
        record Simple(JExpressionInformation body, List<JArgumentType> explicitParameterTypes, int parameterCount, boolean implicitReturn) implements Lambda {
            @Override
            public String simpleName() {
                return "(" + (this.explicitParameterTypes != null && this.explicitParameterTypes.size() == parameterCount ?
                        this.explicitParameterTypes.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) :
                        IntStream.range(0, parameterCount).mapToObj(i -> "arg" + i).collect(Collectors.joining(", "))) +
                        ") -> " + (this.implicitReturn ? "" : "{ ") + this.body.simpleName() + (this.implicitReturn ? "" : " }");
            }

            @Override
            public String toString() {
                return "lambda([" + (this.explicitParameterTypes != null && this.explicitParameterTypes.size() == parameterCount ?
                        this.explicitParameterTypes.stream().map(Object::toString).collect(Collectors.joining(", ")) :
                        this.parameterCount) + "] -> "
                        + (this.implicitReturn ? "" : "{ ") + this.body + (this.implicitReturn ? "" : " }");
            }
        }

        JExpressionInformation body();

        List<JArgumentType> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends JExpressionInformation {
        record Simple(JInstantiableType type, List<JArgumentType> explicitTypeArguments) implements InstantiationReference {
            @Override
            public String simpleName() {
                return type.simpleName() + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "::new";
            }

            @Override
            public String toString() {
                return "instantiation_ref(" + this.type + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ">") +
                        ")";
            }
        }

        JInstantiableType type();

        List<JArgumentType> explicitTypeArguments();
    }

    interface InvocationReference<T extends SimpleName> extends JExpressionInformation {
        record Simple<T extends SimpleName>(T source, String methodName, List<JArgumentType> explicitTypeArguments) implements InvocationReference<T> {
            @Override
            public String simpleName() {
                return source.simpleName() + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "::" + methodName;
            }

            @Override
            public String toString() {
                return "instantiation_ref(" + this.source + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ">") +
                        ":: " + methodName + ")";
            }
        }

        T source();

        String methodName();

        List<JArgumentType> explicitTypeArguments();
    }

}
