package honeyroasted.jype.metadata.location;

import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public record JMethodLocation(JClassLocation containing, String name, JClassLocation returnType,
                             List<JClassLocation> parameters) implements JGenericDeclarationLocation {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public JMethodLocation(JClassLocation containing, String name, JClassLocation returnType, List<JClassLocation> parameters) {
        this.containing = containing;
        this.name = name;
        this.returnType = returnType;
        this.parameters = List.copyOf(parameters);
    }

    public static JMethodLocation of(String name, JClassLocation containing, MethodTypeDesc desc) {
        return new JMethodLocation(
                containing,
                name,
                JClassLocation.of(desc.returnType()),
                desc.parameterList().stream().map(JClassLocation::of).toList()
        );
    }

    public String simpleName() {
        return this.containing + "." + this.name;
    }

    public static JMethodLocation of(Method method) {
        return new JMethodLocation(
                JClassLocation.of(method.getDeclaringClass()),
                method.getName(),
                JClassLocation.of(method.getReturnType()),
                Stream.of(method.getParameterTypes()).map(JClassLocation::of).toList());
    }

    public static JMethodLocation of(Constructor<?> cons) {
        return new JMethodLocation(
                JClassLocation.of(cons.getDeclaringClass()),
                CONSTRUCTOR_NAME,
                JClassLocation.VOID,
                Stream.of(cons.getParameterTypes()).map(JClassLocation::of).toList());
    }

    public boolean isConstructor() {
        return this.name.equals(CONSTRUCTOR_NAME);
    }

    @Override
    public JClassLocation containingClass() {
        return this.containing;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.containing).append("::").append(this.name).append("{(");

        for (int i = 0; i < this.parameters.size(); i++) {
            sb.append(this.parameters.get(i));
            if (i < this.parameters.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")->").append(this.returnType).append("}");
        return sb.toString();
    }
}
