package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

public class TypeNone implements TypeConcrete {
    public static final TypeNone VOID = new TypeNone("void", "V");
    public static final TypeNone NONE = new TypeNone("none", "V");

    private String name;
    private String descriptor;

    public TypeNone(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return Constraint.FALSE;
    }
}
