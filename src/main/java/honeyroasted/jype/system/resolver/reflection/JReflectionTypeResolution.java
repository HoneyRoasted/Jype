package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

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

        if (methodLocation.name().equals(method.getName()) &&
                methodLocation.containing().equals(JClassNamespace.of(method.getDeclaringClass())) &&
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
        } catch (JResolutionFailedException ex) {
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
            throw new JResolutionFailedException("Failed to resolve class (runtime lookup failed): " + location, ex);
        }
    }

    static Executable methodFromLocation(JMethodLocation location) throws JResolutionFailedException {
        Class<?> targetCls;
        try {
            targetCls = classFromLocation(location.containing().location());
        } catch (JResolutionFailedException ex) {
            throw new JResolutionFailedException("Could not resolve method (containing class not found): " + location, ex);
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
            throw new JResolutionFailedException("Could not resolve method (matching method not found): " + location);
        }

        return targetMethod;
    }

    static Optional<JMethodReference> createMethodReference(JTypeSystem system, Executable executable, JMethodLocation location) {
        JMethodReference mRef = system.typeFactory().newMethodReference();
        mRef.metadata().attach(new JReflectionType.Executable(executable));
        mRef.setLocation(location);
        mRef.setModifiers(executable.getModifiers());
        Optional<? extends JType> outerClass = system.resolve(Type.class, JType.class, executable.getDeclaringClass());
        if (outerClass.isPresent() && outerClass.get() instanceof JClassReference cr) {
            mRef.setOuterClass(cr);
        }

        if (executable instanceof Method method) {
            Optional<? extends JType> returnType = system.resolve(Type.class, JType.class, method.getGenericReturnType());
            if (returnType.isPresent()) {
                mRef.setReturnType(returnType.get());
            } else {
                return Optional.empty();
            }
        } else {
            mRef.setReturnType(system.constants().voidType());
        }

        List<JType> resolvedExceptions = new ArrayList<>();
        for (Type except : executable.getGenericExceptionTypes()) {
            Optional<? extends JType> resolved = system.resolve(Type.class, JType.class, except);
            if (resolved.isPresent()) {
                resolvedExceptions.add(resolved.get());
            } else {
                return Optional.empty();
            }
        }

        List<JType> resolvedParams = new ArrayList<>();
        for (Type param : executable.getGenericParameterTypes()) {
            Optional<? extends JType> resolved = system.resolve(Type.class, JType.class, param);
            if (resolved.isPresent()) {
                resolvedParams.add(resolved.get());
            } else {
                return Optional.empty();
            }
        }

        List<JVarType> resolvedTypeParams = new ArrayList<>();
        for (TypeVariable<?> tvar : executable.getTypeParameters()) {
            Optional<? extends JType> resolved = system.resolve(Type.class, JType.class, tvar);
            if (resolved.isPresent() && resolved.get() instanceof JVarType res) {
                resolvedTypeParams.add(res);
            } else {
                return Optional.empty();
            }
        }

        mRef.setExceptionTypes(resolvedExceptions);
        mRef.setParameters(resolvedParams);
        mRef.setTypeParameters(resolvedTypeParams);
        mRef.setUnmodifiable(true);
        system.storage().cacheFor(JMethodLocation.class).put(mRef.location(), mRef);
        system.storage().cacheFor(Executable.class).put(executable, mRef);

        return Optional.of(mRef);
    }

    static Optional<JType> createClassReference(JTypeSystem system, Class<?> cls, JClassLocation location) {
        if (cls.isArray()) {
            return createClassReference(system, cls.componentType(), location.containing()).map(c -> {
                JArrayType type = system.typeFactory().newArrayType();
                type.setComponent(c);
                type.setUnmodifiable(true);
                type.metadata().attach(new JReflectionType.Type(cls));
                return type;
            });
        }

        JClassReference reference = system.typeFactory().newClassReference();

        reference.metadata().attach(new JReflectionType.Type(cls));
        reference.setNamespace(JClassNamespace.of(cls));
        reference.setModifiers(cls.getModifiers());
        system.storage().cacheFor(Type.class).put(cls, reference);
        system.storage().cacheFor(JClassLocation.class).put(reference.namespace().location(), reference);

        if (cls.getSuperclass() != null) {
            Optional<? extends JType> superCls = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, cls.getGenericSuperclass());

            if (superCls.isEmpty() || !(superCls.get() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setSuperClass((JClassType) superCls.get());
            }
        } else if (!cls.equals(Object.class)) {
            Optional<? extends JType> objClass = system.resolve(Object.class);
            if (objClass.isPresent() && objClass.get() instanceof JClassType ct) {
                reference.setSuperClass(ct);
            } else {
                return Optional.empty();
            }
        }

        if (cls.getEnclosingClass() != null) {
            Optional<? extends JType> enclosing = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, cls.getEnclosingClass());

            if (enclosing.isEmpty() || !(enclosing.get() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setOuterClass(((JClassType) enclosing.get()).classReference());
            }
        }

        if (cls.getEnclosingMethod() != null || cls.getEnclosingConstructor() != null) {
            Executable enclsoingExecutable = cls.getEnclosingMethod() != null ? cls.getEnclosingMethod() : cls.getEnclosingConstructor();

            Optional<? extends JType> enclosing = system.resolvers().resolverFor(Executable.class, JType.class)
                    .resolve(system, enclsoingExecutable);

            if (enclosing.isEmpty() || !(enclosing.get() instanceof JMethodType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setOuterMethod(((JMethodType) enclosing.get()).methodReference());
            }
        }

        for (Type inter : cls.getGenericInterfaces()) {
            Optional<? extends JType> interRef = system.resolvers().resolverFor(Type.class, JType.class)
                    .resolve(system, inter);

            if (interRef.isEmpty() || !(interRef.get() instanceof JClassType)) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.interfaces().add((JClassType) interRef.get());
            }
        }

        for (TypeVariable<?> param : cls.getTypeParameters()) {
            JTypeParameterLocation loc = new JTypeParameterLocation(reference.namespace(), param.getName());
            Optional<? extends JVarType> paramRef = createVarType(system, param, loc);

            if (paramRef.isEmpty()) {
                system.storage().cacheFor(Type.class).remove(cls);
                system.storage().cacheFor(JClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.typeParameters().add(paramRef.get());
            }
        }

        reference.setUnmodifiable(true);
        return Optional.of(reference);
    }

    static Optional<JVarType> createVarType(JTypeSystem system, TypeVariable<?> var, JTypeParameterLocation location) {
        JVarType varType = system.typeFactory().newVarType();
        varType.metadata().attach(new JReflectionType.Type(var));
        varType.setLocation(location);
        system.storage().cacheFor(JTypeParameterLocation.class).put(location, varType);

        for (Type bound : var.getBounds()) {
            Optional<? extends JType> param = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, bound);
            if (param.isPresent()) {
                varType.upperBounds().add(param.get());
            } else {
                system.storage().cacheFor(JTypeParameterLocation.class).remove(location);
                return Optional.empty();
            }
        }

        varType.setUnmodifiable(true);
        return Optional.of(varType);
    }

    static TypeVariable<?> typeParameterFromLocation(JTypeParameterLocation location) throws JResolutionFailedException {
        if (location.isVirtual()) {
            throw new JResolutionFailedException("Could not resolve type parameter from virtual location: " + location);
        }

        TypeVariable[] parameters = null;

        try {
            if (location.containing() instanceof JClassNamespace containing) {
                parameters = JReflectionTypeResolution.classFromLocation(containing.location()).getTypeParameters();
            } else if (location.containing() instanceof JMethodLocation containing) {
                parameters = JReflectionTypeResolution.methodFromLocation(containing).getTypeParameters();
            }
        } catch (JResolutionFailedException ex) {
            throw new JResolutionFailedException("Could not resolve type parameter (no generic declaration found): " + location, ex);
        }

        if (parameters == null) {
            throw new JResolutionFailedException("Could not resolve type parameter (no type parameters found): " + location);
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
            throw new JResolutionFailedException("Could not resolve type parameter (no matching name found): " + location);
        }
    }
}
