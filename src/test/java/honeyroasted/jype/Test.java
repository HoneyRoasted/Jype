package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        System.out.println(system.operations().isCompatible(system.tryResolve(String.class), system.tryResolve(String.class), TypeBound.Compatible.Context.LOOSE_INVOCATION));
    }

}
