package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;

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
    public TypeString toSignature(TypeString.Context context) {
        return toDescriptor(context);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful("V");
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.successful("void");
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system) {
        return TypeConstraint.FALSE;
    }

    @Override
    public String toString() {
        return "<" + this.name + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeNone typeNone = (TypeNone) o;

        return Objects.equals(name, typeNone.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
