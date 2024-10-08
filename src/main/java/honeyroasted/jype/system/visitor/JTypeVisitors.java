package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.cache.JInMemoryTypeCache;
import honeyroasted.jype.system.visitor.visitors.JErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JSimpleToStringVisitor;
import honeyroasted.jype.system.visitor.visitors.JStripExceptionsTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JToSignatureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JToSourceTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JTypeMappingVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarWildcardingVisitor;
import honeyroasted.jype.system.visitor.visitors.JVerboseToStringVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JSignature;
import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.HashSet;

public interface JTypeVisitors {
    JTypeMappingVisitor<Boolean> ERASURE = new JErasureTypeVisitor();
    JTypeMappingVisitor<Object> IDENTITY = new JTypeMappingVisitor<>() {
    };
    JTypeMappingVisitor<Void> VAR_WIDLCARDER = new JVarWildcardingVisitor().withContext(() -> new JInMemoryTypeCache<>(JType.class, JType.class));
    JTypeMappingVisitor<Void> ERASE_EXCEPTIONS = new JStripExceptionsTypeVisitor().withContext(() -> new JInMemoryTypeCache<>(JType.class, JType.class));
    JTypeVisitor<Boolean, Void> IS_PROPER_TYPE = new JRecursiveTypeVisitor<Boolean, Void>((JTypeVisitor.Default) (type, context) -> !(type instanceof JMetaVarType), null, false)
            .mapResult(ls -> ls.stream().allMatch(b -> b != null && b)).withContext(HashMap::new);

    JTypeVisitor<JSignature, JToSignatureTypeVisitor.Mode> TO_SIGNATURE = new JToSignatureTypeVisitor();

    JTypeVisitor<JSignature, JToSignatureTypeVisitor.Mode> TO_DESCRIPTOR = ERASE_EXCEPTIONS.andThen(ERASURE.andThen(TO_SIGNATURE, true));

    JTypeVisitor<String, JToSourceTypeVisitor.Mode> TO_SOURCE = new JToSourceTypeVisitor();

    JTypeVisitor<String, Void> TO_STRING_SIMPLE = new JSimpleToStringVisitor()
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_DETAIL_NAMES = new JVerboseToStringVisitor(false, false, false, false, false, "", "")
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_DETAIL = new JVerboseToStringVisitor(false, false, true, true, false, "", "")
            .withContext(HashSet::new);
    JTypeVisitor<String, Void> TO_STRING_ALL = new JVerboseToStringVisitor(true, true, true, true, true, "{", "}")
            .withContext(HashSet::new);

    static <T> JTypeMappingVisitor<T> identity() {
        return (JTypeMappingVisitor<T>) IDENTITY;
    }

}
