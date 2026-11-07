package honeyroasted.jypestub.model.types;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jypestub.model.JStubSerialization;

import java.lang.reflect.AccessFlag;
import java.util.Set;

public record JStubMember(String def, Set<AccessFlag> flags) {

    public static JStubMember of(JMethodReference jmr) {
        return new JStubMember(jmr.declarationSignature().toString(), AccessFlag.maskToAccessFlags(jmr.modifiers(), AccessFlag.Location.METHOD));
    }

    public static JStubMember of(JFieldReference jfr) {
        return new JStubMember(jfr.signature().toString(), AccessFlag.maskToAccessFlags(jfr.modifiers(), AccessFlag.Location.FIELD));
    }

    private static final Set<AccessFlag> defaultFlags = Set.of(AccessFlag.PUBLIC);

    public static class Deserializer extends StdDeserializer<JStubMember> {
        public Deserializer() {
            super(JStubMember.class);
        }

        @Override
        public JStubMember deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);
            if (node.isValueNode()) {
                return new JStubMember(node.asString(), defaultFlags);
            } else if (node.isObject()) {
                return new JStubMember(node.get("def").asString(),
                        JStubSerialization.getOrDefault(ctxt, node, new TypeReference<Set<AccessFlag>>() {}, "flags", defaultFlags));
            }
            return null;
        }
    }
}
