package honeyroasted.jype.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public record MethodLocation(ClassLocation containing, String name, ClassLocation returnType, List<ClassLocation> parameters) implements TypeParameterHost {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public MethodLocation(ClassLocation containing, String name, ClassLocation returnType, List<ClassLocation> parameters) {
        this.containing = containing;
        this.name = name;
        this.returnType = returnType;
        this.parameters = List.copyOf(parameters);
    }

    public static MethodLocation of(Method method) {
        return new MethodLocation(
                ClassLocation.of(method.getDeclaringClass()),
                method.getName(),
                ClassLocation.of(method.getReturnType()),
                Stream.of(method.getParameterTypes()).map(ClassLocation::of).toList());
    }

    public static MethodLocation of(Constructor cons) {
        return new MethodLocation(
                ClassLocation.of(cons.getDeclaringClass()),
                "<init>",
                ClassLocation.VOID,
                Stream.of(cons.getParameterTypes()).map(ClassLocation::of).toList());
    }

    @Override
    public ClassLocation containingClass() {
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
