package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeContext {
    private Map<TypeConcrete, TypeConcrete> parameters;

    public TypeContext(Map<TypeConcrete, TypeConcrete> parameters) {
        this.parameters = parameters;
    }

    public TypeContext() {
        this(new HashMap<>());
    }

    public TypeContext put(TypeConcrete parameter, TypeConcrete concrete) {
        this.parameters.put(parameter, concrete);
        return this;
    }

    public Optional<TypeConcrete> get(TypeConcrete parameter) {
        return Optional.ofNullable(this.parameters.get(parameter));
    }

    public Map<TypeConcrete, TypeConcrete> parameters() {
        return parameters;
    }
}
