package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassBytecode;
import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassName;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import org.glavo.classfile.Attributes;
import org.glavo.classfile.ClassFile;
import org.glavo.classfile.ClassModel;
import org.glavo.classfile.ClassSignature;
import org.glavo.classfile.attribute.EnclosingMethodAttribute;
import org.glavo.classfile.attribute.InnerClassesAttribute;
import org.glavo.classfile.attribute.SignatureAttribute;
import org.glavo.classfile.constantpool.ClassEntry;

import java.lang.constant.ClassDesc;
import java.util.Optional;

public class JRawBinaryClassReferenceResolver implements JTypeResolver<JClassBytecode, JType> {

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, JClassBytecode value) {
        ClassModel model = ClassFile.of().parse(value.bytes());

        ClassDesc desc = model.thisClass().asSymbol();
        JClassLocation location = JClassLocation.of(model);

        //------ check the cache
        Optional<JType> cached = system.storage().cacheFor(JClassLocation.class).get(location);
        if (cached.isPresent() && cached.get() instanceof JClassReference cRef) {
            return Optional.of(cRef);
        }
        //--------

        JClassName name;
        JClassLocation outerType = null;
        JMethodLocation outerMethod = null;

        Optional<InnerClassesAttribute> innerAttr = model.findAttribute(Attributes.INNER_CLASSES);
        Optional<EnclosingMethodAttribute> enclMeth = model.findAttribute(Attributes.ENCLOSING_METHOD);
        if (innerAttr.isEmpty() && enclMeth.isEmpty()) {
            name = JClassName.of(desc.packageName().split("\\."), desc.displayName());
        } else {
            name = JClassName.of(desc.packageName().split("\\."), desc.displayName());
            //TODO more advanced processing
        }

        JClassNamespace namespace = new JClassNamespace(location, name);

        JClassReference ref = system.typeFactory().newClassReference();
        ref.setNamespace(namespace);
        ref.setModifiers(model.flags().flagsMask());

        system.storage().cacheFor(JClassLocation.class).put(location, ref);

        Optional<SignatureAttribute> sigAttr = model.findAttribute(Attributes.SIGNATURE);
        if (sigAttr.isPresent()) {
            ClassSignature signature = sigAttr.get().asClassSignature();
            //TODO signature building
        } else {
            if (model.superclass().isPresent()) {
                Optional<? extends JType> superclass = system.resolve(JClassLocation.of(model.superclass().get()));
                if (superclass.isPresent() && superclass.get() instanceof JClassType ct) {
                    ref.setSuperClass(ct);
                } else {
                    system.storage().cacheFor(JClassLocation.class).remove(location);
                    return Optional.empty();
                }
            }

            for (ClassEntry inter : model.interfaces()) {
                Optional<? extends JType> interOpt = system.resolve(JClassLocation.of(inter));
                if (interOpt.isPresent() && interOpt.get() instanceof JClassType ct) {
                    ref.interfaces().add(ct);
                } else {
                    system.storage().cacheFor(JClassLocation.class).remove(location);
                    return Optional.empty();
                }
            }

            if (outerMethod != null) {
                Optional<? extends JType> methodOpt = system.resolve(outerMethod);
                if (methodOpt.isPresent() && methodOpt.get() instanceof JMethodReference mref) {
                    ref.setOuterMethod(mref);

                    if (outerType == null) {
                        ref.setOuterClass(mref.outerClass());
                    }
                } else {
                    system.storage().cacheFor(JClassLocation.class).remove(location);
                    return Optional.empty();
                }
            }

            if (outerType != null) {
                Optional<? extends JType> outerOpt = system.resolve(outerType);
                if (outerOpt.isPresent() && outerOpt.get() instanceof JClassReference cref) {
                    ref.setOuterClass(cref);
                } else {
                    system.storage().cacheFor(JClassLocation.class).remove(location);
                    return Optional.empty();
                }
            }
        }

        ref.setUnmodifiable(true);
        return Optional.of(ref);
    }
}
