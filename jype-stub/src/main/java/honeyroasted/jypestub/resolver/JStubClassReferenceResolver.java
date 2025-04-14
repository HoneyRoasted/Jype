package honeyroasted.jypestub.resolver;

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
import honeyroasted.jype.system.resolver.binary.JBinaryLookupException;
import honeyroasted.jype.system.resolver.general.JSignatureTypeResolution;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.impl.delegate.JClassReferenceDelegate;
import honeyroasted.jypestub.model.types.JStubClass;

import java.lang.reflect.AccessFlag;
import java.util.Set;

public class JStubClassReferenceResolver implements JTypeResolver<JStubClass, JType> {

    @Override
    public JResolutionResult<JStubClass, JType> resolve(JTypeSystem system, JStubClass value) {
        JClassLocation loc = JClassLocation.of(value.location());
        JResolutionResult<JClassLocation, JType> cached = system.storage().cacheFor(JClassLocation.class).asResolution(loc);
        if (cached.success()) {
            return JResolutionResult.inherit(value, cached);
        }

        JClassName name = JClassName.of(loc.getPackage().toArray(), value.name().split("[\\./]"));

        JClassReference ref = system.typeFactory().newClassReference();
        ref.setNamespace(new JClassNamespace(loc, name));
        system.storage().cacheFor(JClassLocation.class).put(loc, ref);

        ref.setModifiers(makeMask(value.flags()));

        JSignatureParser parser = new JSignatureParser(value.def());
        try {
            JSignature.ClassDeclaration decl = parser.parseClassDeclaration();
            JSignatureTypeResolution.applyClassSignature(system, ref, decl);

        } catch (JStringParseException | JResolutionFailedException | JBinaryLookupException ex) {
            throw ex;
        }

        value.fields().forEach((fieldName, stubMember) -> {
            JFieldLocation floc = new JFieldLocation(fieldName, loc);
            JFieldReference fRef = system.typeFactory().newFieldReference();
            ref.declaredFields().add(fRef);

            fRef.setLocation(floc);
            fRef.setModifiers(makeMask(stubMember.flags()));
            fRef.setOuterClass(ref);

            fRef.setType(JSignatureTypeResolution.resolveTypeSig(system, ref,
                    new JSignatureParser(stubMember.def()).parseInformalType()));
            fRef.setUnmodifiable(true);
        });

        value.methods().forEach((methodName, stubMember) -> {
            JMethodReference mRef = system.typeFactory().newMethodReference();
            ref.declaredMethods().add(mRef);

            mRef.setModifiers(makeMask(stubMember.flags()));
            mRef.setOuterClass(ref);

            JSignatureTypeResolution.applyMethodSignature(system, mRef,
                    new JSignatureParser(stubMember.def()).parseMethodDeclaration());

            mRef.setLocation(new JMethodLocation(mRef.outerClass().namespace().location(),
                    methodName, JClassLocation.ofDescriptor(mRef.returnType().descriptor().toString()),
                    mRef.parameters().stream().map(p -> JClassLocation.ofDescriptor(p.descriptor().toString())).toList()));

            mRef.setUnmodifiable(true);
        });

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

    private static int makeMask(Set<AccessFlag> flags) {
        int m = 0;
        for (AccessFlag flag : flags) {
            m |= flag.mask();
        }
        return m;
    }

}
