package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.visitor.visitors.ErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.SimpleToStringVisitor;
import honeyroasted.jype.system.visitor.visitors.StripExceptionsTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.ToSignatureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.ToSourceTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.TypeMappingVisitor;
import honeyroasted.jype.system.visitor.visitors.VarWildcardingVisitor;
import honeyroasted.jype.system.visitor.visitors.VerboseToStringVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Signature;
import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.HashSet;

public interface TypeVisitors {
    TypeMappingVisitor<Boolean> ERASURE = new ErasureTypeVisitor();
    TypeMappingVisitor<Object> IDENTITY = new TypeMappingVisitor<>() {
    };
    TypeMappingVisitor<Void> VAR_WIDLCARDER = new VarWildcardingVisitor().withContext(() -> new InMemoryTypeCache<>(Type.class, Type.class));
    TypeMappingVisitor<Void> ERASE_EXCEPTIONS = new StripExceptionsTypeVisitor().withContext(() -> new InMemoryTypeCache<>(Type.class, Type.class));
    TypeVisitor<Boolean, Void> IS_PROPER_TYPE = new RecursiveTypeVisitor<Boolean, Void>((TypeVisitor.Default) (type, context) -> !(type instanceof MetaVarType), null, false)
            .mapResult(ls -> ls.stream().allMatch(b -> b != null && b)).withContext(HashMap::new);

    TypeVisitor<Signature, ToSignatureTypeVisitor.Mode> TO_SIGNATURE = new ToSignatureTypeVisitor();

    TypeVisitor<Signature, ToSignatureTypeVisitor.Mode> TO_DESCRIPTOR = ERASE_EXCEPTIONS.andThen(ERASURE.andThen(TO_SIGNATURE, true));

    TypeVisitor<String, ToSourceTypeVisitor.Mode> TO_SOURCE = new ToSourceTypeVisitor();

    TypeVisitor<String, Void> TO_STRING_SIMPLE = new SimpleToStringVisitor()
            .withContext(HashSet::new);
    TypeVisitor<String, Void> TO_STRING_DETAIL_NAMES = new VerboseToStringVisitor(false, false, false, false, false, "", "")
            .withContext(HashSet::new);
    TypeVisitor<String, Void> TO_STRING_DETAIL = new VerboseToStringVisitor(false, false, true, true, false, "", "")
            .withContext(HashSet::new);
    TypeVisitor<String, Void> TO_STRING_ALL = new VerboseToStringVisitor(true, true, true, true, true, "{", "}")
            .withContext(HashSet::new);

    static <T> TypeMappingVisitor<T> identity() {
        return (TypeMappingVisitor<T>) IDENTITY;
    }

}
