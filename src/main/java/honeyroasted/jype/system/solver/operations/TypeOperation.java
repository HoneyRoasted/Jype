package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;

import java.util.function.BiFunction;

public interface TypeOperation<I, O> extends BiFunction<TypeSystem, I, O> {

}
