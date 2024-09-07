package honeyroasted.jype.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public record JMethodLocation(JClassNamespace containing, String name, JClassLocation returnType,
                             List<JClassLocation> parameters) implements JGenericDeclarationLocation {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public JMethodLocation(JClassNamespace containing, String name, JClassLocation returnType, List<JClassLocation> parameters) {
        this.containing = containing;
        this.name = name;
        this.returnType = returnType;
        this.parameters = List.copyOf(parameters);
    }

    public String simpleName() {
        return this.containing.name().simpleName() + "." + this.name;
    }

    public static JMethodLocation of(Method method) {
        return new JMethodLocation(
                JClassNamespace.of(method.getDeclaringClass()),
                method.getName(),
                JClassLocation.of(method.getReturnType()),
                Stream.of(method.getParameterTypes()).map(JClassLocation::of).toList());
    }

    public static JMethodLocation of(Constructor cons) {
        return new JMethodLocation(
                JClassNamespace.of(cons.getDeclaringClass()),
                CONSTRUCTOR_NAME,
                JClassLocation.VOID,
                Stream.of(cons.getParameterTypes()).map(JClassLocation::of).toList());
    }

    public boolean isConstructor() {
        return this.name.equals(CONSTRUCTOR_NAME);
    }

    @Override
    public JClassNamespace containingClass() {
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
