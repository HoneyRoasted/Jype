package honeyroasted.jype.model;

public abstract class PossiblyUnmodifiable {
    private boolean unmodifiable;

    protected void checkUnmodifiable() {
        if (this.unmodifiable) {
            throw new UnsupportedOperationException("This object is currently unmodifiable");
        }
    }

    protected void makeUnmodifiable() {}

    public boolean isUnmodifiable() {
        return this.unmodifiable;
    }

    public void setUnmodifiable(boolean unmodifiable) {
        if (!this.unmodifiable && unmodifiable) {
            makeUnmodifiable();
        }
        this.unmodifiable = unmodifiable;
    }
}
