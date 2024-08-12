package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.ArrayTypeImpl;
import honeyroasted.jype.type.impl.ClassReferenceImpl;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;
import honeyroasted.jype.type.impl.MethodReferenceImpl;
import honeyroasted.jype.type.impl.NoneTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedMethodTypeImpl;
import honeyroasted.jype.type.impl.PrimitiveTypeImpl;
import honeyroasted.jype.type.impl.VarTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;

public class SimpleTypeFactory implements TypeFactory {
    private TypeSystem system;

    public SimpleTypeFactory(TypeSystem system) {
        this.system = system;
    }

    @Override
    public ArrayType newArrayType() {
        return new ArrayTypeImpl(this.system);
    }

    @Override
    public ClassReference newClassReference() {
        return new ClassReferenceImpl(this.system);
    }

    @Override
    public IntersectionType newIntersectionType() {
        return new IntersectionTypeImpl(this.system);
    }

    @Override
    public MetaVarType newMetaVarType(String name) {
        return new MetaVarTypeImpl(this.system, name);
    }

    @Override
    public MetaVarType newMetaVarType(int identity, String name) {
        return new MetaVarTypeImpl(this.system, identity, name);
    }

    @Override
    public MethodReference newMethodReference() {
        return new MethodReferenceImpl(this.system);
    }

    @Override
    public NoneType newNoneType(String name) {
        return new NoneTypeImpl(this.system, name);
    }

    @Override
    public ParameterizedClassType newParameterizedClassType() {
        return new ParameterizedClassTypeImpl(this.system);
    }

    @Override
    public ParameterizedMethodType newParameterizedMethodType() {
        return new ParameterizedMethodTypeImpl(this.system);
    }

    @Override
    public PrimitiveType newPrimitiveType(ClassNamespace namespace, ClassReference box, String descriptor) {
        return new PrimitiveTypeImpl(this.system, namespace, box, descriptor);
    }

    @Override
    public VarType newVarType() {
        return new VarTypeImpl(this.system);
    }

    @Override
    public WildType.Lower newLowerWildType() {
        return new WildTypeLowerImpl(this.system);
    }

    @Override
    public WildType.Upper newUpperWildType() {
        return new WildTypeUpperImpl(this.system);
    }
}
