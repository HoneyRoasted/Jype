package honeyroasted.jype.type;

import honeyroasted.jype.model.PossiblyUnmodifiable;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;

public abstract class WildType extends AbstractPossiblyUnmodifiableType {

    public WildType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    public static class Upper extends WildType {
        private Type upperBound;

        public Upper(TypeSystem typeSystem, Type upperBound) {
            super(typeSystem);
            this.upperBound = upperBound;
        }

        @Override
        public Type upperBound() {
            return this.upperBound;
        }

        @Override
        public Type lowerBound() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Upper upper = (Upper) o;
            return Objects.equals(upperBound, upper.upperBound);
        }

        @Override
        public int hashCode() {
            return Objects.hash(upperBound);
        }

        @Override
        public String toString() {
            return "? extends " + this.upperBound;
        }
    }

    public static class Lower extends WildType {
        private Type lowerBound;

        public Lower(TypeSystem typeSystem, Type lowerBound) {
            super(typeSystem);
            this.lowerBound = lowerBound;
        }

        @Override
        public Type upperBound() {
            return this.typeSystem().OBJECT;
        }

        @Override
        public Type lowerBound() {
            return this.lowerBound;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Lower lower = (Lower) o;
            return Objects.equals(lowerBound, lower.lowerBound);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lowerBound);
        }

        @Override
        public String toString() {
            return "? super " + this.lowerBound;
        }
    }

    public abstract Type upperBound();

    public abstract Type lowerBound();

}
