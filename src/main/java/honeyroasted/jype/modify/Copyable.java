package honeyroasted.jype.modify;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;

public interface Copyable<K> {

    <T extends K> T copy(TypeCache<Type, Type> cache);

}
