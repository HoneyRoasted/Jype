package honeyroasted.jype;

public class TypeString {
    private String value;
    private boolean successful;

    private Class<?> type;
    private Target target;

    public TypeString(String value, boolean successful, Class<?> type, Target target) {
        this.value = value;
        this.successful = successful;
        this.type = type;
        this.target = target;
    }
    
    public static TypeString successful(String value, Class<?> type, Target target) {
        return new TypeString(value, true, type, target);
    }
    
    public static TypeString failure(Class<?> type, Target target) {
        return new TypeString("<unable to convert type " + target + " to " + (type == null ? "null" : type.getSimpleName()) + ">",
                false, type, target);
    }

    public String value() {
        return this.value;
    }

    public boolean successful() {
        return this.successful;
    }

    public Class<?> type() {
        return this.type;
    }

    public Target target() {
        return this.target;
    }

    public enum Context {
        DECLARATION,
        CONCRETE
    }

    public enum Target {
        SOURCE,
        DESCRIPTOR,
        SIGNATURE,
        READABLE
    }
    
}
