package honeyroasted.jype.system.solver.fixtures.raw;

public class RawGeneric<T> {
    @SafeVarargs
    public final void va(T... xs) {
    }

    public <U> U gen(U u) {
        return u;
    }

    public <U extends Comparable<U>> U bounded(U u) {
        return u;
    }

    public T get() {
        return null;
    }
}
