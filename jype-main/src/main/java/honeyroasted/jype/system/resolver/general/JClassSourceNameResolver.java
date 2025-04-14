package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.metadata.JClassSourceName;
import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JClassSourceNameResolver implements JTypeResolver<JClassSourceName, JType> {
    @Override
    public JResolutionResult<JClassSourceName, JType> resolve(JTypeSystem system, JClassSourceName value) {
        String name = value.name();
        int depth = 0;

        while (name.endsWith("[]")) {
            depth += 1;
            name = name.substring(0, name.length() - 2);
        }

        List<JResolutionResult<?, ?>> children = new ArrayList<>();

        String[] parts = value.name().split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String nameGuess = String.join("/", Arrays.asList(parts).subList(0, i)) +
                    (i != 0 ? "/" : "") +
                    String.join("$", Arrays.asList(parts).subList(i, parts.length));
            JClassLocation curr = JClassLocation.of(nameGuess);
            for (int j = 0; j < depth; j++) {
                curr = new JClassLocation(JClassLocation.Type.ARRAY, curr, "[]");
            }

            JResolutionResult<JClassLocation, JType> attempt = system.resolve(curr);
            children.add(attempt);
            if (attempt.success()) {
                break;
            }
        }

        return JResolutionResult.inherit(value, children);
    }
}
