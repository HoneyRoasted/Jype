package honeyroasted.jype.metadata.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

public record JTypeParameterLocation(JGenericDeclarationLocation containing, String name) {

    public static JTypeParameterLocation of(GenericDeclaration declaration, String name) {
        JGenericDeclarationLocation containing;
        if (declaration instanceof Class<?> cls) {
            containing = JClassNamespace.of(cls);
        } else if (declaration instanceof Method mth) {
            containing = JMethodLocation.of(mth);
        } else if (declaration instanceof Constructor<?> cons) {
            containing = JMethodLocation.of(cons);
        } else {
            throw new IllegalArgumentException("Unknown GenericDeclaration type: " + declaration);
        }

        return new JTypeParameterLocation(containing, name);
    }

    public static JTypeParameterLocation of(TypeVariable<?> tVar) {
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
