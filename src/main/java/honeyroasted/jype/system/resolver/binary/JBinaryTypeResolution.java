package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.LinkedHashMap;
import java.util.Map;

public interface JBinaryTypeResolution {

    JBundledTypeResolvers BINARY_TYPE_RESOLVERS = new JBundledTypeResolvers(
            new JDescClassReferenceResolver(),
            new JEntryClassReferenceResolver(),
            new JRawBinaryClassReferenceResolver(),

            new JModelClassReferenceResolver(),
            new JDeclaredSignatureTypeResolver()
    );

    static void applyClassSignature(JTypeSystem system, JClassReference ref, JSignature.ClassDeclaration decl) {
        applyVarTypes(system, ref, decl);
        ref.setSuperClass((JClassType) resolveTypeSig(system, ref, decl.superclass()));
        decl.interfaces().forEach(inter -> ref.interfaces().add((JClassType) resolveTypeSig(system, ref, inter)));
    }

    static void applyMethodSignature(JTypeSystem system, JMethodReference ref, JSignature.MethodDeclaration decl) {
        applyVarTypes(system, ref, decl);
        ref.setReturnType(resolveTypeSig(system, ref, decl.returnType()));
        decl.parameters().forEach(it -> ref.parameters().add(resolveTypeSig(system, ref, it)));
        decl.exceptions().forEach(it -> ref.exceptionTypes().add(resolveTypeSig(system, ref, it)));
    }

    static void applyVarTypes(JTypeSystem system, JGenericDeclaration ref, JSignature.GenericDeclaration decl) {
        Map<JSignature.VarTypeDeclaration, JVarType> varTypes = new LinkedHashMap<>();
        //Instantiate them all first in case they refer to each other
        decl.vars().forEach(varDecl -> {
            JVarType varType = system.typeFactory().newVarType();
            varType.setLocation(new JTypeParameterLocation(ref.genericDeclarationLocation(), varDecl.name()));
            ref.typeParameters().add(varType);

            varTypes.put(varDecl, varType);
        });

        //Instantiate the interfaceBounds now
        varTypes.forEach((varDecl, varType) -> {
            if (varDecl.classBound() != null) {
                varType.upperBounds().add(resolveTypeSig(system, ref, varDecl.classBound()));
            }
            varDecl.interfaceBounds().forEach(it -> varType.upperBounds().add(resolveTypeSig(system, ref, it)));
            varType.setUnmodifiable(true);
        });
    }

    static JType resolveTypeSig(JTypeSystem system, JGenericDeclaration containing, JSignature.InformalType sig) {
        if (sig instanceof JSignature.Array jsa) {
            JArrayType jat = system.typeFactory().newArrayType();
            jat.setComponent(resolveTypeSig(system, containing, jsa.component()));
            jat.setUnmodifiable(true);
            return jat;
        } else if (sig instanceof JSignature.Type jst) {
            return system.tryResolve(jst.descriptor().toLocation());
        } else if (sig instanceof JSignature.VarType jvt) {
            return containing.resolveVarType(jvt.name()).orElseThrow(() -> new JBinaryLookupException("Could not resolve type variable " + jvt.name() + " in class " + containing));
        } else if (sig instanceof JSignature.WildType jsw) {
            if (jsw.upper() != null) {
                JWildType.Upper res = system.typeFactory().newUpperWildType();
                res.upperBounds().add(resolveTypeSig(system, containing, jsw.upper()));
                res.setUnmodifiable(true);
                return res;
            } else if (jsw.lower() != null) {
                JWildType.Lower res = system.typeFactory().newLowerWildType();
                res.lowerBounds().add(resolveTypeSig(system, containing, jsw.lower()));
                res.setUnmodifiable(true);
                return res;
            } else {
                JWildType.Upper res = system.typeFactory().newUpperWildType();
                res.setUnmodifiable(true);
                return res;
            }
        } else if (sig instanceof JSignature.IntersectionType jsit) {
            JIntersectionType jit = system.typeFactory().newIntersectionType();
            jsit.types().forEach(it -> jit.children().add(resolveTypeSig(system, containing, it)));
            jit.setUnmodifiable(true);
            return jit;
        } else if (sig instanceof JSignature.Parameterized jsp) {
            JClassReference ref = system.tryResolve(jsp.type().descriptor().toLocation());
            if (jsp.outer() != null || !jsp.parameters().isEmpty()) {
                JParameterizedClassType jpct = system.typeFactory().newParameterizedClassType();
                jpct.setClassReference(ref);
                if (jsp.outer() != null) {
                    jpct.setOuterType((JClassType) resolveTypeSig(system, containing, jsp.outer()));
                }
                jsp.parameters().forEach(it -> jpct.typeArguments().add((JArgumentType) resolveTypeSig(system, containing, it)));
                jpct.setUnmodifiable(true);
                return jpct;
            } else {
                return ref;
            }
        }

        throw new IllegalArgumentException("Unknown signature type: " + (sig == null ? "null" : sig.getClass()));
    }

}
