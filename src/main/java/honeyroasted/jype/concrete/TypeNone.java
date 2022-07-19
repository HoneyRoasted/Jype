package honeyroasted.jype.concrete;

public class TypeNone implements TypeConcrete {
    public static final TypeNone VOID = new TypeNone("void", "V");
    public static final TypeNone UNKNOWN = new TypeNone("unknown", "V");
    public static final TypeNone NONE = new TypeNone("none", "V");

    private String name;
    private String descriptor;

    public TypeNone(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

}
