package honeyroasted.jype.system.solver.exception;

import honeyroasted.jype.system.solver.TypeBound;

import java.util.Set;

public class TypeSolverException extends RuntimeException {
    private Set<TypeBound> initialBounds;

    public TypeSolverException(Set<TypeBound> initialBounds) {
        this.initialBounds = initialBounds;
    }

    public TypeSolverException(String message, Set<TypeBound> initialBounds) {
        super(message);
        this.initialBounds = initialBounds;
    }

    public TypeSolverException(String message, Throwable cause, Set<TypeBound> initialBounds) {
        super(message, cause);
        this.initialBounds = initialBounds;
    }

    public TypeSolverException(Throwable cause, Set<TypeBound> initialBounds) {
        super(cause);
        this.initialBounds = initialBounds;
    }

    public TypeSolverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Set<TypeBound> initialBounds) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.initialBounds = initialBounds;
    }
}
