package honeyroasted.jype.location;

public record JClassNamespace(JClassLocation location, JClassName name) implements JGenericDeclarationLocation {

    public static JClassNamespace of(Class<?> cls) {
        return new JClassNamespace(JClassLocation.of(cls), JClassName.of(cls));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JClassNamespace namespace && namespace.location().equals(this.location());
    }

    @Override
    public int hashCode() {
        return this.location().hashCode();
    }

    @Override
    public String toString() {
        return this.location().toString();
    }

    @Override
    public JClassLocation containingClass() {
        return this.location;
    }


}
