package honeyroasted.jypestub.model.test;

import tools.jackson.core.type.TypeReference;
import honeyroasted.almonds.SimpleName;
import honeyroasted.collect.multi.Triple;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jypestub.model.JStubSerialization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JTestExpression {
    Map<String, Class<? extends JTestExpression>> SUBTYPE_KEYS = Map.ofEntries(
            Map.entry("simple", SimplyTyped.class),
            Map.entry("constant", Constant.class),
            Map.entry("invocation", Invocation.class),
            Map.entry("instantiation", Instantiation.class),
            Map.entry("anonymous", Anonymous.class),
            Map.entry("get_field", GetField.class),
            Map.entry("lambda", Lambda.class),
            Map.entry("lambda_param", LambdaParam.class),
            Map.entry("instantiation_ref", InstantiationReference.class),
            Map.entry("invocation_ref", InvocationReference.class),
            Map.entry("cast", Cast.class),
            Map.entry("instance_of", InstanceOf.class),
            Map.entry("conditional", Conditional.class),
            Map.entry("unary", Unary.class),
            Map.entry("binary", Binary.class),
            Map.entry("array_access", ArrayAccess.class),
            Map.entry("assignment", Assignment.class),
            Map.entry("increment", IncrementDecrement.class),
            Map.entry("switch", Switch.class),
            Map.entry("class_literal", ClassLiteral.class),
            Map.entry("this", This.class),
            Map.entry("local", Local.class)
    );

    JExpressionInformation resolve(JTypeSystem system);

    record Wrapper(JTestExpression inner) implements JStubSerialization.Wrapper<JTestExpression> {

        public JExpressionInformation resolve(JTypeSystem system) {
            return this.inner() == null ? new JExpressionInformation.Constant.Simple(null) : this.inner().resolve(system);
        }

        public static JStubSerialization.WrapperSerializer<JTestExpression, Wrapper> SERIALIZER = new JStubSerialization.WrapperSerializer<>(Wrapper.class, SUBTYPE_KEYS);
        public static JStubSerialization.WrapperDeserializer<JTestExpression, Wrapper> DESERIALIZER = new JStubSerialization.WrapperDeserializer<>(Wrapper.class, SUBTYPE_KEYS, Wrapper::new);

    }

    record SimplyTyped(String def,  String declaring, String declaringMethod) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            return new JExpressionInformation.SimplyTyped.Simple(JStubSerialization.readType(system, def, containing));
        }
    }

    record Constant(Object value) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.Constant.Simple(this.value);
        }

        public static JStubSerialization.UnwrappingSerializer<Constant, Object> SERIALIZER = new JStubSerialization.UnwrappingSerializer<>(Constant.class, Constant::value);
        public static JStubSerialization.WrappingDeserializer<Constant, Object> DESERIALIZER = new JStubSerialization.WrappingDeserializer<>(Constant.class, new TypeReference<Object>() {}, Constant::new);
    }

    record Instantiation(String declaring, String declaringMethod, String type, List<Wrapper> parameters, List<String> typeArguments) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod);
            JClassReference type = JStubSerialization.<JClassType>readType(system, this.type, declaring.left()).classReference();

            return new JExpressionInformation.Instantiation.Simple(declaring.middle(), declaring.right(), type,
                    this.parameters().stream().map(expr -> expr.resolve(system)).toList(),
                    this.typeArguments().stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, declaring.left())).toList());
        }
    }

    record Anonymous(String declaring, String declaringMethod, String type, List<Wrapper> parameters, List<String> typeArguments) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod);
            JClassReference type = JStubSerialization.<JClassType>readType(system, this.type, declaring.left()).classReference();

            return new JExpressionInformation.Instantiation.Anonymous(declaring.middle(), declaring.right(), type,
                    list(this.parameters).stream().map(expr -> expr.resolve(system)).toList(),
                    list(this.typeArguments).stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, declaring.left())).toList(), List.of(), null);
        }
    }

    record Invocation(String declaring, String declaringMethod, String staticSource, Wrapper source, String name, List<Wrapper> parameters, List<String> typeArguments) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod);
            SimpleName source = JStubSerialization.source(system, declaring.left(), this.staticSource, this.source);

            return new JExpressionInformation.MethodInvocation.Simple<>(declaring.middle(), declaring.right(), source, name,
                    this.parameters().stream().map(expr -> expr.resolve(system)).toList(),
                    this.typeArguments().stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, declaring.left())).toList()
            );
        }
    }

    record GetField(String declaring, String declaringMethod, String staticSource, Wrapper source, String name) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod);

            return new JExpressionInformation.GetField.Simple<>(declaring.middle(), declaring.right(),
                    JStubSerialization.source(system, declaring.left(), this.staticSource, this.source), name);
        }
    }

    record Lambda(Wrapper body, String declaring, String declaringMethod, List<String> parameterTypes, List<String> parameterNames, int parameterCount, boolean implicitReturn, boolean block, List<Wrapper> returns, boolean voidReturn, boolean canCompleteNormally) implements JTestExpression {
        private static final ThreadLocal<Deque<Map<String, JExpressionInformation.LambdaParameter>>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            List<String> types = list(this.parameterTypes);
            List<String> names = list(this.parameterNames);
            int count = Math.max(this.parameterCount, Math.max(types.size(), names.size()));

            Map<String, JExpressionInformation.LambdaParameter> scope = new HashMap<>();
            List<JExpressionInformation.LambdaParameter> parameters = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                JExpressionInformation.LambdaParameter parameter = JExpressionInformation.LambdaParameter.fresh(i < names.size() ? names.get(i) : "arg" + i);
                parameters.add(parameter);
                scope.put(parameter.name(), parameter);
            }

            SCOPES.get().push(scope);
            try {
                JExpressionInformation.LambdaBody lambdaBody;
                if (this.block) {
                    List<JExpressionInformation> statements = this.body == null || this.body.inner() == null ? List.of() : List.of(this.body.resolve(system));
                    lambdaBody = new JExpressionInformation.LambdaBody.Block(statements, list(this.returns).stream().map(r -> r.resolve(system)).toList(), this.voidReturn, this.canCompleteNormally);
                } else if (this.implicitReturn) {
                    lambdaBody = new JExpressionInformation.LambdaBody.Expression(this.body.resolve(system));
                } else {
                    lambdaBody = JExpressionInformation.LambdaBody.Block.empty();
                }
                return new JExpressionInformation.Lambda.Simple(parameters, types.stream().map(str -> JStubSerialization.<JType>readType(system, str, containing)).toList(), lambdaBody);
            } finally {
                SCOPES.get().pop();
            }
        }
    }

    record LambdaParam(String name) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            for (Map<String, JExpressionInformation.LambdaParameter> scope : Lambda.SCOPES.get()) {
                JExpressionInformation.LambdaParameter parameter = scope.get(this.name);
                if (parameter != null) {
                    return parameter;
                }
            }
            throw new IllegalArgumentException("Unknown lambda parameter: " + this.name);
        }

        public static JStubSerialization.UnwrappingSerializer<LambdaParam, String> SERIALIZER = new JStubSerialization.UnwrappingSerializer<>(LambdaParam.class, LambdaParam::name);
        public static JStubSerialization.WrappingDeserializer<LambdaParam, String> DESERIALIZER = new JStubSerialization.WrappingDeserializer<>(LambdaParam.class, new TypeReference<String>() {}, LambdaParam::new);
    }

    record InstantiationReference(String type, String declaring, String declaringMethod, List<String> typeArguments) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            return new JExpressionInformation.InstantiationReference.Simple(
                    JStubSerialization.readType(system, this.type, containing),
                    this.typeArguments.stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, containing)).toList()
            );
        }
    }

    record InvocationReference(String declaring, String declaringMethod, String staticSource, Wrapper source, String methodName, List<String> typeArguments) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            SimpleName source = JStubSerialization.source(system, containing, this.staticSource, this.source);

            return new JExpressionInformation.InvocationReference.Simple<>(
                    source, this.methodName,
                    this.typeArguments.stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, containing)).toList()
            );
        }
    }

    record Cast(String type, Wrapper operand, String declaring, String declaringMethod) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            return new JExpressionInformation.Cast.Simple(this.operand.resolve(system), JStubSerialization.readType(system, this.type, containing));
        }
    }

    record InstanceOf(String type, Wrapper operand, String declaring, String declaringMethod) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            return new JExpressionInformation.InstanceOf.Simple(this.operand.resolve(system), JStubSerialization.readType(system, this.type, containing));
        }
    }

    record Conditional(Wrapper condition, Wrapper whenTrue, Wrapper whenFalse) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.Conditional.Simple(this.condition.resolve(system), this.whenTrue.resolve(system), this.whenFalse.resolve(system));
        }
    }

    record Unary(String operator, Wrapper operand) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.UnaryOperator.Simple(JExpressionInformation.UnaryOperator.Kind.valueOf(this.operator), this.operand.resolve(system));
        }
    }

    record Binary(String operator, Wrapper left, Wrapper right) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.BinaryOperator.Simple(JExpressionInformation.BinaryOperator.Kind.valueOf(this.operator), this.left.resolve(system), this.right.resolve(system));
        }
    }

    record ArrayAccess(Wrapper array, Wrapper index) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.ArrayAccess(this.array.resolve(system), this.index.resolve(system));
        }
    }

    record Assignment(Wrapper variable, String operator, Wrapper value) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JExpressionInformation.BinaryOperator.Kind kind = this.operator == null || this.operator.isEmpty() ? null : JExpressionInformation.BinaryOperator.Kind.valueOf(this.operator);
            return new JExpressionInformation.Assignment(this.variable.resolve(system), kind, this.value.resolve(system));
        }
    }

    record IncrementDecrement(Wrapper variable, boolean increment, boolean prefix) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.IncrementDecrement(this.variable.resolve(system), this.increment, this.prefix);
        }
    }

    record Switch(Wrapper selector, List<Wrapper> results, boolean patterns) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.Switch(this.selector.resolve(system), list(this.results).stream().map(r -> r.resolve(system)).toList(), this.patterns);
        }
    }

    record ClassLiteral(String type, String declaring, String declaringMethod) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            return new JExpressionInformation.ClassLiteral(JStubSerialization.readType(system, this.type, containing));
        }
    }

    record This(String declaring, String declaringMethod, String qualifier) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod);
            JClassReference qualifier = this.qualifier == null ? null : JStubSerialization.<JClassType>readType(system, this.qualifier, declaring.left()).classReference();
            return new JExpressionInformation.This(declaring.middle(), declaring.right(), qualifier);
        }
    }

    record Local(String name, String type, boolean isFinal, String declaring, String declaringMethod) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            return new JExpressionInformation.LocalVariable(this.name, JStubSerialization.readType(system, this.type, containing), this.isFinal);
        }
    }

    private static <T> List<T> list(List<T> list) {
        return list == null ? List.of() : list;
    }
}
