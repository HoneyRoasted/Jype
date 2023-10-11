package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.ArrayTypeImpl;
import honeyroasted.jype.type.impl.ClassReferenceImpl;
import honeyroasted.jype.type.impl.MethodReferenceImpl;
import honeyroasted.jype.type.impl.ParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedMethodTypeImpl;
import honeyroasted.jype.type.impl.VarTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;

import java.util.List;
import java.util.Optional;

public interface DeepStructuralMappingVisitor extends MappingVisitor<TypeCache<Type, Type>> {
    default boolean overridesClassType(ClassType type) {
        return false;
    }

    default Type classTypeOverride(ClassType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesPrimitiveType(PrimitiveType type) {
        return false;
    }

    default Type primitiveTypeOverride(PrimitiveType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesWildcardType(WildType type) {
        return false;
    }

    default Type wildcardTypeOverride(WildType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesArrayType(ArrayType type) {
        return false;
    }

    default Type arrayTypeOverride(ArrayType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesMethodType(MethodType type) {
        return false;
    }

    default Type methodTypeOverride(MethodType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesVarType(VarType type) {
        return false;
    }

    default Type varTypeOverride(VarType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesMetaVarType(MetaVarType type) {
        return false;
    }

    default Type metaVarTypeOverride(MetaVarType type, TypeCache<Type, Type> cache) {
        return type;
    }

    default boolean overridesNoneType(NoneType type) {
        return false;
    }

    default Type noneTypeOverride(NoneType type, TypeCache<Type, Type> cache) {
        return type;
    }

    @Override
    default Type visit(Type type) {
        return visit(type, new InMemoryTypeCache<>());
    }

    @Override
    default Type visitClassType(ClassType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesClassType(type)) return this.classTypeOverride(type, context);

        if (type instanceof ClassReference ref) {
            ClassReference newRef = new ClassReferenceImpl(ref.typeSystem());
            context.put(type, newRef);
            newRef.setNamespace(ref.namespace());
            newRef.setModifiers(ref.modifiers());

            Type newSuper = visit(ref.superClass(), context);
            newRef.setSuperClass(newSuper instanceof ClassType ct ? ct : ref.superClass());

            Type newOuter = visit(ref.outerClass(), context);
            newRef.setOuterClass(newOuter instanceof ClassReference cr ? cr : ref.outerClass());

            newRef.setInterfaces((List<ClassType>) (List) this.visit(ref.interfaces(), context).stream().filter(t -> t instanceof ClassType).toList());
            newRef.setTypeParameters((List<VarType>) (List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof VarType).toList());
            newRef.setUnmodifiable(true);

            return newRef;
        } else if (type instanceof ParameterizedClassType pt) {
            ParameterizedClassType newType = new ParameterizedClassTypeImpl(type.typeSystem());
            context.put(type, newType);

            Type newRef = visit(pt.classReference(), context);
            newType.setClassReference(newRef instanceof ClassReference cr ? cr : pt.classReference());

            Type newOuter = visit(pt.outerType(), context);
            newType.setOuterType(newOuter instanceof ClassType ct ? ct : pt.outerType());

            newType.setTypeArguments((List) this.visit(pt.typeArguments(), context));
            newType.setUnmodifiable(true);

            return newType;
        }
        return type;
    }

    @Override
    default Type visitWildcardType(WildType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesWildcardType(type)) return this.wildcardTypeOverride(type, context);

        if (type instanceof WildType.Lower lower) {
            WildType.Lower newLower = new WildTypeLowerImpl(lower.typeSystem());
            context.put(type, newLower);
            newLower.setLowerBounds(this.visit(lower.lowerBounds(), context));
            newLower.setUnmodifiable(true);
            return newLower;
        } else if (type instanceof WildType.Upper upper) {
            WildType.Upper newUpper = new WildTypeUpperImpl(upper.typeSystem());
            context.put(type, newUpper);
            newUpper.setUpperBounds(this.visit(upper.upperBounds(), context));
            newUpper.setUnmodifiable(true);
            return newUpper;
        }
        return type;
    }

    @Override
    default Type visitArrayType(ArrayType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesArrayType(type)) return this.arrayTypeOverride(type, context);

        ArrayType newArray = new ArrayTypeImpl(type.typeSystem());
        context.put(type, newArray);
        newArray.setComponent(this.visit(newArray.component(), context));
        newArray.setUnmodifiable(true);
        return newArray;
    }

    @Override
    default Type visitMethodType(MethodType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesMethodType(type)) return this.methodTypeOverride(type, context);

        if (type instanceof MethodReference ref) {
            MethodReference newRef = new MethodReferenceImpl(ref.typeSystem());
            context.put(type, newRef);
            newRef.setLocation(ref.location());

            Type newOuter = this.visit(ref.outerClass(), context);
            newRef.setOuterClass(newOuter instanceof ClassReference cr ? cr : newRef.outerClass());

            newRef.setModifiers(ref.modifiers());
            newRef.setExceptionTypes(this.visit(ref.exceptionTypes(), context));
            newRef.setReturnType(this.visit(ref.returnType(), context));
            newRef.setParameters(this.visit(ref.parameters(), context));
            newRef.setTypeParameters((List<VarType>) (List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof VarType).toList());
            newRef.setUnmodifiable(true);
            return newRef;
        } else if (type instanceof ParameterizedMethodType pt) {
            ParameterizedMethodType newType = new ParameterizedMethodTypeImpl(type.typeSystem());
            context.put(type, newType);

            Type newRef = this.visit(pt.methodReference(), context);
            newType.setMethodReference(newRef instanceof MethodReference mr ? mr : pt.methodReference());

            Type newOuter = this.visit(pt.outerType(), context);
            newType.setOuterType(newOuter instanceof ClassType ct ? ct : pt.outerType());

            newType.setTypeArguments((List) this.visit(pt.typeArguments(), context));
            newType.setUnmodifiable(true);
            return newType;
        }
        return type;
    }

    @Override
    default Type visitVarType(VarType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesVarType(type)) return this.varTypeOverride(type, context);

        VarType newType = new VarTypeImpl(type.typeSystem());
        context.put(type, newType);
        newType.setLocation(type.location());
        newType.setUpperBounds(this.visit(type.upperBounds(), context));
        newType.setUnmodifiable(true);
        return newType;
    }

    @Override
    default Type visitMetaVarType(MetaVarType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesMetaVarType(type)) return this.metaVarTypeOverride(type, context);

        return type;
    }

    @Override
    default Type visitNoneType(NoneType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesNoneType(type)) return this.noneTypeOverride(type, context);

        return type;
    }

    @Override
    default Type visitPrimitiveType(PrimitiveType type, TypeCache<Type, Type> context) {
        Optional<Type> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesPrimitiveType(type)) return this.primitiveTypeOverride(type, context);

        return type;
    }
}