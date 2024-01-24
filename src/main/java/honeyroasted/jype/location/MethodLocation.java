package honeyroasted.jype.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public record MethodLocation(ClassNamespace containing, String name, ClassLocation returnType,
                             List<ClassLocation> parameters) implements GenericDeclarationLocation {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public MethodLocation(ClassNamespace containing, String name, ClassLocation returnType, List<ClassLocation> parameters) {
        this.containing = containing;
        this.name = name;
        this.returnType = returnType;
        this.parameters = List.copyOf(parameters);
    }

    public String simpleName() {
        return this.containing.name().simpleName() + "." + this.name;
    }

    public static MethodLocation of(Method method) {
        return new MethodLocation(
                ClassNamespace.of(method.getDeclaringClass()),
                method.getName(),
                ClassLocation.of(method.getReturnType()),
                Stream.of(method.getParameterTypes()).map(ClassLocation::of).toList());
    }

    public static MethodLocation of(Constructor cons) {
        return new MethodLocation(
                ClassNamespace.of(cons.getDeclaringClass()),
                CONSTRUCTOR_NAME,
                ClassLocation.VOID,
                Stream.of(cons.getParameterTypes()).map(ClassLocation::of).toList());
    }

    public boolean isConstructor() {
        return this.name.equals(CONSTRUCTOR_NAME);
    }

    @Override
    public ClassNamespace containingClass() {
        return this.containing;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.containing).append("::").append(this.name).append("->").append(this.returnType).append("(");

        for (int i = 0; i < this.parameters.size(); i++) {
            sb.append(this.parameters.get(i));
            if (i < this.parameters.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");
        return sb.toString();
    }
}
