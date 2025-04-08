package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JClassName;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.impl.delegate.JClassReferenceDelegate;
import honeyroasted.jype.type.impl.delegate.JMethodReferenceDelegate;
import org.glavo.classfile.Attributes;
import org.glavo.classfile.ClassModel;
import org.glavo.classfile.MethodModel;
import org.glavo.classfile.attribute.EnclosingMethodAttribute;
import org.glavo.classfile.attribute.ExceptionsAttribute;
import org.glavo.classfile.attribute.InnerClassesAttribute;
import org.glavo.classfile.attribute.SignatureAttribute;
import org.glavo.classfile.constantpool.ClassEntry;

import java.lang.constant.MethodTypeDesc;
import java.util.Optional;

public class JModelClassReferenceResolver implements JTypeResolver<ClassModel, JType> {

    @Override
    public JResolutionResult<ClassModel, JType> resolve(JTypeSystem system, ClassModel value) {
        JClassLocation loc = JClassLocation.of(value.thisClass());
        JResolutionResult<JClassLocation, JType> cached = system.storage().cacheFor(JClassLocation.class).asResolution(loc);
        if (cached.success()) {
            return JResolutionResult.inherit(value, cached);
        }

        JClassReference ref = system.typeFactory().newClassReference();
        ref.setNamespace(new JClassNamespace(loc, loc.toName())); //Temporary location-only namespace
        system.storage().cacheFor(JClassLocation.class).put(loc, ref);

        JClassName name = null;

        Optional<InnerClassesAttribute> innerClsAttr = value.findAttribute(Attributes.INNER_CLASSES);
        Optional<EnclosingMethodAttribute> enclMethAttr = value.findAttribute(Attributes.ENCLOSING_METHOD);

        JClassLocation outerClsLoc = null;
        JMethodLocation outerMethLoc = null;
        if (innerClsAttr.isPresent() || enclMethAttr.isPresent()) {
            //TODO Generate correct name
        }

        if (name == null) {
            name = loc.toName();
        }

        ref.setNamespace(new JClassNamespace(loc, name)); //Update namespace with known name
        ref.setModifiers(value.flags().flagsMask());

        if (outerClsLoc != null) {
            ref.setOuterClass(new JClassReferenceDelegate(system, s -> s.tryResolve(outerClsLoc)));
        }

        if (outerMethLoc != null) {
            ref.setOuterMethod(new JMethodReferenceDelegate(system, s -> s.tryResolve(outerMethLoc)));
        }

        Optional<SignatureAttribute> sigAttr = value.findAttribute(Attributes.SIGNATURE);
        if (sigAttr.isPresent()) {
            //TODO Explore signature
        } else {
            if (value.superclass().isPresent()) {
                JClassLocation sloc = JClassLocation.of(value.superclass().get());
                ref.setSuperClass(new JClassReferenceDelegate(system, s -> s.tryResolve(sloc)));
            }

            for (ClassEntry ce : value.interfaces()) {
                JClassLocation iloc = JClassLocation.of(ce);
                ref.interfaces().add(new JClassReferenceDelegate(system, s -> s.tryResolve(iloc)));
            }
        }

        for (MethodModel model : value.methods()) {
            Optional<SignatureAttribute> methSigAttr = model.findAttribute(Attributes.SIGNATURE);
            if (methSigAttr.isPresent()) {
                //TODO Explore signature
            } else {
                JMethodReference mRef = system.typeFactory().newMethodReference();
                ref.declaredMethods().add(mRef);

                MethodTypeDesc desc = model.methodTypeSymbol();
                JMethodLocation mloc = JMethodLocation.of(model.methodName().stringValue(), loc, desc);

                mRef.setLocation(mloc);
                mRef.setModifiers(model.flags().flagsMask());
                mRef.setOuterClass(ref);

                JClassLocation ret = mloc.returnType();

                mRef.setReturnType(delayed(system, ret));
                mloc.parameters().forEach(prm -> mRef.parameters().add(delayed(system, prm)));

                Optional<ExceptionsAttribute> exAttr = model.findAttribute(Attributes.EXCEPTIONS);
                if (exAttr.isPresent()) {
                    for (ClassEntry ce : exAttr.get().exceptions()) {
                        JClassLocation exLoc = JClassLocation.of(ce);
                        mRef.exceptionTypes().add(delayed(system, exLoc));
                    }
                }
            }
        }

        return new JResolutionResult<>(ref, value);
    }

    private static JType delayed(JTypeSystem system, JClassLocation location) {
        if (location.isArray()) {
            JArrayType type = system.typeFactory().newArrayType();
            type.setComponent(delayed(system, location.containing()));
            return type;
        } else if (location.getPackage().isDefaultPackage() && ("void".equals(location.value()) || system.constants().allPrimitives().stream().anyMatch(prim -> prim.namespace().location().equals(location)))) {
            return system.tryResolve(location);
        } else {
            return new JClassReferenceDelegate(system, s -> s.tryResolve(location));
        }
    }

}
