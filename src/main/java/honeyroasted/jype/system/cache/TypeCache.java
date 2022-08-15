package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.resolution.TypeResolver;

import java.lang.ref.WeakReference;

/**
 * Represents a cache of {@link Type}s used by a {@link TypeResolver}. Notably, {@link Type}s cached here should
 * not be discarded if a reference to them is still held during their construction. This prevents circular type
 * hierarchies from causing a stack overflow. In practice, this means implementations of this class will either
 * have to maintain all the {@link Type}s in memory or make use of {@link  WeakReference}s.
 *
 * @param <K> The key type supported by this {@link TypeCache}
 */
public interface TypeCache<K> {

    /**
     * Retrieves a {@link Type} from this {@link TypeCache}.
     *
     * @param key   The key of the {@link Type}
     * @param clazz The class of the {@link Type}
     * @param <T>   The type of the {@link Type}
     * @return The cached {@link Type}, or null if it was not found
     */
    <T extends Type> T get(K key, Class<T> clazz);

    /**
     * Caches a {@link Type} at the given key.
     *
     * @param key  The key to cache at
     * @param type The {@link Type} to cache
     * @return this, for method chaining
     */
    TypeCache<K> cache(K key, Type type);

    /**
     * Caches a {@link Type} at the given key and {@link Class}.
     *
     * @param key  The key to cache at
     * @param type The {@link Type} to cache
     * @param clazz The {@link Class} to cache at
     * @return this, for method chaining
     */
    TypeCache<K> cache(K key, Type type, Class<? extends Type> clazz);

    /**
     * Tests whether this {@link TypeCache} has a cached {@link Type} for the given key and {@link Class}.
     *
     * @param key   The key to test
     * @param clazz The class to test
     * @return True if a mapping exists, false otherwise
     */
    default boolean has(K key, Class<? extends Type> clazz) {
        return get(key, clazz) == null;
    }

}
