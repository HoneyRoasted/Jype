package honeyroasted.jypestub.model.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jypestub.model.JStubSerialization;

import java.io.IOException;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record JStubClass(@JsonIgnore String location, String def, String name, Set<AccessFlag> flags, Map<String, JStubMember> fields, Map<String, JStubMember> methods) {

    public static JStubClass of(JClassType jct) {
        String def = jct.declarationSignature().toString();
        Map<String, JStubMember> fields = new LinkedHashMap<>();
        jct.declaredFields().forEach(jfr -> fields.put(jfr.location().name(), JStubMember.of(jfr)));

        Map<String, JStubMember> methods = new LinkedHashMap<>();
        jct.declaredMethods().forEach(jmr -> methods.put(jmr.location().name(), JStubMember.of(jmr)));

        return new JStubClass(jct.namespace().location().toInternalName(), def, jct.namespace().location().getPackage().toInternalName() + "/" + jct.namespace().name().simpleName(),
                AccessFlag.maskToAccessFlags(jct.modifiers(), jct.outerClass() != null || jct.outerMethod() != null ? AccessFlag.Location.INNER_CLASS : AccessFlag.Location.CLASS),
                fields, methods);
    }

    private static final Set<AccessFlag> defaultFlags = Set.of(AccessFlag.PUBLIC);

    public static class Deserializer extends StdDeserializer<JStubClass> {
        public Deserializer() {
            super(JStubClass.class);
        }

        @Override
        public JStubClass deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String name = ctxt.getParser().currentName();

            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            if (node.isValueNode()) {
                return new JStubClass(name, node.asText(), name, defaultFlags, Collections.emptyMap(), Collections.emptyMap());
            } else if (node.isObject()) {
                return new JStubClass(name,
                        JStubSerialization.getOrDefault(node, "def", name),
                        JStubSerialization.getOrDefault(node, "name", name),
                        JStubSerialization.getOrDefault(oc, node, new TypeReference<Set<AccessFlag>>() {}, "flags", defaultFlags),
                        JStubSerialization.getOrDefault(oc, node, new TypeReference<Map<String, JStubMember>>() {}, "fields", Collections.emptyMap()),
                        JStubSerialization.getOrDefault(oc, node, new TypeReference<Map<String, JStubMember>>() {}, "methods", Collections.emptyMap()));
            }
            return null;
        }
    }

}
