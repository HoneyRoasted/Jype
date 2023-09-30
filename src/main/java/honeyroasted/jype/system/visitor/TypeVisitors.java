package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.visitors.ErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.SimpleTypeVisitor;
import honeyroasted.jype.type.*;

import java.util.List;
import java.util.function.Function;

public interface TypeVisitors {
    Mapping<Boolean> ERASURE = new ErasureTypeVisitor();
    Mapping<Object> IDENTITY = new Mapping<>();

    static <T> Mapping<T> identity() {
        return (Mapping<T>) IDENTITY;
    }

    class Mapping<P> extends SimpleTypeVisitor<Type, P> implements Function<Type, Type> {

        public Type visit(Type type) {
            return type.accept(this, null);
        }

        @Override
        public Type apply(Type type) {
            return visit(type);
        }

        public List<Type> visit(List<Type> types, P context) {
            return types.stream().map(t -> visit(t, context)).toList();
        }

        @Override
        public Type visitType(Type type, P context) {
            return type;
        }

        @Override
        public <T extends Type> Type visitMetadataType(TypeWithMetadata<T> type, P context) {
            Type result = super.visitMetadataType(type, context);
            return result instanceof TypeWithMetadata<?> ? result : new TypeWithMetadata<>(result, type.metadata().copy());
        }
    }

    class StructuralMapping<P> extends Mapping<P> {
        @Override
        public Type visitClassType(ClassType type, P context) {
            if (type instanceof ParameterizedClassType pType) {
                List<Type> args = pType.typeArguments();
                List<Type> newArgs = visit(args, context);

                if (!args.equals(newArgs)) {
                    ParameterizedClassType newType = new ParameterizedClassType(pType.typeSystem());
                    newType.setClassReference(pType.classReference());
                    newType.setTypeArguments(newArgs);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            }
            return type;
        }

        @Override
        public Type visitWildcardType(WildType type, P context) {
            if (type instanceof WildType.Upper uType) {
                List<Type> newUpper = visit(uType.upperBounds(), context);
                if (!newUpper.equals(uType.upperBounds())) {
                    return new WildType.Upper(type.typeSystem(), newUpper);
                }
            } else if (type instanceof WildType.Lower lType) {
                List<Type> newLower = visit(lType.lowerBounds(), context);
                if (!newLower.equals(lType.lowerBounds())) {
                    return new WildType.Lower(type.typeSystem(), newLower);
                }
            }
            return type;
        }

        @Override
        public Type visitArrayType(ArrayType type, P context) {
            Type newComponent = visit(type.component(), context);
            if (!newComponent.equals(type.component())) {
                return new ArrayType(type.typeSystem(), newComponent);
            }
            return type;
        }

        @Override
        public Type visitMethodType(MethodType type, P context) {
            if (type instanceof MethodReference rType) {
                List<Type> newParams = visit(rType.parameters(), context);
                Type newRet = visit(type.returnType(), context);
                if (!newParams.equals(rType.parameters()) && !newRet.equals(type.returnType())) {
                    MethodReference newType = new MethodReference(type.typeSystem());
                    newType.setLocation(type.location());
                    newType.setTypeParameters(type.typeParameters());
                    newType.setParameters(newParams);
                    newType.setReturnType(newRet);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            } else if (type instanceof ParameterizedMethodType pType) {
                Type newRef = visit(pType.methodReference(), context);
                List<Type> newTypeArgs = visit(pType.typeArguments(), context);
                if (!newRef.equals(pType.methodReference()) && !newTypeArgs.equals(pType.typeArguments())) {
                    ParameterizedMethodType newType = new ParameterizedMethodType(type.typeSystem());
                    newType.setMethodReference(newRef instanceof MethodReference ref ? ref : pType.methodReference());
                    newType.setTypeArguments(newTypeArgs);
                    newType.setUnmodifiable(true);
                    return newType;
                }
            }
            return type;
        }
    }
}
