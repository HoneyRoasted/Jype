package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeParameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents a mapping of {@link TypeConcrete}s. It is generally for use in {@link TypeSolver}s to provide
 * a solution mapping of their input types to output types. For example, a {@link TypeSolver} performing inference
 * might provide the mapping of {@link TypeParameter}s to proper types through this class.
 */
public class TypeContext {
    private Map<TypeConcrete, TypeConcrete> parameters;

    /**
     * Creates a new {@link TypeContext}
     *
     * @param parameters The {@link Map} to use as the initial parameters for this {@link TypeContext}
     */
    public TypeContext(Map<TypeConcrete, TypeConcrete> parameters) {
        this.parameters = parameters;
    }

    /**
     * Creates a new {@link TypeContext} with an empty parameter map
     */
    public TypeContext() {
        this(new HashMap<>());
    }

    /**
     * Puts a key-value pair into this {@link TypeContext}. This method overwrites any mappings that already exist
     * under the given key
     *
     * @param parameter The key
     * @param concrete  The value
     * @return This, for method chaining
     */
    public TypeContext put(TypeConcrete parameter, TypeConcrete concrete) {
        this.parameters.put(parameter, concrete);
        return this;
    }

    /**
     * Attempts to get a value from this {@link TypeContext} at the given key.
     *
     * @param parameter The key to search for
     * @return An {@link Optional} containing the value at the given key, or an empty {@link Optional} if no mapping
     * exists
     */
    public Optional<TypeConcrete> get(TypeConcrete parameter) {
        return Optional.ofNullable(this.parameters.get(parameter));
    }

    /**
     * @return The backing {@link Map} of this {@link TypeContext}. Changes to the returnd {@link Map} will be
     * reflected in this {@link TypeContext}
     */
    public Map<TypeConcrete, TypeConcrete> parameters() {
        return this.parameters;
    }
}
