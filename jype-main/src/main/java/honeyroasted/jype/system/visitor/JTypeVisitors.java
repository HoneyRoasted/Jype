package honeyroasted.jype.system.visitor;

import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.system.cache.JInMemoryTypeCache;
import honeyroasted.jype.system.visitor.visitors.JDownwardProjectionVisitor;
import honeyroasted.jype.system.visitor.visitors.JErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JSimpleToStringVisitor;
import honeyroasted.jype.system.visitor.visitors.JStripExceptionsTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JToDescriptorVisitor;
import honeyroasted.jype.system.visitor.visitors.JToSignatureVisitor;
import honeyroasted.jype.system.visitor.visitors.JToSourceTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JTypeMappingVisitor;
import honeyroasted.jype.system.visitor.visitors.JUpwardProjectionVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarWildcardingVisitor;
import honeyroasted.jype.system.visitor.visitors.JVerboseToStringVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Predicate;

public interface JTypeVisitors {
    JTypeMappingVisitor<Object> IDENTITY = new JTypeMappingVisitor<>() {};
    static <T> JTypeMappingVisitor<T> identity() {
        return (JTypeMappingVisitor<T>) IDENTITY;
    }

    //Operations
    JTypeMappingVisitor<Boolean> ERASURE = new JErasureTypeVisitor();
    JTypeMappingVisitor<Void> VAR_WIDLCARDER = new JVarWildcardingVisitor().withContext(() -> new JInMemoryTypeCache<>(JType.class, JType.class));
    JTypeMappingVisitor<Void> ERASE_EXCEPTIONS = new JStripExceptionsTypeVisitor().withContext(() -> new JInMemoryTypeCache<>(JType.class, JType.class));

    static JUpwardProjectionVisitor upwardProjection(Predicate<JType> restricted) {
        return new JUpwardProjectionVisitor(restricted);
    }

    static JDownwardProjectionVisitor downwardProjection(Predicate<JType> restricted) {
        return new JDownwardProjectionVisitor(restricted);
    }

    //Conditions
    JTypeVisitor<Boolean, Void> IS_PROPER_TYPE = new JRecursiveTypeVisitor<Boolean, Void>((JTypeVisitor.Default) (type, context) -> !(type instanceof JMetaVarType), null, false)
            .mapResult(ls -> ls.stream().allMatch(b -> b != null && b)).withContext(HashMap::new);

    static JTypeVisitor<Boolean, Void> typePredicate(Predicate<JType> test) {
        return new JRecursiveTypeVisitor<Boolean, Void>((JTypeVisitor.Default) (type, context) -> test.test(type), null, false)
                .mapResult(ls -> ls.stream().allMatch(b -> b != null && b)).withContext(HashMap::new);
    }

    //String representations
    JTypeVisitor<JSignature, JToSignatureVisitor.Mode> TO_SIGNATURE = new JToSignatureVisitor();
    JTypeVisitor<JDescriptor, Void> TO_DESCRIPTOR = ERASURE.andThen(new JToDescriptorVisitor(), true);
    JTypeVisitor<String, JToSourceTypeVisitor.Mode> TO_SOURCE = new JToSourceTypeVisitor();
    JTypeVisitor<String, Void> TO_STRING_SIMPLE = new JSimpleToStringVisitor()
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_DETAIL_NAMES = new JVerboseToStringVisitor(false, false, false, false, false, "", "")
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_DETAIL = new JVerboseToStringVisitor(false, false, true, true, false, "", "")
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_ALL = new JVerboseToStringVisitor(true, true, true, true, true, "{", "}")
            .withContext(HashSet::new);

}
