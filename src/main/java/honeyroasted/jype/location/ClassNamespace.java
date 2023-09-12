package honeyroasted.jype.location;

public record ClassNamespace(ClassLocation location, ClassName name) {

    public static ClassNamespace of(Class<?> cls) {
        return new ClassNamespace(ClassLocation.of(cls), ClassName.of(cls));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassNamespace namespace && namespace.location().equals(this.location());
    }

    @Override
    public int hashCode() {
        return this.location().hashCode();
    }

    @Override
    public String toString() {
        return this.location().toString();
    }
}
