package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * This is a {@link TypeResolver} for resolving {@link honeyroasted.jype.Type}s from the java reflection objects
 * {@link Type} and {@link Class}.
 */
public class ReflectionTypeResolver extends AbstractTypeResolver<Type, Class> {

    /**
     * Creates a new {@link ReflectionTypeResolver}.
     *
     * @param typeSystem The {@link TypeSystem} this {@link ReflectionTypeResolver} is resolving types for
     * @param cache      The {@link TypeCache} this {@link ReflectionTypeResolver} is using to cache resolved types
     */
    public ReflectionTypeResolver(TypeSystem typeSystem, TypeCache<? super Type> cache) {
        super(typeSystem, cache, Type.class, Class.class);
    }

    @Override
    public TypeConcrete resolve(Type type) {
        return of(type);
    }

    @Override
    public TypeDeclaration resolveDeclaration(Class type) {
        return declaration(type);
    }

    private TypeConcrete of(java.lang.reflect.Type type) {
        if (type instanceof Class clazz) {
            if (clazz.isPrimitive()) {
                if (clazz == void.class) {
                    return this.typeSystem().VOID;
                } else {
                    return this.typeSystem().ALL_PRIMITIVES.stream().filter(t -> t.reflectionClass() == clazz).findFirst().get();
                }
            } else if (clazz.isArray()) {
                return new TypeArray(this.typeSystem(), of(clazz.getComponentType()));
            } else {
                if (this.cache().has(clazz, TypeParameterized.class)) {
                    return this.cache().get(clazz, TypeParameterized.class);
                } else {
                    TypeParameterized cls = new TypeParameterized(this.typeSystem(), declaration(clazz));
                    this.cache().cache(clazz, cls);
                    cls.lock();
                    return cls;
                }
            }
        } else if (type instanceof ParameterizedType ptype) {
            java.lang.reflect.Type raw = ptype.getRawType();
            if (raw instanceof Class clazz) {
                if (this.cache().has(type, TypeParameterized.class)) {
                    return this.cache().get(type, TypeParameterized.class);
                }
                TypeParameterized typeParameterized = new TypeParameterized(this.typeSystem(), declaration(clazz));
                this.cache().cache(type, typeParameterized);
                for (java.lang.reflect.Type param : ptype.getActualTypeArguments()) {
                    typeParameterized.arguments().add(of(param));
                }
                typeParameterized.lock();
                return typeParameterized;
            } else {
                throw new IllegalArgumentException("Unknown raw type: " + type.getClass().getName());
            }
        } else if (type instanceof WildcardType wtype) {
            if (wtype.getLowerBounds().length != 0) {
                return new TypeIn(this.typeSystem(), and(wtype.getLowerBounds()));
            } else {
                return new TypeOut(this.typeSystem(), and(wtype.getUpperBounds()));
            }
        } else if (type instanceof TypeVariable<?> vtype) {
            if (this.cache().has(vtype, TypeParameter.class)) {
                return this.cache().get(vtype, TypeParameter.class);
            } else {
                TypeParameter parameter = new TypeParameter(this.typeSystem(), vtype.getName());
                this.cache().cache(vtype, parameter);
                parameter.setUpperBound(and(vtype.getBounds()));
                parameter.lock();
                return parameter;
            }
        } else if (type instanceof GenericArrayType atype) {
            return new TypeArray(this.typeSystem(), of(atype.getGenericComponentType()));
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getClass().getName());
        }
    }

    private TypeConcrete and(Type... array) {
        TypeAnd and = new TypeAnd(this.typeSystem(), Arrays.stream(array).map(this::of).collect(Collectors.toCollection(LinkedHashSet::new)));
        and.lock();
        return and;
    }

    private TypeDeclaration declaration(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray()) {
            throw new IllegalArgumentException("Cannot get declaration from primitive or array type");
        }

        Namespace namespace = Namespace.of(clazz);
        if (this.cache().has(clazz, TypeDeclaration.class)) {
            return this.cache().get(clazz, TypeDeclaration.class);
        } else {
            TypeDeclaration type = new TypeDeclaration(this.typeSystem(), namespace, clazz.isInterface());
            this.cache().cache(clazz, type);

            for (TypeVariable<?> var : clazz.getTypeParameters()) {
                if (this.cache().has(var, TypeParameter.class)) {
                    type.parameters().add(this.cache().get(var, TypeParameter.class));
                } else {
                    TypeParameter parameter = new TypeParameter(this.typeSystem(), var.getName());
                    this.cache().cache(var, parameter);
                    parameter.setUpperBound(and(var.getBounds()));
                    parameter.lock();
                    type.parameters().add(parameter);
                }
            }

            if (clazz.getSuperclass() != null) {
                type.parents().add((TypeParameterized) of(clazz.getGenericSuperclass()));
            } else if (clazz.isInterface()) {
                type.parents().add((TypeParameterized) of(Object.class));
            }

            for (java.lang.reflect.Type inter : clazz.getGenericInterfaces()) {
                type.parents().add((TypeParameterized) of(inter));
            }

            type.lock();
            return type;
        }
    }

}
