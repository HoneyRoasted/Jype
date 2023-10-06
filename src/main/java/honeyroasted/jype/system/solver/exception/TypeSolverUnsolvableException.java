package honeyroasted.jype.system.solver.exception;

import honeyroasted.jype.system.solver.TypeBound;

import java.util.Set;

public class TypeSolverUnsolvableException extends TypeSolverException {

    public TypeSolverUnsolvableException(Set<TypeBound> initialBounds) {
        super(initialBounds);
    }

    public TypeSolverUnsolvableException(String message, Set<TypeBound> initialBounds) {
        super(message, initialBounds);
    }

    public TypeSolverUnsolvableException(String message, Throwable cause, Set<TypeBound> initialBounds) {
        super(message, cause, initialBounds);
    }

    public TypeSolverUnsolvableException(Throwable cause, Set<TypeBound> initialBounds) {
        super(cause, initialBounds);
    }

    public TypeSolverUnsolvableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Set<TypeBound> initialBounds) {
        super(message, cause, enableSuppression, writableStackTrace, initialBounds);
    }
}
