package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.TypeToken;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeDeclaration;

import java.lang.reflect.ParameterizedType;

public class TypeTokenTypeResolver extends AbstractTypeResolver<TypeToken, TypeToken> {

    public TypeTokenTypeResolver(TypeSystem typeSystem, TypeCache<TypeToken> cache) {
        super(typeSystem, cache, TypeToken.class, TypeToken.class);
    }

    @Override
    public Type resolve(TypeToken type) {
        return this.typeSystem().of(((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    @Override
    public TypeDeclaration resolveDeclaration(TypeToken type) {
        return this.typeSystem().declaration(((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

}
