package honeyroasted.jype.system.resolver;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassSourceName;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.Arrays;
import java.util.Optional;

public class ClassSourceNameResolver implements TypeResolver<ClassSourceName, Type> {
    public static TypeResolver<ClassSourceName, Type> INSTANCE = new ClassSourceNameResolver();

    @Override
    public Optional<? extends Type> resolve(TypeSystem system, ClassSourceName value) {
        String name = value.name();
        int depth = 0;

        if (name.endsWith("...")) {
            depth += 1;
            name = name.substring(0, name.length() - 3);
        }

        while (name.endsWith("[]")) {
            depth += 1;
            name = name.substring(0, name.length() - 2);
        }


        String[] parts = value.name().split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String nameGuess = String.join("/", Arrays.asList(parts).subList(0, i)) +
                            (i != 0 ? "/" : "") +
                            String.join("$", Arrays.asList(parts).subList(i, parts.length));
            ClassLocation curr = ClassLocation.of(nameGuess);
            for (int j = 0; j < depth; j++) {
                curr = new ClassLocation(ClassLocation.Type.ARRAY, curr, "[]");
            }

            Optional<? extends Type> attempt = system.resolve(curr);
            if (attempt.isPresent()) {
                return attempt;
            }
        }
        return Optional.empty();
    }

}
