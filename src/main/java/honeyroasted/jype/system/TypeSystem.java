package honeyroasted.jype.system;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypeAnd;
import honeyroasted.jype.concrete.TypeArray;
import honeyroasted.jype.concrete.TypeClass;
import honeyroasted.jype.concrete.TypeIn;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.concrete.TypeNone;
import honeyroasted.jype.concrete.TypeNull;
import honeyroasted.jype.concrete.TypeOr;
import honeyroasted.jype.concrete.TypeOut;
import honeyroasted.jype.concrete.TypeParameterReference;
import honeyroasted.jype.concrete.TypePrimitive;
import honeyroasted.jype.declaration.TypeDeclaration;
import honeyroasted.jype.declaration.TypeParameter;
import honeyroasted.jype.system.cache.SimpleTypeCache;
import honeyroasted.jype.system.cache.TypeCache;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

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

    public TypeSolver newInference() {
        return new TypeSolver();
    }

    public TypePlaceholder newPlaceholder() {
        return new TypePlaceholder();
    }

    public TypeParameter newParameter(String name) {
        return new TypeParameter(name);
    }

    public TypeDeclaration newDeclaration(Namespace namespace) {
        return new TypeDeclaration(namespace);
    }

    public TypeClass newType(TypeDeclaration declaration) {
        return new TypeClass(declaration);
    }

    public TypeArray newArray(TypeConcrete element) {
        return new TypeArray(element);
    }

    public TypeArray newArray(TypeConcrete element, int dims) {
        if (dims < 1) {
            throw new IllegalArgumentException("Array must have at least 1 dimension");
        }

        TypeArray res = new TypeArray(element);
        for (int i = 0; i < dims - 1; i++) {
            res = new TypeArray(res);
        }
        return res;
    }

    public <T extends TypeConcrete> T token(Token token) {
        return of(((ParameterizedType) token.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }
    public  <T extends TypeConcrete> T of(java.lang.reflect.Type type) {
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
                if (this.cache.has(clazz.getName(), TypeClass.class)) {
                    return (T) this.cache.get(clazz.getName(), TypeClass.class);
                } else {
                    TypeClass cls = new TypeClass(declaration(clazz));
                    this.cache.cache(cls.declaration().namespace().name(), cls);
                    cls.lock();
                    return (T) cls;
                }
            }
        } else if (type instanceof ParameterizedType ptype) {
            java.lang.reflect.Type raw = ptype.getRawType();
            if (raw instanceof Class clazz) {
                if (this.cache.has(type, TypeClass.class)) {
                    return (T) this.cache.get(type, TypeClass.class);
                }
                TypeClass typeClass = new TypeClass(declaration(clazz));
                this.cache.cache(type, typeClass);
                for (java.lang.reflect.Type param : ptype.getActualTypeArguments()) {
                    typeClass.arguments().add(of(param));
                }
                typeClass.lock();
                return (T) typeClass;
            } else {
                throw new IllegalArgumentException("Unknown raw type: " + type.getClass().getName());
            }
        } else if (type instanceof WildcardType wtype) {
            if (this.cache.has(type, TypeIn.class)) {
                return (T) this.cache.get(type, TypeIn.class);
            } else if (this.cache.has(type, TypeOut.class)) {
                return (T) this.cache.get(type, TypeOut.class);
            }

            if (wtype.getLowerBounds().length != 0) {
                TypeIn typeIn = new TypeIn(and(wtype.getLowerBounds()));
                this.cache.cache(type, typeIn);
                return (T) typeIn;
            } else {
                TypeOut typeOut = new TypeOut(and(wtype.getUpperBounds()));
                this.cache.cache(type, typeOut);
                return (T) typeOut;
            }
        } else if (type instanceof TypeVariable<?> vtype) {
            if (this.cache.has(vtype, TypeParameter.class)) {
                return (T) new TypeParameterReference(this.cache.get(vtype, TypeParameter.class));
            } else {
                TypeParameter parameter = new TypeParameter(vtype.getName());
                this.cache.cache(vtype, parameter);
                parameter.setBound(and(vtype.getBounds()));
                parameter.lock();
                return (T) new TypeParameterReference(parameter);
            }
        } else if (type instanceof GenericArrayType atype) {
            return (T) new TypeArray(of(atype.getGenericComponentType()));
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getClass().getName());
        }
    }

    private TypeConcrete and(Type... array) {
        return new TypeAnd(Arrays.stream(array).map(t -> (TypeConcrete) of(t)).toList());
    }

    private TypeConcrete or(Type... array) {
        return new TypeOr(Arrays.stream(array).map(t -> (TypeConcrete) of(t)).toList());
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

            for (TypeVariable<?> var : clazz.getTypeParameters()) {
                if (this.cache.has(var, TypeParameter.class)) {
                    type.parameters().add(this.cache.get(var, TypeParameter.class));
                } else {
                    TypeParameter parameter = new TypeParameter(var.getName());
                    this.cache.cache(var, parameter);
                    parameter.setBound(and(var.getBounds()));
                    parameter.lock();
                    type.parameters().add(parameter);
                }
            }

            if (clazz.getSuperclass() != null) {
                type.parents().add(of(clazz.getGenericSuperclass()));
            } else if (clazz.isInterface()) {
                type.parents().add(of(Object.class));
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
