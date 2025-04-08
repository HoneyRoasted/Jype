package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.impl.delegate.JMethodReferenceDelegate;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface JReflectionTypeResolution {

    JBundledTypeResolvers REFLECTION_TYPE_RESOLVERS = new JBundledTypeResolvers(
            new JReflectionTypeTokenResolver(),
            new JReflectionJavaTypeResolver(),
            new JReflectionJavaMethodResolver(),
            new JReflectionClassReferenceResolver(),
            new JReflectionMethodReferenceResolver(),
            new JReflectionTypeParameterResolver()
    );

    static boolean locationMatchesMethod(JMethodLocation methodLocation, Executable method) {
        JClassLocation retType = method instanceof Method mth ? JClassLocation.of(mth.getReturnType()) : JClassLocation.of(void.class);
        String name = method instanceof Constructor<?> ? "<init>" : method.getName();

        if (methodLocation.name().equals(name) &&
                methodLocation.containing().equals(JClassLocation.of(method.getDeclaringClass())) &&
                methodLocation.returnType().equals(retType) &&
                methodLocation.parameters().size() == method.getParameterCount()) {
            Class[] parameters = method.getParameterTypes();

            for (int i = 0; i < parameters.length; i++) {
                if (!methodLocation.parameters().get(i).equals(JClassLocation.of(parameters[i]))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    static <T> Optional<T> getReflectionType(JType type) {
        try {
            if (type.metadata().has(JReflectionType.class)) {
                JReflectionType<?> meta = type.metadata().first(JReflectionType.class).get();
                return (Optional<T>) Optional.of(meta.type());
            } else if (type instanceof JClassReference cr) {
                return (Optional<T>) Optional.of(classFromLocation(cr.namespace().location()));
            } else if (type instanceof JMethodReference mr) {
                return (Optional<T>) Optional.of(methodFromLocation(mr.location()));
            } else if (type instanceof JVarType vr) {
                return (Optional<T>) Optional.of(typeParameterFromLocation(vr.location()));
            }
            return Optional.empty();
        } catch (JResolutionFailedException | JReflectionLookupException ex) {
            return Optional.empty();
        }
    }

    static Optional<Class<?>> getReflectionType(JClassReference reference) {
        return getReflectionType((JType) reference);
    }

    static Optional<Executable> getReflectionType(JMethodReference reference) {
        return getReflectionType((JType) reference);
    }

    static Optional<TypeVariable<?>> getReflectionType(JVarType type) {
        return getReflectionType((JType) type);
    }

    static Class<?> classFromLocation(JClassLocation location) throws JResolutionFailedException {
        try {
            int depth = 0;
            JClassLocation current = location;
            while (current.isArray()) {
                current = current.containing();
                depth++;
            }

            Class<?> target = Class.forName(current.toRuntimeName());

            if (depth != 0) {
                target = Array.newInstance(target, new int[depth]).getClass();
            }

            return target;
        } catch (ClassNotFoundException ex) {
            throw new JReflectionLookupException("Failed to resolve class (runtime lookup failed): " + location, ex);
        }
    }

    static Executable methodFromLocation(JMethodLocation location) throws JResolutionFailedException {
        Class<?> targetCls;
        try {
            targetCls = classFromLocation(location.containing());
        } catch (JResolutionFailedException | JReflectionLookupException ex) {
            throw new JReflectionLookupException("Could not resolve method (containing class not found): " + location, ex);
        }
        Executable targetMethod = null;

        if (location.isConstructor()) {
            Constructor[] cons = targetCls.getDeclaredConstructors();
            for (Constructor con : cons) {
                if (locationMatchesMethod(location, con)) {
                    targetMethod = con;
                    break;
                }
            }
        } else {
            Method[] methods = targetCls.getDeclaredMethods();
            for (Method method : methods) {
                if (locationMatchesMethod(location, method)) {
                    targetMethod = method;
                    break;
                }
            }
        }

        if (targetMethod == null) {
            throw new JReflectionLookupException("Could not resolve method (matching method not found): " + location);
        }

        return targetMethod;
    }

    static TypeVariable<?> typeParameterFromLocation(JTypeParameterLocation location) throws JResolutionFailedException {
        if (location.isVirtual()) {
            throw new JReflectionLookupException("Could not resolve type parameter from virtual location: " + location);
        }

        TypeVariable[] parameters = null;

        try {
            if (location.containing() instanceof JClassNamespace containing) {
                parameters = JReflectionTypeResolution.classFromLocation(containing.location()).getTypeParameters();
            } else if (location.containing() instanceof JMethodLocation containing) {
                parameters = JReflectionTypeResolution.methodFromLocation(containing).getTypeParameters();
            }
        } catch (JResolutionFailedException | JReflectionLookupException ex) {
            throw new JReflectionLookupException("Could not resolve type parameter (no generic declaration found): " + location, ex);
        }

        if (parameters == null) {
            throw new JReflectionLookupException("Could not resolve type parameter (no type parameters found): " + location);
        }

        TypeVariable varTarget = null;
        for (TypeVariable var : parameters) {
            if (var.getName().equals(location.name())) {
                varTarget = var;
                break;
            }
        }

        if (varTarget != null) {
            return varTarget;
        } else {
            throw new JReflectionLookupException("Could not resolve type parameter (no matching name found): " + location);
        }
    }

    static JResolutionResult<Executable, JMethodReference> createMethodReference(JTypeSystem system, Executable executable, JMethodLocation location) {
        List<JResolutionResult<?, ?>> children = new ArrayList<>();

        JMethodReference mRef = system.typeFactory().newMethodReference();
        mRef.metadata().attach(new JReflectionType.Executable(executable));
        mRef.setLocation(location);
        mRef.setModifiers(executable.getModifiers());

        system.storage().cacheFor(JMethodLocation.class).put(location, mRef);

        JResolutionResult<Type, JType> outerClass = system.resolve(Type.class, JType.class, executable.getDeclaringClass());
        children.add(outerClass);
        if (outerClass.success() && outerClass.value() instanceof JClassReference cr) {
            mRef.setOuterClass(cr);
        } else {
            system.storage().cacheFor(JMethodLocation.class).remove(location);
            return new JResolutionResult<>("Failed to resolve declaring class", executable, children);
        }

        if (executable instanceof Method method) {
            JResolutionResult<Type, JType> returnType = system.resolve(Type.class, JType.class, method.getGenericReturnType());
            children.add(returnType);
            if (returnType.success()) {
                mRef.setReturnType(returnType.value());
            } else {
                system.storage().cacheFor(JMethodLocation.class).remove(location);
                return new JResolutionResult<>("Failed to resolve return type", executable, children);
            }
        } else {
            mRef.setReturnType(system.constants().voidType());
        }

        List<JType> resolvedExceptions = new ArrayList<>();
        for (Type except : executable.getGenericExceptionTypes()) {
            JResolutionResult<Type, JType> resolved = system.resolve(Type.class, JType.class, except);
            children.add(resolved);
            if (resolved.success()) {
                resolvedExceptions.add(resolved.value());
            } else {
                system.storage().cacheFor(JMethodLocation.class).remove(location);
                return new JResolutionResult<>("Failed to resolve exception type", executable, children);
            }
        }

        List<JType> resolvedParams = new ArrayList<>();
        for (Type param : executable.getGenericParameterTypes()) {
            JResolutionResult<Type, JType> resolved = system.resolve(Type.class, JType.class, param);
            children.add(resolved);
            if (resolved.success()) {
                resolvedParams.add(resolved.value());
            } else {
                system.storage().cacheFor(JMethodLocation.class).remove(location);
                return new JResolutionResult<>("Failed to resolve parameter type", executable, children);
            }
        }

        List<JVarType> resolvedTypeParams = new ArrayList<>();
        for (TypeVariable<?> tvar : executable.getTypeParameters()) {
            JResolutionResult<Type, JType> resolved = system.resolve(Type.class, JType.class, tvar);
            children.add(resolved);
            if (resolved.success() && resolved.value() instanceof JVarType res) {
                resolvedTypeParams.add(res);
            } else {
                system.storage().cacheFor(JMethodLocation.class).remove(location);
                return new JResolutionResult<>("failed to resolve type parameter", executable, children);
            }
        }

        mRef.setExceptionTypes(resolvedExceptions);
        mRef.setParameters(resolvedParams);
        mRef.setTypeParameters(resolvedTypeParams);
        mRef.setUnmodifiable(true);
        system.storage().cacheFor(JMethodLocation.class).put(mRef.location(), mRef);
        system.storage().cacheFor(Executable.class).put(executable, mRef);

        return new JResolutionResult<>(mRef, executable, children);
    }

    static JResolutionResult<Class<?>, JType> createClassReference(JTypeSystem system, Class<?> cls, JClassLocation location) {
        if (cls.isPrimitive()) {
            if (cls.equals(void.class)) {
                return new JResolutionResult<>(system.constants().voidType(), cls);
            } else {
                return JResolutionResult.inherit(cls, system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(JClassLocation.of(cls))).findFirst(),
                        "Unknown primitive type");
            }
        } else if (cls.isArray()) {
            return createClassReference(system, cls.componentType(), location.containing()).map(cls, c -> {
                JArrayType type = system.typeFactory().newArrayType();
                type.setComponent(c);
                type.setUnmodifiable(true);
                type.metadata().attach(new JReflectionType.Type(cls));
                return type;
            }, "Failed to create array type");
        }

        List<JResolutionResult<?, ?>> children = new ArrayList<>();

        JClassReference reference = system.typeFactory().newClassReference();

        reference.metadata().attach(new JReflectionType.Type(cls));
        reference.setNamespace(JClassNamespace.of(cls));
        reference.setModifiers(cls.getModifiers());

        system.storage().cacheFor(Type.class).put(cls, reference);
        system.storage().cacheFor(JClassLocation.class).put(reference.namespace().location(), reference);

        if (cls.getSuperclass() != null) {
            JResolutionResult<Type, JType> superCls = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, cls.getGenericSuperclass());
            children.add(superCls);

            if (superCls.failure() || !(superCls.value() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve super class", cls, children);
            } else {
                reference.setSuperClass((JClassType) superCls.value());
            }
        } else if (!cls.equals(Object.class)) {
            JResolutionResult<Type, JType> objClass = system.resolve(Object.class);
            children.add(objClass);
            if (objClass.success() && objClass.value() instanceof JClassType ct) {
                reference.setSuperClass(ct);
            } else {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve super class", cls, children);
            }
        }

        if (cls.getEnclosingClass() != null) {
            JResolutionResult<Type, JType> enclosing = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, cls.getEnclosingClass());
            children.add(enclosing);

            if (enclosing.failure() || !(enclosing.value() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve enclosing class", cls, children);
            } else {
                reference.setOuterClass(((JClassType) enclosing.value()).classReference());
            }
        }

        if (cls.getEnclosingMethod() != null || cls.getEnclosingConstructor() != null) {
            Executable enclsoingExecutable = cls.getEnclosingMethod() != null ? cls.getEnclosingMethod() : cls.getEnclosingConstructor();

            JResolutionResult<Executable, JType> enclosing = system.resolvers().resolverFor(Executable.class, JType.class)
                    .resolve(system, enclsoingExecutable);
            children.add(enclosing);

            if (enclosing.failure() || !(enclosing.value() instanceof JMethodType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve enclosing method", cls, children);
            } else {
                reference.setOuterMethod(((JMethodType) enclosing.value()).methodReference());
            }
        }

        for (Type inter : cls.getGenericInterfaces()) {
            JResolutionResult<Type, JType> interRef = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, inter);
            children.add(interRef);

            if (interRef.failure() || !(interRef.value() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve interface", cls, children);
            } else {
                reference.interfaces().add((JClassType) interRef.value());
            }
        }

        for (TypeVariable<?> param : cls.getTypeParameters()) {
            JTypeParameterLocation loc = new JTypeParameterLocation(reference.namespace(), param.getName());
            JResolutionResult<TypeVariable<?>, JVarType> paramRef = createVarType(system, param, loc);
            children.add(paramRef);

            if (paramRef.failure()) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return new JResolutionResult<>("Failed to resolve type parameter", cls, children);
            } else {
                reference.typeParameters().add(paramRef.value());
            }
        }

        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            JMethodLocation loc = JMethodLocation.of(constructor);
            reference.declaredMethods().add(new JMethodReferenceDelegate(system, s -> s.tryResolve(loc)));
        }

        for (Method method : cls.getDeclaredMethods()) {
            JMethodLocation loc = JMethodLocation.of(method);
            reference.declaredMethods().add(new JMethodReferenceDelegate(system, s -> s.tryResolve(loc)));
        }

        reference.setUnmodifiable(true);
        return new JResolutionResult<>(reference, cls, children);
    }

    static JResolutionResult<TypeVariable<?>, JVarType> createVarType(JTypeSystem system, TypeVariable<?> var, JTypeParameterLocation location) {
        JVarType varType = system.typeFactory().newVarType();
        varType.metadata().attach(new JReflectionType.Type(var));
        varType.setLocation(location);
        system.storage().cacheFor(JTypeParameterLocation.class).put(location, varType);

        List<JResolutionResult<?, ?>> children = new ArrayList<>();

        for (Type bound : var.getBounds()) {
            JResolutionResult<Type, JType> param = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, bound);
            children.add(param);
            if (param.success()) {
                varType.upperBounds().add(param.value());
            } else {
                system.storage().cacheFor(JTypeParameterLocation.class).remove(location);
                break;
            }
        }

        varType.setUnmodifiable(true);
        return JResolutionResult.inherit(varType, var, children);
    }
}
