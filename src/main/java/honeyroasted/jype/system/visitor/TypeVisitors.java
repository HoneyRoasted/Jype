package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.visitor.visitors.ErasureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.MappingVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.SignatureTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.StripExceptionsTypeVisitor;
import honeyroasted.jype.type.MetaVarType;

import java.util.HashMap;

public interface TypeVisitors {
    MappingVisitor<Boolean> ERASURE = new ErasureTypeVisitor();
    MappingVisitor<Object> IDENTITY = new MappingVisitor<>() {};
    MappingVisitor<Void> ERASE_EXCEPTIONS = new StripExceptionsTypeVisitor().withContext(() -> new InMemoryTypeCache<>());
    TypeVisitor<String, SignatureTypeVisitor.Mode> SIGNATURE = new SignatureTypeVisitor();
    TypeVisitor<String, SignatureTypeVisitor.Mode> DESCRIPTOR = ERASE_EXCEPTIONS.andThen(ERASURE.andThen(SIGNATURE, true));
    TypeVisitor<Boolean, Void> IS_PROPER_TYPE = new RecursiveTypeVisitor<Boolean, Void>((TypeVisitor.Default) (type, context) -> !(type instanceof MetaVarType), null)
            .mapResult(ls -> ls.stream().allMatch(b -> b != null && b)).withContext(HashMap::new);

    static <T> MappingVisitor<T> identity() {
        return (MappingVisitor<T>) IDENTITY;
    }

}
