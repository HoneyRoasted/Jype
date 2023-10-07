package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.visitors.ErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.SimpleTypeVisitor;
import honeyroasted.jype.type.*;
import honeyroasted.jype.type.impl.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface TypeVisitors {
    Mapping<Boolean> ERASURE = new ErasureTypeVisitor();
    Mapping<Object> IDENTITY = new Mapping<>() {};

    static <T> Mapping<T> identity() {
        return (Mapping<T>) IDENTITY;
    }

    interface Mapping<P> extends SimpleTypeVisitor<Type, P>, Function<Type, Type> {

        default Type visit(Type type) {
            return type.accept(this, null);
        }

        @Override
        default Type apply(Type type) {
            return visit(type);
        }

        default List<Type> visit(List<? extends Type> types, P context) {
            return types.stream().map(t -> visit(t, context)).toList();
        }

        default Set<Type> visit(Set<Type> types, P context) {
            return types.stream().map(t -> visit(t, context)).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        default Type visitType(Type type, P context) {
            return type;
        }
    }

    interface StructuralMapping<P> extends Mapping<P> {
        @Override
        default Type visitClassType(ClassType type, P context) {
            if (type instanceof ParameterizedClassType pType) {
                List<Type> args = pType.typeArguments();
                List<Type> newArgs = visit(args, context);

                if (!args.equals(newArgs)) {
                    ParameterizedClassType newType = new ParameterizedClassTypeImpl(pType.typeSystem());
                    newType.setClassReference(pType.classReference());
                    newType.setTypeArguments(newArgs);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            }
            return type;
        }

        @Override
        default Type visitWildcardType(WildType type, P context) {
            if (type instanceof WildType.Upper uType) {
                Set<Type> newUpper = visit(uType.upperBounds(), context);
                if (!newUpper.equals(uType.upperBounds())) {
                    WildType.Upper newType = new WildTypeUpperImpl(type.typeSystem());
                    newType.setUpperBounds(newUpper);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            } else if (type instanceof WildType.Lower lType) {
                Set<Type> newLower = visit(lType.lowerBounds(), context);
                if (!newLower.equals(lType.lowerBounds())) {
                    WildType.Lower newType = new WildTypeLowerImpl(type.typeSystem());
                    newType.setLowerBounds(newLower);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            }
            return type;
        }

        @Override
        default Type visitArrayType(ArrayType type, P context) {
            Type newComponent = visit(type.component(), context);
            if (!newComponent.equals(type.component())) {
                ArrayType newType = new ArrayTypeImpl(type.typeSystem());
                newType.setComponent(newComponent);
                newType.setUnmodifiable(true);
                return newType;
            }
            return type;
        }

        @Override
        default Type visitMethodType(MethodType type, P context) {
            if (type instanceof MethodReference rType) {
                List<Type> newParams = visit(rType.parameters(), context);
                Type newRet = visit(type.returnType(), context);
                if (!newParams.equals(rType.parameters()) && !newRet.equals(type.returnType())) {
                    MethodReference newType = new MethodReferenceImpl(type.typeSystem());
                    newType.setLocation(type.location());
                    newType.setTypeParameters(type.typeParameters());
                    newType.setParameters(newParams);
                    newType.setReturnType(newRet);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            } else if (type instanceof ParameterizedMethodTypeImpl pType) {
                Type newRef = visit(pType.methodReference(), context);
                List<Type> newTypeArgs = visit(pType.typeArguments(), context);
                if (!newRef.equals(pType.methodReference()) && !newTypeArgs.equals(pType.typeArguments())) {
                    ParameterizedMethodType newType = new ParameterizedMethodTypeImpl(type.typeSystem());
                    newType.setMethodReference(newRef instanceof MethodReference ref ? ref : pType.methodReference());
                    newType.setTypeArguments(newTypeArgs);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            }
            return type;
        }
    }

    interface DeepStructuralTypeMapping extends Mapping<TypeCache<Type, Type>> {
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
                newRef.setInterface(ref.isInterface());

                Type newSuper = visit(ref.superClass(), context);
                newRef.setSuperClass(newSuper instanceof ClassType ct ? ct : ref.superClass());

                newRef.setInterfaces((List<ClassType>) (List) this.visit(ref.interfaces(), context).stream().filter(t -> t instanceof ClassType).toList());
                newRef.setTypeParameters((List<VarType>) (List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof VarType).toList());
                newRef.setUnmodifiable(true);

                return newRef;
            } else if (type instanceof ParameterizedClassType pt) {
                ParameterizedClassType newType = new ParameterizedClassTypeImpl(type.typeSystem());
                context.put(type, newType);

                Type newRef = this.visitClassType(pt.classReference(), context);
                newType.setClassReference(newRef instanceof ClassReference cr ? cr : pt.classReference());
                newType.setTypeArguments(this.visit(pt.typeArguments(), context));
                newType.setUnmodifiable(true);

                return newType;
            }
            return type;
        }

        @Override
        default Type visitPrimitiveType(PrimitiveType type, TypeCache<Type, Type> context) {
            Optional<Type> cached = context.get(type);
            if (cached.isPresent()) return cached.get();
            if (this.overridesPrimitiveType(type)) return this.primitiveTypeOverride(type, context);

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
                newRef.setReturnType(this.visit(ref.returnType(), context));
                newRef.setParameters(this.visit(ref.parameters(), context));
                newRef.setTypeParameters((List<VarType>) (List) this.visit(ref.typeParameters(), context).stream().filter(t -> t instanceof VarType).toList());
                newRef.setUnmodifiable(true);
                return newRef;
            } else if (type instanceof ParameterizedMethodType pt) {
                ParameterizedMethodType newType = new ParameterizedMethodTypeImpl(type.typeSystem());
                context.put(type, newType);

                Type newRef = this.visit(newType.methodReference(), context);
                newType.setMethodReference(newRef instanceof MethodReference mr ? mr : pt.methodReference());
                newType.setTypeArguments(this.visit(pt.typeArguments(), context));
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
            newType.setLowerBounds(this.visit(type.lowerBounds(), context));
            newType.setUnmodifiable(true);
            return newType;
        }

        @Override
        default Type visitNoneType(NoneType type, TypeCache<Type, Type> context) {
            Optional<Type> cached = context.get(type);
            if (cached.isPresent()) return cached.get();
            if (this.overridesNoneType(type)) return this.noneTypeOverride(type, context);

            return type;
        }
    }
}
