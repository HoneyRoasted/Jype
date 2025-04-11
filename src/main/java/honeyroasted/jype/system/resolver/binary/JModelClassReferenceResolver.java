package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JClassName;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.location.JFieldLocation;
import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JSignatureParser;
import honeyroasted.jype.metadata.signature.JStringParseException;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.impl.delegate.JClassReferenceDelegate;
import honeyroasted.jype.type.impl.delegate.JMethodReferenceDelegate;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.constantpool.ClassEntry;
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

        Optional<InnerClassesAttribute> innerClsAttr = value.findAttribute(Attributes.innerClasses());
        Optional<EnclosingMethodAttribute> enclMethAttr = value.findAttribute(Attributes.enclosingMethod());

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

        Optional<SignatureAttribute> sigAttr = value.findAttribute(Attributes.signature());
        boolean fallback = true;
        if (sigAttr.isPresent()) {
            JSignatureParser parser = new JSignatureParser(sigAttr.get().signature().stringValue());
            try {
                JSignature.ClassDeclaration decl = parser.parseClassDeclaration();
                JSignatureTypeResolution.applyClassSignature(system, ref, decl);
                fallback = false;
            } catch (JStringParseException | JResolutionFailedException | JBinaryLookupException ex) {
                fallback = true;
            }
        }

        if (fallback) {
            if (value.superclass().isPresent()) {
                JClassLocation sloc = JClassLocation.of(value.superclass().get());
                ref.setSuperClass(new JClassReferenceDelegate(system, s -> s.tryResolve(sloc)));
            }

            for (ClassEntry ce : value.interfaces()) {
                JClassLocation iloc = JClassLocation.of(ce);
                ref.interfaces().add(new JClassReferenceDelegate(system, s -> s.tryResolve(iloc)));
            }
        }

        for (FieldModel model : value.fields()) {
            JFieldLocation floc = new JFieldLocation(model.fieldName().stringValue(), loc);
            JFieldReference fRef = system.typeFactory().newFieldReference();
            ref.declaredFields().add(fRef);

            fRef.setLocation(floc);
            fRef.setModifiers(model.flags().flagsMask());
            fRef.setOuterClass(ref);

            Optional<SignatureAttribute> fieldSigAttr = model.findAttribute(Attributes.signature());
            boolean fieldFallback = true;
            if (fieldSigAttr.isPresent()) {
                try {
                    fRef.setType(JSignatureTypeResolution.resolveTypeSig(system, ref,
                            new JSignatureParser(fieldSigAttr.get().signature().stringValue()).parseInformalType()));
                    fieldFallback = false;
                } catch (JStringParseException | JResolutionFailedException | JBinaryLookupException ex) {
                    fieldFallback = true;
                }
            }

            if (fieldFallback) {
                fRef.setType(delayed(system, JClassLocation.of(model.fieldTypeSymbol())));
            }

            fRef.setUnmodifiable(true);
        }

        for (MethodModel model : value.methods()) {
            JMethodReference mRef = system.typeFactory().newMethodReference();
            ref.declaredMethods().add(mRef);

            MethodTypeDesc desc = model.methodTypeSymbol();
            JMethodLocation mloc = JMethodLocation.of(model.methodName().stringValue(), loc, desc);

            mRef.setLocation(mloc);
            mRef.setModifiers(model.flags().flagsMask());
            mRef.setOuterClass(ref);

            Optional<SignatureAttribute> methSigAttr = model.findAttribute(Attributes.signature());
            boolean methodFallback = true;
            if (methSigAttr.isPresent()) {
                try {
                    JSignatureTypeResolution.applyMethodSignature(system, mRef,
                            new JSignatureParser(methSigAttr.get().signature().stringValue()).parseMethodDeclaration());
                    methodFallback = false;
                } catch (JStringParseException | JResolutionFailedException | JBinaryLookupException ex) {
                    methodFallback = true;
                }
            }

            if (methodFallback) {
                JClassLocation ret = mloc.returnType();

                mRef.setReturnType(delayed(system, ret));
                mloc.parameters().forEach(prm -> mRef.parameters().add(delayed(system, prm)));

                Optional<ExceptionsAttribute> exAttr = model.findAttribute(Attributes.exceptions());
                if (exAttr.isPresent()) {
                    for (ClassEntry ce : exAttr.get().exceptions()) {
                        JClassLocation exLoc = JClassLocation.of(ce);
                        mRef.exceptionTypes().add(delayed(system, exLoc));
                    }
                }
            }

            mRef.setUnmodifiable(true);
        }

        ref.setUnmodifiable(true);
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
