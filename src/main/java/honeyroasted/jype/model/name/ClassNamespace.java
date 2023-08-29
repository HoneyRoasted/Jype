package honeyroasted.jype.model.name;

public record ClassNamespace(ClassLocation location, ClassName name) {

    public static ClassNamespace of(Class<?> cls) {
        return new ClassNamespace(ClassLocation.of(cls), ClassName.of(cls));
    }

}
