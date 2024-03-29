package honeyroasted.jype.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

public record TypeParameterLocation(GenericDeclarationLocation containing, String name) {

    public static TypeParameterLocation of(GenericDeclaration declaration, String name) {
        GenericDeclarationLocation containing;
        if (declaration instanceof Class<?> cls) {
            containing = ClassNamespace.of(cls);
        } else if (declaration instanceof Method mth) {
            containing = MethodLocation.of(mth);
        } else if (declaration instanceof Constructor<?> cons) {
            containing = MethodLocation.of(cons);
        } else {
            throw new IllegalArgumentException("Unknown GenericDeclaration type: " + declaration);
        }

        return new TypeParameterLocation(containing, name);
    }

    public static TypeParameterLocation of(TypeVariable<?> tVar) {
        return of(tVar.getGenericDeclaration(), tVar.getName());
    }

    public boolean isVirtual() {
        return this.containing == null;
    }

    @Override
    public String toString() {
        return this.containing == null ? this.name : this.containing + ".#" + this.name;
    }
}
