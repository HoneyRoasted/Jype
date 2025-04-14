package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JInMemoryTypeCache;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.List;
import java.util.Optional;

public interface JDeepStructuralTypeMappingVisitor extends JTypeMappingVisitor<JTypeCache<JType, JType>> {

    default boolean visitStructural() {
        return true;
    }

    default boolean overridesClassType(JClassType type) {
        return false;
    }

    default JType classTypeOverride(JClassType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesPrimitiveType(JPrimitiveType type) {
        return false;
    }

    default JType primitiveTypeOverride(JPrimitiveType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesWildcardType(JWildType type) {
        return false;
    }

    default JType wildcardTypeOverride(JWildType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesArrayType(JArrayType type) {
        return false;
    }

    default JType arrayTypeOverride(JArrayType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesIntersectionType(JIntersectionType type) {
        return false;
    }

    default JType intersectionTypeOverride(JIntersectionType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesMethodType(JMethodType type) {
        return false;
    }

    default JType methodTypeOverride(JMethodType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesVarType(JVarType type) {
        return false;
    }

    default JType varTypeOverride(JVarType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesMetaVarType(JMetaVarType type) {
        return false;
    }

    default JType metaVarTypeOverride(JMetaVarType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    default boolean overridesNoneType(JNoneType type) {
        return false;
    }

    default JType noneTypeOverride(JNoneType type, JTypeCache<JType, JType> cache) {
        return type;
    }

    @Override
    default JType visit(JType type) {
        return visit(type, new JInMemoryTypeCache<>(JType.class, JType.class));
    }

    @Override
    default JType visitClassType(JClassType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesClassType(type)) return this.classTypeOverride(type, context);

        if (type instanceof JClassReference ref) {
            JClassReference newRef = ref.typeSystem().typeFactory().newClassReference();
            context.put(type, newRef);
            newRef.setNamespace(ref.namespace());
            newRef.setModifiers(ref.modifiers());

            JType newSuper = visit(ref.superClass(), context);
            newRef.setSuperClass(newSuper instanceof JClassType ct ? ct : ref.superClass());

            if (this.visitStructural() || type.hasRelevantOuterType()) {
                JType newOuter = visit(ref.outerClass(), context);
                newRef.setOuterClass(newOuter instanceof JClassReference cr ? cr : ref.outerClass());
            } else {
                newRef.setOuterClass(ref.outerClass());
            }

            newRef.setInterfaces((List<JClassType>) (List) this.visit(ref.interfaces(), context).stream().filter(t -> t instanceof JClassType).toList());

            if (this.visitStructural()) {
                newRef.setTypeParameters((List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof JVarType).toList());
                newRef.setDeclaredMethods((List) this.visit(ref.declaredMethods(), context).stream().filter(t -> t instanceof JMethodReference).toList());
                newRef.setDeclaredFields((List) this.visit(ref.declaredFields(), context).stream().filter(t -> t instanceof JFieldReference).toList());
            } else {
                newRef.setTypeParameters(ref.typeParameters());
                newRef.setDeclaredMethods(ref.declaredMethods());
                newRef.setDeclaredFields(ref.declaredFields());
            }

            newRef.setUnmodifiable(true);

            return newRef;
        } else if (type instanceof JParameterizedClassType pt) {
            JParameterizedClassType newType = type.typeSystem().typeFactory().newParameterizedClassType();
            context.put(type, newType);

            JType newRef = visit(pt.classReference(), context);
            newType.setClassReference(newRef instanceof JClassReference cr ? cr : pt.classReference());

            if (this.visitStructural() || pt.hasRelevantOuterType()) {
                JType newOuter = visit(pt.outerType(), context);
                newType.setOuterType(newOuter instanceof JClassType ct ? ct : pt.outerType());
            } else {
                newType.setOuterType(pt.outerType());
            }

            newType.setTypeArguments((List) this.visit(pt.typeArguments(), context));
            newType.setUnmodifiable(true);

            return newType;
        }
        return type;
    }

    @Override
    default JType visitWildcardType(JWildType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesWildcardType(type)) return this.wildcardTypeOverride(type, context);

        if (type instanceof JWildType.Lower lower) {
            JWildType.Lower newLower = lower.typeSystem().typeFactory().newLowerWildType();
            context.put(type, newLower);
            newLower.setLowerBounds(this.visit(lower.lowerBounds(), context));
            newLower.setUnmodifiable(true);
            return newLower;
        } else if (type instanceof JWildType.Upper upper) {
            JWildType.Upper newUpper = upper.typeSystem().typeFactory().newUpperWildType();
            context.put(type, newUpper);
            newUpper.setUpperBounds(this.visit(upper.upperBounds(), context));
            newUpper.setUnmodifiable(true);
            return newUpper;
        }
        return type;
    }

    @Override
    default JType visitArrayType(JArrayType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesArrayType(type)) return this.arrayTypeOverride(type, context);

        JArrayType newArray = type.typeSystem().typeFactory().newArrayType();
        context.put(type, newArray);
        newArray.setComponent(this.visit(type.component(), context));
        newArray.setUnmodifiable(true);
        return newArray;
    }

    @Override
    default JType visitIntersectionType(JIntersectionType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesIntersectionType(type)) return this.intersectionTypeOverride(type, context);

        return JIntersectionType.of(this.visit(type.children(), context), type.typeSystem());
    }

    @Override
    default JType visitMethodType(JMethodType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesMethodType(type)) return this.methodTypeOverride(type, context);

        if (type instanceof JMethodReference ref) {
            JMethodReference newRef = ref.typeSystem().typeFactory().newMethodReference();
            context.put(type, newRef);
            newRef.setLocation(ref.location());

            if (this.visitStructural() || ref.hasRelevantOuterType()) {
                JType newOuter = this.visit(ref.outerClass(), context);
                newRef.setOuterClass(newOuter instanceof JClassReference cr ? cr : newRef.outerClass());
            } else {
                newRef.setOuterClass(newRef.outerClass());
            }

            newRef.setModifiers(ref.modifiers());
            newRef.setExceptionTypes(this.visit(ref.exceptionTypes(), context));
            newRef.setReturnType(this.visit(ref.returnType(), context));
            newRef.setParameters(this.visit(ref.parameters(), context));
            if (this.visitStructural()) {
                newRef.setTypeParameters((List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof JVarType).toList());
            } else {
                newRef.setTypeParameters(ref.typeParameters());
            }
            newRef.setUnmodifiable(true);
            return newRef;
        } else if (type instanceof JParameterizedMethodType pt) {
            JParameterizedMethodType newType = type.typeSystem().typeFactory().newParameterizedMethodType();
            context.put(type, newType);

            JType newRef = this.visit(pt.methodReference(), context);
            newType.setMethodReference(newRef instanceof JMethodReference mr ? mr : pt.methodReference());

            JType newOuter = this.visit(pt.outerType(), context);
            newType.setOuterType(newOuter instanceof JClassType ct ? ct : pt.outerType());

            newType.setTypeArguments((List) this.visit(pt.typeArguments(), context));
            newType.setUnmodifiable(true);
            return newType;
        }
        return type;
    }

    @Override
    default JType visitVarType(JVarType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesVarType(type)) return this.varTypeOverride(type, context);

        JVarType newType = type.typeSystem().typeFactory().newVarType();
        context.put(type, newType);
        newType.setLocation(type.location());
        newType.setUpperBounds(this.visit(type.upperBounds(), context));
        newType.setUnmodifiable(true);
        return newType;
    }

    @Override
    default JType visitMetaVarType(JMetaVarType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesMetaVarType(type)) return this.metaVarTypeOverride(type, context);

        return type;
    }

    @Override
    default JType visitNoneType(JNoneType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesNoneType(type)) return this.noneTypeOverride(type, context);

        return type;
    }

    @Override
    default JType visitPrimitiveType(JPrimitiveType type, JTypeCache<JType, JType> context) {
        Optional<JType> cached = context.get(type);
        if (cached.isPresent()) return cached.get();
        if (this.overridesPrimitiveType(type)) return this.primitiveTypeOverride(type, context);

        return type;
    }
}