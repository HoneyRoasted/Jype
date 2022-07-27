package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.declaration.TypeParameter;

import java.util.HashMap;
import java.util.Map;

public class TypeContext {
    private Map<TypePlaceholder, TypeConcrete> placeholders = new HashMap<>();
    private Map<TypeParameter, TypeConcrete> parameters = new HashMap<>();

    public TypeContext put(TypePlaceholder placeholder, TypeConcrete concrete) {
        this.placeholders.put(placeholder, concrete);
        return this;
    }

    public TypeContext put(TypeParameter parameter, TypeConcrete concrete) {
        this.parameters.put(parameter, concrete);
        return this;
    }

    public TypeConcrete get(TypePlaceholder placeholder) {
        return this.placeholders.get(placeholder);
    }

    public TypeConcrete get(TypeParameter parameter) {
        return this.parameters.get(parameter);
    }

}
