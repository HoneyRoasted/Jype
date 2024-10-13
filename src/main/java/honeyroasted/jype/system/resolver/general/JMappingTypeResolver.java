package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.util.function.BiFunction;

public class JMappingTypeResolver<I, O extends JType, M extends JType> implements JTypeResolver<I, M> {
    private JTypeResolver<I, O> backing;
    private BiFunction<JTypeSystem, O, M> mapper;
    private Class<M> resultType;

    public JMappingTypeResolver(JTypeResolver<I, O> backing, BiFunction<JTypeSystem, O, M> mapper, Class<M> resultType) {
        this.backing = backing;
        this.mapper = mapper;
        this.resultType = resultType;
    }

    @Override
    public JResolutionResult<I, M> resolve(JTypeSystem system, I value) {
        return this.backing.resolve(system, value)
                .map(value, t -> this.mapper.apply(system, t), "Failed execute mapper: " + this.mapper);
    }

    @Override
    public Class<I> keyType() {
        return this.backing.keyType();
    }

    @Override
    public Class<M> resultType() {
        return this.resultType;
    }
}
