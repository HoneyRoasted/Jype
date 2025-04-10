package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.List;

public class JToDescriptorVisitor implements JTypeVisitor<JDescriptor, Void> {
    @Override
    public JDescriptor visitClassType(JClassType type, Void context) {
        return null;
    }

    @Override
    public JDescriptor visitPrimitiveType(JPrimitiveType type, Void context) {
        return type.descriptor();
    }

    @Override
    public JDescriptor visitNoneType(JNoneType type, Void context) {
        return JDescriptor.Primitive.VOID;
    }

    @Override
    public JDescriptor visitArrayType(JArrayType type, Void context) {
        return new JDescriptor.Array((JDescriptor.Type) visit(type.component()));
    }

    @Override
    public JDescriptor visitMethodType(JMethodType type, Void context) {
        JDescriptor.Type ret = (JDescriptor.Type) visit(type.returnType());
        List<JDescriptor.Type> params = type.parameters().stream()
                .map(this::visit).map(t -> (JDescriptor.Type) t)
                .toList();
        return new JDescriptor.Method(ret, params);
    }

    @Override
    public JDescriptor visitWildcardType(JWildType type, Void context) {
        throw new IllegalArgumentException("Cannot write descriptor for wild card type");
    }

    @Override
    public JDescriptor visitIntersectionType(JIntersectionType type, Void context) {
        throw new IllegalArgumentException("Cannot write descriptor for wild card type");
    }

    @Override
    public JDescriptor visitVarType(JVarType type, Void context) {
        throw new IllegalArgumentException("Cannot write descriptor for wild card type");
    }

    @Override
    public JDescriptor visitMetaVarType(JMetaVarType type, Void context) {
        throw new IllegalArgumentException("Cannot write descriptor for wild card type");
    }


}
