package honeyroasted.jypestub.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.SimpleName;
import honeyroasted.collect.multi.Triple;
import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JSignatureParser;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jypestub.model.test.JTestCondition;
import honeyroasted.jypestub.model.test.JTestConstraint;
import honeyroasted.jypestub.model.test.JTestExpression;
import honeyroasted.jypestub.model.types.JStubClass;
import honeyroasted.jypestub.model.types.JStubMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public interface JStubSerialization {

    static ObjectMapper buildMapper() {
        SimpleModule module = new SimpleModule("JStubModule", Version.unknownVersion());

        module.addDeserializer(JTestExpression.Wrapper.class, JTestExpression.Wrapper.DESERIALIZER)
                .addSerializer(JTestExpression.Wrapper.SERIALIZER);

        module.addDeserializer(JTestConstraint.Wrapper.class, JTestConstraint.Wrapper.DESERIALIZER)
                .addSerializer(JTestConstraint.Wrapper.SERIALIZER);

        module.addDeserializer(JTestCondition.Wrapper.class, JTestCondition.Wrapper.DESERIALIZER)
                .addSerializer(JTestCondition.Wrapper.SERIALIZER);

        module.addDeserializer(JStubMember.class, new JStubMember.Deserializer())
                .addDeserializer(JStubClass.class, new JStubClass.Deserializer());

        module.addDeserializer(JTestConstraint.Equal.class, JTestConstraint.Equal.DESERIALIZER)
                .addDeserializer(JTestConstraint.Subtype.class, JTestConstraint.Subtype.DESERIALIZER)
                .addDeserializer(JTestConstraint.Contains.class, JTestConstraint.Contains.DESERIALIZER)
                .addDeserializer(JTestConstraint.Capture.class, JTestConstraint.Capture.DESERIALIZER);

        module.addDeserializer(JTestExpression.Constant.class, JTestExpression.Constant.DESERIALIZER)
                .addSerializer(JTestExpression.Constant.SERIALIZER);

        module.addDeserializer(JTestExpression.Multi.class, JTestExpression.Multi.DESERIALIZER)
                .addSerializer(JTestExpression.Multi.SERIALIZER);

        module.addDeserializer(JTestCondition.Status.class, JTestCondition.Status.DESERIALIZER)
                .addSerializer(JTestCondition.Status.SERIALIZER);

        return YAMLMapper.builder()
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultNullHandling(existing -> JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
                .addModule(module)
                .build();
    }

    static String getOrDefault(JsonNode node, String key, String fallback) {
        JsonNode sub = node.get(key);
        if (sub != null) {
            return sub.asString(fallback);
        }
        return fallback;
    }

    static <T> T getOrDefault(DeserializationContext ctxt, JsonNode node, TypeReference<T> reference, String key, T fallback) {
        JsonNode sub = node.get(key);
        if (sub != null) {
            return ctxt.readTreeAsValue(sub, ctxt.getTypeFactory().constructType(reference));
        }
        return fallback;
    }

    static Set<ConstraintBranch> branches(ConstraintTree tree, JTestCondition.Branch branch) {
        if (branch == JTestCondition.Branch.ALL) {
            return tree.branches();
        } else if (branch == JTestCondition.Branch.INVALID) {
            return tree.invalidBranches();
        } else {
            return tree.validBranches();
        }
    }

    static SimpleName source(JTypeSystem system, JGenericDeclaration containing, String staticSource, JTestExpression.Wrapper source) {
        if (staticSource != null && !staticSource.isEmpty()) {
            return JStubSerialization.<JClassType>readType(system, staticSource, containing);
        } else {
            return source.resolve(system);
        }
    }

    static Triple<JGenericDeclaration, JClassReference, JMethodReference> declaring(JTypeSystem system, String declaringStr, String declaringMethodStr) {
        JClassType declaring = JStubSerialization.readType(system, declaringStr, system.constants().object());
        JMethodType declaringMethod = JStubSerialization.readType(system, declaringMethodStr, system.constants().object());

        if (declaring == null) {
            if (declaringMethod != null) {
                declaring = declaringMethod.outerClass();
            } else {
                declaring = system.constants().object();
            }
        }

        JClassReference declaringRef = declaring.classReference();
        JMethodReference declaringMethodRef = declaringMethod == null ? null : declaringMethod.methodReference();
        JGenericDeclaration containing = declaringMethod != null ? declaringMethod : declaring;

        return Triple.of(containing, declaringRef, declaringMethodRef);
    }

    static <T extends JType> T readType(JTypeSystem system, String ref, JGenericDeclaration containing) {
        if (ref == null || ref.isEmpty()) {
            return null;
        } else if (ref.contains(":#")) {
            String[] parts = ref.split(":#");
            JClassType jct = system.tryResolve(JClassLocation.of(parts[0]));
            return (T) jct.declaredMethods().stream()
                    .filter(jfr -> jfr.location().name().equals(parts[1]))
                    .findFirst().orElseThrow();
        } else if (ref.contains("#.")) {
            String[] parts = ref.split("\\.#");
            JClassType jct = system.tryResolve(JClassLocation.of(parts[0]));
            return (T) jct.declaredFields().stream()
                    .filter(jfr -> jfr.location().name().equals(parts[1]))
                    .findFirst().orElseThrow();
        } else {
            return system.tryResolve(new JSignature.Declared(
                    new JSignatureParser(ref).parseInformalType(),
                    containing
            ));
        }
    }

    class UnwrappingSerializer<T, S> extends StdSerializer<T> {
        private Function<T, S> getter;

        public UnwrappingSerializer(Class<T> t, Function<T, S> getter) {
            super(t);
            this.getter = getter;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
            ctxt.writeValue(gen, this.getter.apply(value));
        }
    }

    class WrappingDeserializer<T, S> extends StdDeserializer<T> {
        private Function<S, T> constructor;
        private TypeReference<S> value;

        public WrappingDeserializer(Class<T> t, TypeReference<S> s, Function<S, T> constructor) {
            super(t);
            this.constructor = constructor;
            this.value = s;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) {
            return constructor.apply(ctxt.readValue(p, this.value));
        }
    }

    class TypeListDeserializer<T> extends StdDeserializer<T> {
        private Function<List<String>, T> constructor;
        private Function<Triple<String, String, List<String>>, T> wideConstructor;

        public TypeListDeserializer(Class<T> v, Function<List<String>, T> constructor,
                                    Function<Triple<String, String, List<String>>, T> wideConstructor) {
            super(v);
            this.constructor = constructor;
            this.wideConstructor = wideConstructor;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);

            if (node.isArray()) {
                List<String> types = new ArrayList<>();
                for (JsonNode element : node.values()) {
                    types.add(element.asString());
                }

                return this.constructor.apply(types);
            } else {
                return this.wideConstructor.apply(Triple.of(
                        getOrDefault(node, "declaring", null),
                        getOrDefault(node, "declaringMethod", null),
                        getOrDefault(ctxt, node, new TypeReference<List<String>>() {}, "children", Collections.emptyList())
                ));
            }
        }
    }

    interface Wrapper<T> {

        T inner();

    }

    class WrapperSerializer<T, W extends Wrapper<T>> extends StdSerializer<W> {
        private Map<String, Class<? extends T>> subtypeKeys;

        public WrapperSerializer(Class<W> t, Map<String, Class<? extends T>> subtypeKeys) {
            super(t);
            this.subtypeKeys = subtypeKeys;
        }

        @Override
        public void serialize(W value, JsonGenerator gen, SerializationContext ctxt) {
            Class<?> inner = value.inner() == null ? null : value.inner().getClass();
            String key = subtypeKeys.entrySet().stream()
                    .filter(en -> en.getValue().equals(inner))
                    .findFirst().map(Map.Entry::getKey)
                    .orElse(null);
            if (key == null) {
                ctxt.reportMappingProblem("Failed to map value of type " + inner);
            }

            gen.writeStartObject();
            gen.writePOJOProperty(key, value.inner());

            gen.writeEndObject();
        }
    }

    class WrapperDeserializer<T, W extends Wrapper<T>> extends StdDeserializer<W> {
        private Map<String, Class<? extends T>> subtypeKeys;
        private Function<T, W> constructor;

        public WrapperDeserializer(Class<W> t, Map<String, Class<? extends T>> subtypeKeys, Function<T, W> constructor) {
            super(t);
            this.subtypeKeys = subtypeKeys;
            this.constructor = constructor;
        }

        @Override
        public W deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);
            if (node.isObject()) {
                for (Map.Entry<String, JsonNode> entry : node.properties()) {
                    if (subtypeKeys.containsKey(entry.getKey())) {
                        return this.constructor.apply(ctxt.readTreeAsValue(entry.getValue(), subtypeKeys.get(entry.getKey())));
                    }
                }
                return ctxt.reportInputMismatch(this, "No valid key included in object");
            }

            return ctxt.reportInputMismatch(this, "Node is not an object");
        }
    }
}
