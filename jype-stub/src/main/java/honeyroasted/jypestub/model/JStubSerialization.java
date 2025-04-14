package honeyroasted.jypestub.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;

public interface JStubSerialization {

    static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                .enable(MINIMIZE_QUOTES)
                .disable(WRITE_DOC_START_MARKER));

        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);

        mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));

        SimpleModule module = new SimpleModule("JStubModule", new Version(0, 0, 0, null));

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
                .addSerializer(JTestExpression.Multi.SERIALIZER);;

        module.addDeserializer(JTestCondition.Status.class, JTestCondition.Status.DESERIALIZER)
                .addSerializer(JTestCondition.Status.SERIALIZER);;

        mapper.registerModule(module);

        return mapper;
    }

    static String getOrDefault(JsonNode node, String key, String fallback) {
        JsonNode sub = node.get(key);
        if (sub != null) {
            return sub.asText(fallback);
        }
        return fallback;
    }

    static <T> T getOrDefault(ObjectCodec codec, JsonNode node, TypeReference<T> reference, String key, T fallback) throws IOException {
        JsonNode sub = node.get(key);
        if (sub != null) {
            return codec.treeAsTokens(sub).readValueAs(reference);
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
        public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.getCodec().writeValue(gen, this.getter.apply(value));
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
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = p.getCodec();
            return constructor.apply(oc.readValue(p, this.value));
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
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);

            if (node.isArray()) {
                List<String> types = new ArrayList<>();
                Iterator<JsonNode> nodes = node.elements();
                while (nodes.hasNext()) {
                    types.add(nodes.next().asText());
                }

                return this.constructor.apply(types);
            } else {
                return this.wideConstructor.apply(Triple.of(
                        getOrDefault(node, "declaring", null),
                        getOrDefault(node, "declaringMethod", null),
                        getOrDefault(oc, node, new TypeReference<List<String>>() {}, "children", Collections.emptyList())
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
        public void serialize(W value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            Class<?> inner = value.inner() == null ? null : value.inner().getClass();
            gen.writeObjectField(subtypeKeys.entrySet().stream()
                            .filter(en -> en.getValue().equals(inner))
                            .findFirst().map(Map.Entry::getKey)
                            .orElseThrow(() -> new JsonGenerationException("Failed to map value of type " + inner, gen)),
                    value.inner());

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
        public W deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                if (fields.hasNext()) {
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        if (subtypeKeys.containsKey(entry.getKey())) {
                            return this.constructor.apply(oc.treeToValue(entry.getValue(), subtypeKeys.get(entry.getKey())));
                        }
                    }
                }
                throw new JsonParseException("No valid key included in object");
            }

            throw new JsonParseException(p, "Node is not an object");
        }
    }
}
