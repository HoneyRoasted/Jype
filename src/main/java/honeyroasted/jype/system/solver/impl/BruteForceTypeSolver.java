package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.type.TypeParameter;

import java.util.function.Predicate;

public class BruteForceTypeSolver extends AbstractTypeSolver {
    private Predicate<TypeParameter> infer;

    public BruteForceTypeSolver(Predicate<TypeParameter> infer) {
        this.infer = infer;
    }

    @Override
    public TypeSolution solve() {
        return null;
    }

}
