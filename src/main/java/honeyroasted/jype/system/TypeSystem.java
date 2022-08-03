package honeyroasted.jype.system;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.cache.SimpleTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Elements;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeSystem {
    public static final TypeSystem GLOBAL = new TypeSystem();

    public final TypeNone NONE;
    public final TypeNull NULL;

    public final TypeNone VOID;
    public final TypeClass VOID_BOX;

    public final TypePrimitive BOOLEAN;
    public final TypeClass BOOLEAN_BOX;

    public final TypePrimitive BYTE;
    public final TypeClass BYTE_BOX;

    public final TypePrimitive SHORT;
    public final TypeClass SHORT_BOX;

    public final TypePrimitive CHAR;
    public final TypeClass CHAR_BOX;

    public final TypePrimitive INT;
    public final TypeClass INT_BOX;

    public final TypePrimitive LONG;
    public final TypeClass LONG_BOX;

    public final TypePrimitive FLOAT;
    public final TypeClass FLOAT_BOX;

    public final TypePrimitive DOUBLE;
    public final TypeClass DOUBLE_BOX;

    public final TypeClass OBJECT;
    public final TypeDeclaration OBJECT_CLASS;

    public final Set<TypePrimitive> ALL_PRIMITIVES;

    private final Map<TypePrimitive, Class<?>> PRIM_TO_BOX;
    private final Map<Namespace, TypePrimitive> BOX_TO_PRIM;

    private TypeCache cache;

    public TypeSystem() {
        this(new SimpleTypeCache());
    }

    public TypeSystem(TypeCache cache) {
        this.cache = cache;
        
        NONE = new TypeNone(this, "none", "V");
        NULL = new TypeNull(this);
        
        VOID = new TypeNone(this, "void", "V");
        BOOLEAN = new TypePrimitive(this, boolean.class, "Z");
        BYTE = new TypePrimitive(this, byte.class, "B");
        SHORT = new TypePrimitive(this, short.class, "S");
        CHAR = new TypePrimitive(this, char.class, "C");
        INT = new TypePrimitive(this, int.class, "I");
        LONG = new TypePrimitive(this, long.class, "J");
        FLOAT = new TypePrimitive(this, float.class, "F");
        DOUBLE = new TypePrimitive(this, double.class, "D");
        
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

        ALL_PRIMITIVES = Set.of(BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE);

        PRIM_TO_BOX = Map.of(
                BOOLEAN, Boolean.class,
                BYTE, Byte.class,
                SHORT, Short.class,
                CHAR, Character.class,
                INT, Integer.class,
                LONG, Long.class,
                FLOAT, Float.class,
                DOUBLE, Double.class
        );

        BOX_TO_PRIM = reverse(PRIM_TO_BOX, Namespace::of);
    }

    public TypeParameter newParameter(String name) {
        return new TypeParameter(this, name);
    }

    public TypeDeclaration newDeclaration(Namespace namespace) {
        return new TypeDeclaration(this, namespace);
    }

    public TypeClass newType(TypeDeclaration declaration) {
        return new TypeClass(this, declaration);
    }

    public TypeArray newArray(TypeConcrete element) {
        return new TypeArray(this, element);
    }

    public TypeArray newArray(TypeConcrete element, int dims) {
        if (dims < 1) {
            throw new IllegalArgumentException("Array must have at least 1 dimension");
        }

        TypeArray res = new TypeArray(this, element);
        for (int i = 0; i < dims - 1; i++) {
            res = new TypeArray(this, res);
        }
        return res;
    }

    public <T extends TypeConcrete> T token(TypeToken token) {
        return of(((ParameterizedType) token.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    public Optional<TypePrimitive> unbox(TypeConcrete type) {
        return type instanceof TypeClass cls ? Optional.ofNullable(BOX_TO_PRIM.get(cls.declaration().namespace())) :
                Optional.empty();
    }

    public TypeConcrete box(TypePrimitive type) {
        return of(PRIM_TO_BOX.get(type));
    }

    public <T extends TypeConcrete> T of(java.lang.reflect.Type type) {
        if (type instanceof Class clazz) {
            if (clazz.isPrimitive()) {
                if (clazz == void.class) {
                    return (T) VOID;
                } else {
                    return (T) ALL_PRIMITIVES.stream().filter(t -> t.reflectionClass() == clazz).findFirst().get();
                }
            } else if (clazz.isArray()) {
                return (T) new TypeArray(this, of(clazz.getComponentType()));
            } else {
                if (this.cache.has(clazz.getName(), TypeClass.class)) {
                    return (T) this.cache.get(clazz.getName(), TypeClass.class);
                } else {
                    TypeClass cls = new TypeClass(this, declaration(clazz));
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
                TypeClass typeClass = new TypeClass(this, declaration(clazz));
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
            if (wtype.getLowerBounds().length != 0) {
                TypeIn typeIn = new TypeIn(this, and(wtype.getLowerBounds()));
                return (T) typeIn;
            } else {
                TypeOut typeOut = new TypeOut(this, and(wtype.getUpperBounds()));
                return (T) typeOut;
            }
        } else if (type instanceof TypeVariable<?> vtype) {
            if (this.cache.has(vtype, TypeParameter.class)) {
                return (T) this.cache.get(vtype, TypeParameter.class);
            } else {
                TypeParameter parameter = new TypeParameter(this, vtype.getName());
                this.cache.cache(vtype, parameter);
                parameter.setBound(and(vtype.getBounds()));
                parameter.lock();
                return (T) parameter;
            }
        } else if (type instanceof GenericArrayType atype) {
            return (T) new TypeArray(this, of(atype.getGenericComponentType()));
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getClass().getName());
        }
    }

    private TypeConcrete and(Type... array) {
        TypeAnd and = new TypeAnd(this, Arrays.stream(array).map(t -> (TypeConcrete) of(t)).collect(Collectors.toCollection(LinkedHashSet::new)));
        and.lock();
        return and;
    }

    private TypeConcrete or(Type... array) {
        TypeOr or = new TypeOr(this, Arrays.stream(array).map(t -> (TypeConcrete) of(t)).collect(Collectors.toCollection(LinkedHashSet::new)));
        or.lock();
        return or;
    }

    private TypeConcrete and(List<? extends TypeMirror> bounds, Elements elements) {
        TypeAnd and = new TypeAnd(this, bounds.stream().map(t -> (TypeConcrete) of(t, elements)).collect(Collectors.toCollection(LinkedHashSet::new)));
        and.lock();
        return and;
    }

    private TypeConcrete or(List<? extends TypeMirror> bounds, Elements elements) {
        TypeOr or = new TypeOr(this, bounds.stream().map(t -> (TypeConcrete) of(t, elements)).collect(Collectors.toCollection(LinkedHashSet::new)));
        or.lock();
        return or;
    }

    public TypeDeclaration declaration(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray()) {
            throw new IllegalArgumentException("Cannot get declaration from primitive or array type");
        }

        Namespace namespace = Namespace.of(clazz);
        if (this.cache.has(namespace.name(), TypeDeclaration.class)) {
            return this.cache.get(namespace.name(), TypeDeclaration.class);
        } else {
            TypeDeclaration type = new TypeDeclaration(this, namespace);
            this.cache.cache(namespace.name(), type);

            for (TypeVariable<?> var : clazz.getTypeParameters()) {
                if (this.cache.has(var, TypeParameter.class)) {
                    type.parameters().add(this.cache.get(var, TypeParameter.class));
                } else {
                    TypeParameter parameter = new TypeParameter(this, var.getName());
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

    public <T extends TypeConcrete> T of(TypeMirror type, Elements elements) {
        if (type.getKind().isPrimitive() && type instanceof PrimitiveType primitiveType) {
            return (T) switch (primitiveType.getKind()) {
                case BOOLEAN -> BOOLEAN;
                case BYTE -> BYTE;
                case SHORT -> SHORT;
                case INT -> INT;
                case LONG -> LONG;
                case CHAR -> CHAR;
                case FLOAT -> FLOAT;
                case DOUBLE -> DOUBLE;
                default -> throw new IllegalArgumentException("Unknown primitive type: " + primitiveType.getKind());
            };
        } else if (type.getKind() == TypeKind.VOID) {
            return (T) VOID;
        } else if (type.getKind() == TypeKind.NONE) {
            return (T) NONE;
        } else if (type.getKind() == TypeKind.NULL) {
            return (T) NULL;
        } else if (type.getKind() == TypeKind.ARRAY && type instanceof ArrayType arrayType) {
            return (T) new TypeArray(this, of(arrayType.getComponentType(), elements));
        } else if (type.getKind() == TypeKind.WILDCARD && type instanceof javax.lang.model.type.WildcardType wildcardType) {
            if (wildcardType.getSuperBound() != null) {
                return (T) new TypeIn(this, of(wildcardType.getSuperBound(), elements));
            } else if (wildcardType.getExtendsBound() != null) {
                return (T) new TypeOut(this, of(wildcardType.getExtendsBound(), elements));
            } else {
                return (T) new TypeOut(this, of(Object.class));
            }
        } else if (type.getKind() == TypeKind.TYPEVAR && type instanceof javax.lang.model.type.TypeVariable typeVariable) {
            TypeParameterElement var = (TypeParameterElement) typeVariable.asElement();
            if (this.cache.has(typeVariable, TypeParameter.class)) {
                return (T) this.cache.get(typeVariable, TypeParameter.class);
            } else {
                TypeParameter parameter = new TypeParameter(this, var.getSimpleName().toString());
                this.cache.cache(var.asType(), parameter);
                parameter.setBound(and(var.getBounds(), elements));
                parameter.lock();
                return (T) parameter;
            }
        } else if (type.getKind() == TypeKind.UNION && type instanceof UnionType unionType) {
            return (T) or(unionType.getAlternatives(), elements);
        } else if (type.getKind() == TypeKind.INTERSECTION && type instanceof IntersectionType intersectionType) {
            return (T) and(intersectionType.getBounds(), elements);
        } else if (type.getKind() == TypeKind.DECLARED && type instanceof DeclaredType declared && declared.asElement() instanceof TypeElement) {
            Namespace namespace = namespace(declared, elements);
            if (declared.getTypeArguments().isEmpty()) {
                if (this.cache.has(namespace.name(), TypeClass.class)) {
                    return (T) this.cache.get(namespace.name(), TypeClass.class);
                } else {
                    TypeClass cls = new TypeClass(this, declaration(declared, elements));
                    this.cache.cache(namespace.name(), cls);
                    cls.lock();
                    return (T) cls;
                }
            } else {
                if (this.cache.has(declared, TypeClass.class)) {
                    return (T) this.cache.get(declared, TypeClass.class);
                } else {
                    TypeClass cls = new TypeClass(this, declaration(declared, elements));
                    this.cache.cache(namespace.name(), cls);
                    for (TypeMirror arg : declared.getTypeArguments()) {
                        cls.arguments().add(of(arg, elements));
                    }
                    cls.lock();
                    return (T) cls;
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getKind() + ", " + type);
        }
    }

    public TypeDeclaration declaration(DeclaredType declared, Elements elements) {
        Element element = declared.asElement();
        if ((element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE ||
                element.getKind() == ElementKind.ANNOTATION_TYPE) && element instanceof TypeElement typeElement) {

            Namespace namespace = namespace(declared, elements);
            if (this.cache.has(namespace.name(), TypeDeclaration.class)) {
                return this.cache.get(namespace.name(), TypeDeclaration.class);
            } else {
                TypeDeclaration type = new TypeDeclaration(this, namespace);
                this.cache.cache(namespace.name(), type);

                for (TypeParameterElement var : ((TypeElement) element).getTypeParameters()) {
                    if (this.cache.has(var.asType(), TypeParameter.class)) {
                        type.parameters().add(this.cache.get(var.asType(), TypeParameter.class));
                    } else {
                        TypeParameter parameter = new TypeParameter(this, var.getSimpleName().toString());
                        this.cache.cache(var.asType(), parameter);
                        parameter.setBound(and(var.getBounds(), elements));
                        parameter.lock();
                        type.parameters().add(parameter);
                    }
                }

                TypeMirror superclass = typeElement.getSuperclass();
                if (superclass.getKind() != TypeKind.NONE) {
                    type.parents().add(of(superclass, elements));
                } else if (typeElement.getKind() == ElementKind.INTERFACE) {
                    type.parents().add(of(Object.class));
                }

                for (TypeMirror inter : typeElement.getInterfaces()) {
                    type.parents().add(of(inter, elements));
                }

                type.lock();
                return type;
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + declared.getKind() + ", " + declared);
        }
    }

    private static Namespace namespace(DeclaredType type, Elements elements) {
        return Namespace.binary(elements.getBinaryName((TypeElement) type.asElement()).toString());
    }

    public TypeCache cache() {
        return this.cache;
    }

    private static <K, V, T> Map<T, V> reverse(Map<V, K> map, Function<K, T> keyFunction) {
        Map<T, V> result = new HashMap<>();
        map.forEach((key, val) -> result.put(keyFunction.apply(val), key));
        return Map.copyOf(result);
    }
}
