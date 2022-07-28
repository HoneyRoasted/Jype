package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeParameter;

import java.util.HashMap;
import java.util.Map;

public class TypeContext {
    private Map<TypeParameter, TypeConcrete> parameters;

    public TypeContext(Map<TypeParameter, TypeConcrete> parameters) {
        this.parameters = parameters;
    }

    public TypeContext() {
        this(new HashMap<>());
    }

    public TypeContext put(TypeParameter parameter, TypeConcrete concrete) {
        this.parameters.put(parameter, concrete);
        return this;
    }

    public TypeConcrete get(TypeParameter parameter) {
        return this.parameters.get(parameter);
    }

}
