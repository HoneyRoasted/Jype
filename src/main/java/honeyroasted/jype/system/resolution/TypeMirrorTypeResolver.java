package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a {@link TypeResolver} for resolving {@link honeyroasted.jype.Type}s from the java language model objects
 * {@link TypeMirror} and {@link DeclaredType}.
 */
public class TypeMirrorTypeResolver extends AbstractTypeResolver<TypeMirror, DeclaredType> {

    /**
     * Creates a new {@link TypeMirrorTypeResolver}.
     *
     * @param typeSystem The {@link TypeSystem} this {@link TypeMirrorTypeResolver} is resolving types for
     * @param cache      The {@link TypeCache} this {@link TypeMirrorTypeResolver} is using to cache resolved types
     */
    public TypeMirrorTypeResolver(TypeSystem typeSystem, TypeCache<? super TypeMirror> cache) {
        super(typeSystem, cache, TypeMirror.class, DeclaredType.class);
    }

    @Override
    public TypeConcrete resolve(TypeMirror type) {
        return of(type);
    }

    @Override
    public TypeDeclaration resolveDeclaration(DeclaredType type) {
        return declaration(type);
    }

    private TypeConcrete of(TypeMirror type) {
        if (type.getKind().isPrimitive() && type instanceof PrimitiveType primitiveType) {
            return switch (primitiveType.getKind()) {
                case BOOLEAN -> this.typeSystem().BOOLEAN;
                case BYTE -> this.typeSystem().BYTE;
                case SHORT -> this.typeSystem().SHORT;
                case INT -> this.typeSystem().INT;
                case LONG -> this.typeSystem().LONG;
                case CHAR -> this.typeSystem().CHAR;
                case FLOAT -> this.typeSystem().FLOAT;
                case DOUBLE -> this.typeSystem().DOUBLE;
                default -> throw new IllegalArgumentException("Unknown primitive type: " + primitiveType.getKind());
            };
        } else if (type.getKind() == TypeKind.VOID) {
            return this.typeSystem().VOID;
        } else if (type.getKind() == TypeKind.NONE) {
            return this.typeSystem().NONE;
        } else if (type.getKind() == TypeKind.NULL) {
            return this.typeSystem().NULL;
        } else if (type.getKind() == TypeKind.ARRAY && type instanceof ArrayType arrayType) {
            return new TypeArray(this.typeSystem(), of(arrayType.getComponentType()));
        } else if (type.getKind() == TypeKind.WILDCARD && type instanceof javax.lang.model.type.WildcardType wildcardType) {
            if (wildcardType.getSuperBound() != null) {
                return new TypeIn(this.typeSystem(), of(wildcardType.getSuperBound()));
            } else if (wildcardType.getExtendsBound() != null) {
                return new TypeOut(this.typeSystem(), of(wildcardType.getExtendsBound()));
            } else {
                return new TypeOut(this.typeSystem(), this.typeSystem().OBJECT);
            }
        } else if (type.getKind() == TypeKind.TYPEVAR && type instanceof javax.lang.model.type.TypeVariable typeVariable) {
            TypeParameterElement var = (TypeParameterElement) typeVariable.asElement();
            if (this.cache().has(typeVariable, TypeParameter.class)) {
                return this.cache().get(typeVariable, TypeParameter.class);
            } else {
                TypeParameter parameter = new TypeParameter(this.typeSystem(), var.getSimpleName().toString());
                this.cache().cache(var.asType(), parameter);
                parameter.setBound(and(var.getBounds()));
                parameter.lock();
                return parameter;
            }
        } else if (type.getKind() == TypeKind.UNION && type instanceof UnionType unionType) {
            return or(unionType.getAlternatives());
        } else if (type.getKind() == TypeKind.INTERSECTION && type instanceof IntersectionType intersectionType) {
            return and(intersectionType.getBounds());
        } else if (type.getKind() == TypeKind.DECLARED && type instanceof DeclaredType declared && declared.asElement() instanceof TypeElement) {
            if (declared.getTypeArguments().isEmpty()) {
                if (this.cache().has(declared, TypeParameterized.class)) {
                    return this.cache().get(declared, TypeParameterized.class);
                } else {
                    TypeParameterized cls = new TypeParameterized(this.typeSystem(), declaration(declared));
                    this.cache().cache(declared, cls);
                    cls.lock();
                    return cls;
                }
            } else {
                if (this.cache().has(declared, TypeParameterized.class)) {
                    return this.cache().get(declared, TypeParameterized.class);
                } else {
                    TypeParameterized cls = new TypeParameterized(this.typeSystem(), declaration(declared));
                    this.cache().cache(declared, cls);
                    for (TypeMirror arg : declared.getTypeArguments()) {
                        cls.arguments().add(of(arg));
                    }
                    cls.lock();
                    return cls;
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + type.getKind() + ", " + type);
        }
    }

    private TypeDeclaration declaration(DeclaredType declared) {
        Element element = declared.asElement();
        if ((element.getKind().isClass() || element.getKind().isInterface()) && element instanceof TypeElement typeElement) {

            Namespace namespace = namespace(declared);
            if (this.cache().has(declared, TypeDeclaration.class)) {
                return this.cache().get(declared, TypeDeclaration.class);
            } else {
                TypeDeclaration type = new TypeDeclaration(this.typeSystem(), namespace, element.getKind().isInterface());
                this.cache().cache(declared, type);

                for (TypeParameterElement var : ((TypeElement) element).getTypeParameters()) {
                    if (this.cache().has(var.asType(), TypeParameter.class)) {
                        type.parameters().add(this.cache().get(var.asType(), TypeParameter.class));
                    } else {
                        TypeParameter parameter = new TypeParameter(this.typeSystem(), var.getSimpleName().toString());
                        this.cache().cache(var.asType(), parameter);
                        parameter.setBound(and(var.getBounds()));
                        parameter.lock();
                        type.parameters().add(parameter);
                    }
                }

                TypeMirror superclass = typeElement.getSuperclass();
                if (superclass.getKind() != TypeKind.NONE) {
                    type.parents().add((TypeParameterized) of(superclass));
                } else if (typeElement.getKind() == ElementKind.INTERFACE) {
                    type.parents().add(this.typeSystem().OBJECT);
                }

                for (TypeMirror inter : typeElement.getInterfaces()) {
                    type.parents().add((TypeParameterized) of(inter));
                }

                type.lock();
                return type;
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + declared.getKind() + ", " + declared);
        }
    }

    private TypeConcrete and(List<? extends TypeMirror> bounds) {
        TypeAnd and = new TypeAnd(this.typeSystem(), bounds.stream().map(t -> (TypeConcrete) of(t)).collect(Collectors.toCollection(LinkedHashSet::new)));
        and.lock();
        return and;
    }

    private TypeConcrete or(List<? extends TypeMirror> bounds) {
        TypeOr or = new TypeOr(this.typeSystem(), bounds.stream().map(t -> (TypeConcrete) of(t)).collect(Collectors.toCollection(LinkedHashSet::new)));
        or.lock();
        return or;
    }

    private Namespace namespace(DeclaredType type) {
        Element element = type.asElement();
        if (element instanceof TypeElement typeElement) {
            return Namespace.of(typeElement);
        } else {
            throw new IllegalArgumentException("Unrecognized element type: " + element.getKind() + " for type: " + type);
        }
    }

}
