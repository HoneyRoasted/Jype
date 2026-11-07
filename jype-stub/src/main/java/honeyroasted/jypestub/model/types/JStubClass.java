package honeyroasted.jypestub.model.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jypestub.model.JStubSerialization;

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
        public JStubClass deserialize(JsonParser p, DeserializationContext ctxt) {
            String name = ctxt.getParser().currentName();

            JsonNode node = ctxt.readTree(p);
            if (node.isValueNode()) {
                return new JStubClass(name, node.asString(), name, defaultFlags, Collections.emptyMap(), Collections.emptyMap());
            } else if (node.isObject()) {
                return new JStubClass(name,
                        JStubSerialization.getOrDefault(node, "def", name),
                        JStubSerialization.getOrDefault(node, "name", name),
                        JStubSerialization.getOrDefault(ctxt, node, new TypeReference<Set<AccessFlag>>() {}, "flags", defaultFlags),
                        JStubSerialization.getOrDefault(ctxt, node, new TypeReference<Map<String, JStubMember>>() {}, "fields", Collections.emptyMap()),
                        JStubSerialization.getOrDefault(ctxt, node, new TypeReference<Map<String, JStubMember>>() {}, "methods", Collections.emptyMap()));
            }
            return null;
        }
    }

}
