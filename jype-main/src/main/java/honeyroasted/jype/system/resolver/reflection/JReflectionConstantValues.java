package honeyroasted.jype.system.resolver.reflection;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class JReflectionConstantValues {
    private static final Map<Class<?>, Map<String, Object>> CACHE = new ConcurrentHashMap<>();

    private JReflectionConstantValues() {
    }

    public static Optional<Object> constantValue(Field field) {
        if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) {
            return Optional.empty();
        }
        Class<?> type = field.getType();
        if (!type.isPrimitive() && type != String.class) {
            return Optional.empty();
        }
        Map<String, Object> values = CACHE.computeIfAbsent(field.getDeclaringClass(), JReflectionConstantValues::read);
        return Optional.ofNullable(values.get(field.getName()));
    }

    private static Map<String, Object> read(Class<?> cls) {
        Map<String, Object> result = new HashMap<>();
        String resource = cls.getName().replace('.', '/') + ".class";
        ClassLoader loader = cls.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : cls.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                return result;
            }
            ClassModel model = ClassFile.of().parse(in.readAllBytes());
            for (FieldModel fm : model.fields()) {
                // JLS §13.1 constant variables are recorded in the ConstantValue attribute
                fm.findAttribute(Attributes.constantValue()).ifPresent(cv -> result.put(fm.fieldName().stringValue(), cv.constant().constantValue()));
            }
        } catch (IOException | IllegalArgumentException e) {
            return result;
        }
        return result;
    }
}
