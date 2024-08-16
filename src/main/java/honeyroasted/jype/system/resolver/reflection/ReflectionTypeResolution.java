package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.BundledTypeResolvers;
import honeyroasted.jype.system.resolver.ResolutionFailedException;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ReflectionTypeResolution {

    BundledTypeResolvers REFLECTION_TYPE_RESOLVERS = new BundledTypeResolvers(
            new ReflectionTypeTokenResolver(),
            new ReflectionJavaTypeResolver(),
            new ReflectionJavaMethodResolver(),
            new ReflectionClassReferenceResolver(),
            new ReflectionMethodReferenceResolver(),
            new ReflectionTypeParameterResolver()
    );

    static boolean locationMatchesMethod(MethodLocation methodLocation, Executable method) {
        ClassLocation retType = method instanceof Method mth ? ClassLocation.of(mth.getReturnType()) : ClassLocation.of(void.class);

        if (methodLocation.name().equals(method.getName()) &&
                methodLocation.containing().equals(ClassNamespace.of(method.getDeclaringClass())) &&
                methodLocation.returnType().equals(retType) &&
                methodLocation.parameters().size() == method.getParameterCount()) {
            Class[] parameters = method.getParameterTypes();

            for (int i = 0; i < parameters.length; i++) {
                if (!methodLocation.parameters().get(i).equals(ClassLocation.of(parameters[i]))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    static <T> Optional<T> getReflectionType(Type type) {
        try {
            if (type.metadata().has(ReflectionType.class)) {
                ReflectionType<?> meta = type.metadata().get(ReflectionType.class).get();
                return (Optional<T>) Optional.of(meta.type());
            } else if (type instanceof ClassReference cr) {
                return (Optional<T>) Optional.of(classFromLocation(cr.namespace().location()));
            } else if (type instanceof MethodReference mr) {
                return (Optional<T>) Optional.of(methodFromLocation(mr.location()));
            } else if (type instanceof VarType vr) {
                return (Optional<T>) Optional.of(typeParameterFromLocation(vr.location()));
            }
            return Optional.empty();
        } catch (ResolutionFailedException ex) {
            return Optional.empty();
        }
    }

    static Optional<Class<?>> getReflectionType(ClassReference reference) {
        return getReflectionType((Type) reference);
    }

    static Optional<Executable> getReflectionType(MethodReference reference) {
        return getReflectionType((Type) reference);
    }

    static Optional<TypeVariable<?>> getReflectionType(VarType type) {
        return getReflectionType((Type) type);
    }

    static Class<?> classFromLocation(ClassLocation location) throws ResolutionFailedException {
        try {
            int depth = 0;
            ClassLocation current = location;
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
            throw new ResolutionFailedException("Failed to resolve class (runtime lookup failed): " + location, ex);
        }
    }

    static Executable methodFromLocation(MethodLocation location) throws ResolutionFailedException {
        Class<?> targetCls;
        try {
            targetCls = classFromLocation(location.containing().location());
        } catch (ResolutionFailedException ex) {
            throw new ResolutionFailedException("Could not resolve method (containing class not found): " + location, ex);
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
            throw new ResolutionFailedException("Could not resolve method (matching method not found): " + location);
        }

        return targetMethod;
    }

    static Optional<MethodReference> createMethodReference(TypeSystem system, Executable executable, MethodLocation location) {
        MethodReference mRef = system.typeFactory().newMethodReference();
        mRef.metadata().attach(ReflectionType.class, new ReflectionType.Executable(executable));
        mRef.setLocation(location);
        mRef.setModifiers(executable.getModifiers());
        Optional<? extends honeyroasted.jype.type.Type> outerClass = system.resolve(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class, executable.getDeclaringClass());
        if (outerClass.isPresent() && outerClass.get() instanceof ClassReference cr) {
            mRef.setOuterClass(cr);
        }

        if (executable instanceof Method method) {
            Optional<? extends honeyroasted.jype.type.Type> returnType = system.resolve(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class, method.getGenericReturnType());
            if (returnType.isPresent()) {
                mRef.setReturnType(returnType.get());
            } else {
                return Optional.empty();
            }
        } else {
            mRef.setReturnType(system.constants().voidType());
        }

        List<honeyroasted.jype.type.Type> resolvedExceptions = new ArrayList<>();
        for (java.lang.reflect.Type except : executable.getGenericExceptionTypes()) {
            Optional<? extends honeyroasted.jype.type.Type> resolved = system.resolve(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class, except);
            if (resolved.isPresent()) {
                resolvedExceptions.add(resolved.get());
            } else {
                return Optional.empty();
            }
        }

        List<honeyroasted.jype.type.Type> resolvedParams = new ArrayList<>();
        for (java.lang.reflect.Type param : executable.getGenericParameterTypes()) {
            Optional<? extends honeyroasted.jype.type.Type> resolved = system.resolve(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class, param);
            if (resolved.isPresent()) {
                resolvedParams.add(resolved.get());
            } else {
                return Optional.empty();
            }
        }

        List<VarType> resolvedTypeParams = new ArrayList<>();
        for (TypeVariable<?> tvar : executable.getTypeParameters()) {
            Optional<? extends honeyroasted.jype.type.Type> resolved = system.resolve(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class, tvar);
            if (resolved.isPresent() && resolved.get() instanceof VarType res) {
                resolvedTypeParams.add(res);
            } else {
                return Optional.empty();
            }
        }

        mRef.setExceptionTypes(resolvedExceptions);
        mRef.setParameters(resolvedParams);
        mRef.setTypeParameters(resolvedTypeParams);
        mRef.setUnmodifiable(true);
        system.storage().cacheFor(MethodLocation.class).put(mRef.location(), mRef);
        system.storage().cacheFor(Executable.class).put(executable, mRef);

        return Optional.of(mRef);
    }

    static Optional<Type> createClassReference(TypeSystem system, Class<?> cls, ClassLocation location) {
        if (cls.isArray()) {
            return createClassReference(system, cls.componentType(), location.containing()).map(c -> {
                ArrayType type = system.typeFactory().newArrayType();
                type.setComponent(c);
                type.setUnmodifiable(true);
                type.metadata().attach(ReflectionType.class, new ReflectionType.Type(cls));
                return type;
            });
        }

        ClassReference reference = system.typeFactory().newClassReference();

        reference.metadata().attach(ReflectionType.class, new ReflectionType.Type(cls));
        reference.setNamespace(ClassNamespace.of(cls));
        reference.setModifiers(cls.getModifiers());
        system.storage().cacheFor(java.lang.reflect.Type.class).put(location, reference);
        system.storage().cacheFor(ClassLocation.class).put(reference.namespace().location(), reference);

        if (cls.getSuperclass() != null) {
            Optional<? extends honeyroasted.jype.type.Type> superCls = system.resolvers().resolverFor(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class)
                    .resolve(system, cls.getGenericSuperclass());

            if (superCls.isEmpty() || !(superCls.get() instanceof ClassType)) {
                system.storage().cacheFor(java.lang.reflect.Type.class).remove(location);
                system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setSuperClass((ClassType) superCls.get());
            }
        } else if (!cls.equals(Object.class)) {
            Optional<? extends Type> objClass = system.resolve(Object.class);
            if (objClass.isPresent() && objClass.get() instanceof ClassType ct) {
                reference.setSuperClass(ct);
            } else {
                return Optional.empty();
            }
        }

        if (cls.getEnclosingClass() != null) {
            Optional<? extends honeyroasted.jype.type.Type> enclosing = system.resolvers().resolverFor(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class)
                    .resolve(system, cls.getEnclosingClass());

            if (enclosing.isEmpty() || !(enclosing.get() instanceof ClassType)) {
                system.storage().cacheFor(java.lang.reflect.Type.class).remove(location);
                system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setOuterClass(((ClassType) enclosing.get()).classReference());
            }
        }

        if (cls.getEnclosingMethod() != null || cls.getEnclosingConstructor() != null) {
            Executable enclsoingExecutable = cls.getEnclosingMethod() != null ? cls.getEnclosingMethod() : cls.getEnclosingConstructor();

            Optional<? extends honeyroasted.jype.type.Type> enclosing = system.resolvers().resolverFor(Executable.class, honeyroasted.jype.type.Type.class)
                    .resolve(system, enclsoingExecutable);

            if (enclosing.isEmpty() || !(enclosing.get() instanceof MethodType)) {
                system.storage().cacheFor(java.lang.reflect.Type.class).remove(location);
                system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.setOuterMethod(((MethodType) enclosing.get()).methodReference());
            }
        }

        for (java.lang.reflect.Type inter : cls.getGenericInterfaces()) {
            Optional<? extends honeyroasted.jype.type.Type> interRef = system.resolvers().resolverFor(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class)
                    .resolve(system, inter);

            if (interRef.isEmpty() || !(interRef.get() instanceof ClassType)) {
                system.storage().cacheFor(java.lang.reflect.Type.class).remove(location);
                system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.interfaces().add((ClassType) interRef.get());
            }
        }

        for (TypeVariable<?> param : cls.getTypeParameters()) {
            TypeParameterLocation loc = new TypeParameterLocation(reference.namespace(), param.getName());
            Optional<? extends VarType> paramRef = system.resolvers().resolverFor(TypeParameterLocation.class, VarType.class)
                    .resolve(system, loc);

            if (paramRef.isEmpty()) {
                system.storage().cacheFor(java.lang.reflect.Type.class).remove(location);
                system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                return Optional.empty();
            } else {
                reference.typeParameters().add(paramRef.get());
            }
        }

        reference.setUnmodifiable(true);
        return Optional.of(reference);
    }

    static Optional<VarType> createVarType(TypeSystem system, TypeVariable<?> var, TypeParameterLocation location) {
        VarType varType = system.typeFactory().newVarType();
        varType.metadata().attach(ReflectionType.class, new ReflectionType.Type(var));
        varType.setLocation(location);
        system.storage().cacheFor(TypeParameterLocation.class).put(location, varType);

        for (java.lang.reflect.Type bound : var.getBounds()) {
            Optional<? extends honeyroasted.jype.type.Type> param = system.resolvers().resolverFor(java.lang.reflect.Type.class, honeyroasted.jype.type.Type.class).resolve(system, bound);
            if (param.isPresent()) {
                varType.upperBounds().add(param.get());
            } else {
                system.storage().cacheFor(TypeParameterLocation.class).remove(location);
                return Optional.empty();
            }
        }

        varType.setUnmodifiable(true);
        return Optional.of(varType);
    }

    static TypeVariable<?> typeParameterFromLocation(TypeParameterLocation location) throws ResolutionFailedException {
        if (location.isVirtual()) {
            throw new ResolutionFailedException("Could not resolve type parameter from virtual location: " + location);
        }

        TypeVariable[] parameters = null;

        try {
            if (location.containing() instanceof ClassNamespace containing) {
                parameters = ReflectionTypeResolution.classFromLocation(containing.location()).getTypeParameters();
            } else if (location.containing() instanceof MethodLocation containing) {
                parameters = ReflectionTypeResolution.methodFromLocation(containing).getTypeParameters();
            }
        } catch (ResolutionFailedException ex) {
            throw new ResolutionFailedException("Could not resolve type parameter (no generic declaration found): " + location, ex);
        }

        if (parameters == null) {
            throw new ResolutionFailedException("Could not resolve type parameter (no type parameters found): " + location);
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
            throw new ResolutionFailedException("Could not resolve type parameter (no matching name found): " + location);
        }
    }

}
