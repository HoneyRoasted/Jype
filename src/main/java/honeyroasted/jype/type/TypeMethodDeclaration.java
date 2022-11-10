package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.marker.TypePsuedo;
import honeyroasted.jype.system.TypeSystem;

import java.util.List;

/**
 * This class represents a method declaration pseudo-type. It is mostly provided for generating method descriptors
 * and signatures.
 */
public class TypeMethodDeclaration extends AbstractType implements TypePsuedo {
    private TypeConcrete returnType;
    private List<TypeParameter> typeParameters;
    private List<TypeConcrete> parameterTypes;
    private List<TypeConcrete> exceptionTypes;

    /**
     * Creates a new {@link TypeMethodDeclaration}.
     *
     * @param typeSystem The {@link TypeSystem} associated with this {@link TypeMethodDeclaration}
     * @param returnType The return {@link TypeConcrete} of this {@link TypeMethodDeclaration}
     * @param typeParameters The {@link TypeParameter}s of this {@link TypeMethodDeclaration}
     * @param parameterTypes The {@link TypeConcrete}s of the method parameters of this {@link TypeMethodDeclaration}
     * @param exceptionTypes The exception {@link TypeConcrete}s of the exceptions thrown by this {@link TypeMethodDeclaration}
     */
    public TypeMethodDeclaration(TypeSystem typeSystem, TypeConcrete returnType, List<TypeParameter> typeParameters, List<TypeConcrete> parameterTypes, List<TypeConcrete> exceptionTypes) {
        super(typeSystem);
        this.returnType = returnType;
        this.typeParameters = typeParameters;
        this.parameterTypes = parameterTypes;
        this.exceptionTypes = exceptionTypes;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        StringBuilder sb = new StringBuilder();
        if (!this.typeParameters.isEmpty()) {
            sb.append("<");
            for (TypeParameter parameter : this.typeParameters) {
                TypeString prmStr = parameter.toSignature(TypeString.Context.DECLARATION);
                if (prmStr.successful()) {
                    sb.append(prmStr.value());
                } else {
                    return prmStr;
                }
            }
            sb.append(">");
        }

        TypeString retSig = this.returnType.toSignature(TypeString.Context.CONCRETE);
        if (retSig.successful()) {
            sb.append("(");
            for (TypeConcrete parameter : this.parameterTypes) {
                TypeString prmStr = parameter.toSignature(TypeString.Context.CONCRETE);
                if (prmStr.successful()) {
                    sb.append(prmStr.value());
                } else {
                    return prmStr;
                }
            }
            sb.append(")").append(retSig.value());

            for (TypeConcrete exception : this.exceptionTypes) {
                TypeString exStr = exception.toSignature(TypeString.Context.CONCRETE);
                if (exStr.successful()) {
                    sb.append("^").append(exStr.value());
                } else {
                    return exStr;
                }
            }

            return TypeString.successful(sb.toString(), TypeMethodDeclaration.class, TypeString.Target.SIGNATURE);
        } else {
            return retSig;
        }
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        StringBuilder sb = new StringBuilder();

        TypeString retDesc = this.returnType.toDescriptor(TypeString.Context.CONCRETE);
        if (retDesc.successful()) {
            sb.append("(");
            for (TypeConcrete type : this.parameterTypes) {
                TypeString prmDesc = type.toDescriptor(TypeString.Context.CONCRETE);
                if (prmDesc.successful()) {
                    sb.append(prmDesc.value());
                } else {
                    return prmDesc;
                }
            }
            sb.append(")").append(retDesc.value());
        } else {
            return retDesc;
        }

        return TypeString.successful(sb.toString(), TypeMethodDeclaration.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure(TypeMethodDeclaration.class, TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        return null;
    }

    @Override
    public boolean equalsExactly(TypeConcrete other) {
        return false;
    }

    @Override
    public int hashCodeExactly() {
        return 0;
    }
}
