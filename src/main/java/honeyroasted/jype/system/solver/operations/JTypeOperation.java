package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;

import java.util.function.BiFunction;

public interface JTypeOperation<I, O> extends BiFunction<JTypeSystem, I, O> {

}
