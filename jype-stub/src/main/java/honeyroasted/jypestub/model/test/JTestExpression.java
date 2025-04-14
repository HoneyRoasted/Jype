package honeyroasted.jypestub.model.test;

import com.fasterxml.jackson.core.type.TypeReference;
import honeyroasted.almonds.SimpleName;
import honeyroasted.collect.multi.Triple;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jypestub.model.JStubSerialization;

import java.util.List;
import java.util.Map;

public interface JTestExpression {
    Map<String, Class<? extends JTestExpression>> SUBTYPE_KEYS = Map.of(
            "simple", SimplyTyped.class,
            "constant", Constant.class,
            "multi", Multi.class,
            "invocation", Invocation.class,
            "instantiation", Instantiation.class,
            "get_field", GetField.class,
            "lambda", Lambda.class,
            "instantiation_ref", InstantiationReference.class,
            "invocation_ref", InvocationReference.class
    );

    JExpressionInformation resolve(JTypeSystem system);

    record Wrapper(JTestExpression inner) implements JStubSerialization.Wrapper<JTestExpression> {

        public JExpressionInformation resolve(JTypeSystem system) {
            return this.inner().resolve(system);
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

    record Multi(List<Wrapper> children) implements JTestExpression {
        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            return new JExpressionInformation.Multi.Simple(this.children().stream().map(wrapper -> wrapper.inner().resolve(system)).toList());
        }

        public static JStubSerialization.UnwrappingSerializer<Multi, List<Wrapper>> SERIALIZER = new JStubSerialization.UnwrappingSerializer<>(Multi.class, Multi::children);
        public static JStubSerialization.WrappingDeserializer<Multi, List<Wrapper>> DESERIALIZER = new JStubSerialization.WrappingDeserializer<>(Multi.class, new TypeReference<List<Wrapper>>() {}, Multi::new);

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

    record Lambda(Wrapper body, String declaring, String declaringMethod, List<String> parameterTypes, int parameterCount, boolean implicitReturn) implements JTestExpression {

        public Lambda(Wrapper body, String declaring, String declaringMethod, List<String> parameterTypes, int parameterCount, boolean implicitReturn) {
            this.body = body;
            this.declaring = declaring;
            this.declaringMethod = declaringMethod;
            this.parameterTypes = parameterTypes;
            this.parameterCount = (!parameterTypes.isEmpty() && parameterCount == 0 ? parameterTypes.size() : parameterCount);
            this.implicitReturn = implicitReturn;
        }

        @Override
        public JExpressionInformation resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            return new JExpressionInformation.Lambda.Simple(body.resolve(system),
                    this.parameterTypes.stream().map(str -> JStubSerialization.<JArgumentType>readType(system, str, containing)).toList(),
                    this.parameterCount, this.implicitReturn);
        }
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

}
