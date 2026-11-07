package honeyroasted.jype.fuzz;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;

public interface FuzzContext {
    JTypeSystem system();

    JClassReference host();

    JType typeOf(String holderField);

    JClassReference classNamed(String name);
}
