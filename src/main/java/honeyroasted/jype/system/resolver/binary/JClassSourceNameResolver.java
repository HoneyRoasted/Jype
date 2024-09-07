package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassSourceName;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.util.Arrays;
import java.util.Optional;

public class JClassSourceNameResolver implements JTypeResolver<JClassSourceName, JType> {
    public static final JTypeResolver<JClassSourceName, JType> INSTANCE = new JClassSourceNameResolver();

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, JClassSourceName value) {
        String name = value.name();
        int depth = 0;

        while (name.endsWith("[]")) {
            depth += 1;
            name = name.substring(0, name.length() - 2);
        }

        String[] parts = value.name().split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String nameGuess = String.join("/", Arrays.asList(parts).subList(0, i)) +
                    (i != 0 ? "/" : "") +
                    String.join("$", Arrays.asList(parts).subList(i, parts.length));
            JClassLocation curr = JClassLocation.of(nameGuess);
            for (int j = 0; j < depth; j++) {
                curr = new JClassLocation(JClassLocation.Type.ARRAY, curr, "[]");
            }

            Optional<? extends JType> attempt = system.resolve(curr);
            if (attempt.isPresent()) {
                return attempt;
            }
        }
        return Optional.empty();
    }
}
