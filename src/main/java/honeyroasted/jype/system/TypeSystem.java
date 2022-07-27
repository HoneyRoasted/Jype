package honeyroasted.jype.system;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypeArray;
import honeyroasted.jype.concrete.TypeClass;
import honeyroasted.jype.concrete.TypeNone;
import honeyroasted.jype.concrete.TypeNull;
import honeyroasted.jype.concrete.TypePrimitive;
import honeyroasted.jype.declaration.TypeDeclaration;
import honeyroasted.jype.system.cache.SimpleTypeCache;
import honeyroasted.jype.system.cache.TypeCache;

public class TypeSystem {
    public final TypeNone NONE = TypeNone.NONE;
    public final TypeNull NULL = TypeNull.NULL;

    public final TypeNone VOID = TypeNone.VOID;
    public final TypeClass VOID_BOX;

    public final TypePrimitive BOOLEAN = TypePrimitive.BOOLEAN;
    public final TypeClass BOOLEAN_BOX;

    public final TypePrimitive BYTE = TypePrimitive.BYTE;
    public final TypeClass BYTE_BOX;

    public final TypePrimitive SHORT = TypePrimitive.SHORT;
    public final TypeClass SHORT_BOX;

    public final TypePrimitive CHAR = TypePrimitive.CHAR;
    public final TypeClass CHAR_BOX;

    public final TypePrimitive INT = TypePrimitive.INT;
    public final TypeClass INT_BOX;

    public final TypePrimitive LONG = TypePrimitive.LONG;
    public final TypeClass LONG_BOX;

    public final TypePrimitive FLOAT = TypePrimitive.FLOAT;
    public final TypeClass FLOAT_BOX;

    public final TypePrimitive DOUBLE = TypePrimitive.DOUBLE;
    public final TypeClass DOUBLE_BOX;

    public final TypeClass OBJECT;
    public final TypeDeclaration OBJECT_CLASS;

    private TypeCache cache;

    public TypeSystem() {
        this(new SimpleTypeCache());
    }

    public TypeSystem(TypeCache cache) {
        this.cache = cache;

        VOID_BOX = of(Void.class);
        BOOLEAN_BOX = of(Boolean.class);
        BYTE_BOX = of(Byte.class);
        SHORT_BOX = of(Short.class);
        CHAR_BOX = of(Character.class);
        INT_BOX = of(Integer.class);
        LONG_BOX = of(Long.class);
        FLOAT_BOX = of(Float.class);
        DOUBLE_BOX = of(Double.class);
        OBJECT = of(Object.class);
        OBJECT_CLASS = declaration(Object.class);
    }

    public <T extends TypeConcrete> T of(java.lang.reflect.Type type) {
        if (type instanceof Class clazz) {
            if (clazz.isPrimitive()) {
                if (clazz == void.class) {
                    return (T) VOID;
                } else {
                    return (T) TypePrimitive.ALL.stream().filter(t -> t.reflectionClass() == clazz).findFirst().get();
                }
            } else if (clazz.isArray()) {
                return (T) new TypeArray(of(clazz.getComponentType()));
            } else {
                TypeClass cls = new TypeClass(declaration(clazz));
                this.cache.cache(cls.declaration().namespace().name(), cls);
                cls.lock();
                return (T) cls;
            }
        }

        return null;
    }

    public TypeDeclaration declaration(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray()) {
            throw new IllegalArgumentException("Cannot get declaration from primitive or array type");
        }

        Namespace namespace = Namespace.of(clazz);
        if (this.cache.has(namespace.name(), TypeDeclaration.class)) {
            return this.cache.get(namespace.name(), TypeDeclaration.class);
        } else {
            TypeDeclaration type = new TypeDeclaration(namespace);
            this.cache.cache(namespace.name(), type);

            if (clazz.getSuperclass() != null) {
                type.parents().add(of(clazz.getGenericSuperclass()));
            } else if (clazz.isInterface()) {
                type.parents().add(OBJECT);
            }

            for (java.lang.reflect.Type inter : clazz.getGenericInterfaces()) {
                type.parents().add(of(inter));
            }

            type.lock();
            return type;
        }
    }

    public TypeCache cache() {
        return this.cache;
    }
}
