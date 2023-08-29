package honeyroasted.jype.type;

public class NoneType {
    public static final NoneType VOID = new NoneType("void");
    public static final NoneType NULL = new NoneType("null");
    public static final NoneType NONE = new NoneType("none");
    public static final NoneType ERROR = new NoneType("error");

    private String name;

    private NoneType(String name) {
        this.name = name;
    }
}
