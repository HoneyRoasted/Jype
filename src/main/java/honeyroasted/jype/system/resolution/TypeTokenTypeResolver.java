package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
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
    public TypeConcrete resolve(TypeToken type) {
        return this.typeSystem().of(((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    @Override
    public TypeDeclaration resolveDeclaration(TypeToken type) {
        return this.typeSystem().declaration(((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    @Override
    public boolean acceptsType(Object type) {
        return super.acceptsType(type) && type.getClass().getGenericSuperclass() instanceof ParameterizedType pt &&
                pt.getActualTypeArguments().length == 1;
    }

    @Override
    public boolean acceptsDeclaration(Object type) {
        return super.acceptsDeclaration(type) && type.getClass().getGenericSuperclass() instanceof ParameterizedType pt &&
                pt.getActualTypeArguments().length == 1;
    }
}
