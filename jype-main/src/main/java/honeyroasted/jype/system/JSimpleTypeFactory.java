package honeyroasted.jype.system;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;
import honeyroasted.jype.type.impl.JArrayTypeImpl;
import honeyroasted.jype.type.impl.JClassReferenceImpl;
import honeyroasted.jype.type.impl.JFieldReferenceImpl;
import honeyroasted.jype.type.impl.JIntersectionTypeImpl;
import honeyroasted.jype.type.impl.JMetaVarTypeImpl;
import honeyroasted.jype.type.impl.JMethodReferenceImpl;
import honeyroasted.jype.type.impl.JNoneTypeImpl;
import honeyroasted.jype.type.impl.JParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.JParameterizedMethodTypeImpl;
import honeyroasted.jype.type.impl.JPrimitiveTypeImpl;
import honeyroasted.jype.type.impl.JVarTypeImpl;
import honeyroasted.jype.type.impl.JWildTypeLowerImpl;
import honeyroasted.jype.type.impl.JWildTypeUpperImpl;

public class JSimpleTypeFactory implements JTypeFactory {
    private JTypeSystem system;

    public JSimpleTypeFactory(JTypeSystem system) {
        this.system = system;
    }

    @Override
    public JArrayType newArrayType() {
        return new JArrayTypeImpl(this.system);
    }

    @Override
    public JClassReference newClassReference() {
        return new JClassReferenceImpl(this.system);
    }

    @Override
    public JIntersectionType newIntersectionType() {
        return new JIntersectionTypeImpl(this.system);
    }

    @Override
    public JMetaVarType newMetaVarType(String name) {
        return new JMetaVarTypeImpl(this.system, name);
    }

    @Override
    public JMetaVarType newMetaVarType(int identity, String name) {
        return new JMetaVarTypeImpl(this.system, identity, name);
    }

    @Override
    public JMethodReference newMethodReference() {
        return new JMethodReferenceImpl(this.system);
    }

    @Override
    public JNoneType newNoneType(String name) {
        return new JNoneTypeImpl(this.system, name);
    }

    @Override
    public JParameterizedClassType newParameterizedClassType() {
        return new JParameterizedClassTypeImpl(this.system);
    }

    @Override
    public JParameterizedMethodType newParameterizedMethodType() {
        return new JParameterizedMethodTypeImpl(this.system);
    }

    @Override
    public JPrimitiveType newPrimitiveType(JClassNamespace namespace, JClassNamespace box, JDescriptor.Primitive descriptor) {
        return new JPrimitiveTypeImpl(this.system, namespace, box, descriptor);
    }

    @Override
    public JVarType newVarType() {
        return new JVarTypeImpl(this.system);
    }

    @Override
    public JWildType.Lower newLowerWildType() {
        return new JWildTypeLowerImpl(this.system);
    }

    @Override
    public JWildType.Upper newUpperWildType() {
        return new JWildTypeUpperImpl(this.system);
    }

    @Override
    public JFieldReference newFieldReference() {
        return new JFieldReferenceImpl(this.system);
    }
}
