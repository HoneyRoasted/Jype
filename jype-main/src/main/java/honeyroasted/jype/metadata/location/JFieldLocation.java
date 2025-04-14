package honeyroasted.jype.metadata.location;

import honeyroasted.almonds.SimpleName;

import java.lang.reflect.Field;

public record JFieldLocation(String name, JClassLocation containing) implements SimpleName {

    public static JFieldLocation of(Field field) {
        return new JFieldLocation(field.getName(), JClassLocation.of(field.getDeclaringClass()));
    }

    @Override
    public String toString() {
        return containing + "." + this.name;
    }


    @Override
    public String simpleName() {
        return this.containing.simpleName() + "." + this.name;
    }
}
