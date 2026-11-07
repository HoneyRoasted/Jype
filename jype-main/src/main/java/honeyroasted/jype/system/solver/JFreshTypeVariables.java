package honeyroasted.jype.system.solver;

import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class JFreshTypeVariables {
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private JFreshTypeVariables() {
    }

    public static JVarType create(JTypeSystem system, String prefix) {
        JVarType var = system.typeFactory().newVarType();
        var.setLocation(new JTypeParameterLocation(null, prefix + "#" + COUNTER.getAndIncrement()));
        var.setUpperBounds(new LinkedHashSet<>());
        var.setLowerBounds(new LinkedHashSet<>());
        return var;
    }

    public static void finish(JVarType var, Set<JType> upper, Set<JType> lower) {
        var.setUpperBounds(new LinkedHashSet<>(upper));
        var.setLowerBounds(new LinkedHashSet<>(lower));
        var.setUnmodifiable(true);
    }
}
