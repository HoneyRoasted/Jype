package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeDeclaration;

/**
 * This interface represents a type resolver capable of transforming some object into a {@link Type}. What method is
 * used to resolve {@link Type}s is left up to implementations.
 *
 * @param <T> The type of objects that can be transformed into {@link TypeConcrete}s
 * @param <K> The type of objects that can be transformed into {@link TypeDeclaration}s
 */
public interface TypeResolver<T, K> {

    /**
     * Resolves a {@link TypeConcrete} from the given object
     *
     * @param type The object to resolve a {@link TypeConcrete} from
     * @return The resolved {@link TypeConcrete}, or null if it could not be resolved
     */
    TypeConcrete resolve(T type);

    /**
     * Resolves a {@link TypeDeclaration} from the given object
     *
     * @param type The object to resolve a {@link TypeDeclaration} from
     * @return The resolved {@link TypeDeclaration}, or null if it could not be resolved
     */
    TypeDeclaration resolveDeclaration(K type);

    /**
     * Determines if this {@link TypeResolver} can resolve a {@link TypeConcrete} from the given object
     *
     * @param type The object to check
     * @return True if this {@link TypeResolver} accepts the given object
     */
    boolean acceptsType(Object type);

    /**
     * Determines if this {@link TypeResolver} can resolve a {@link TypeDeclaration} from the given object
     *
     * @param type The object to check
     * @return True if this {@link TypeResolver} accepts the given object
     */
    boolean acceptsDeclaration(Object type);

}
