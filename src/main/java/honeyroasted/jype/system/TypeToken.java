package honeyroasted.jype.system;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.resolution.TypeTokenTypeResolver;

/**
 * This is a utility class used by {@link TypeTokenTypeResolver} to facilitate easy creation of {@link Type}s from source code.
 * It works by being an abstract class, forcing any instantiations to create an anonymous subclass. This has the
 * effect of reifying the type variable {@code A} in the anonymous subclass's superclass information. The expected use
 * is {@code new TypeToken<MyType>() {}}.
 *
 * @param <A> The type to reify
 */
public abstract class TypeToken<A> {

}
