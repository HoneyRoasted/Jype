package honeyroasted.jype.type;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.List;

public abstract sealed class WildType extends AbstractType {

    public WildType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    public static final class Upper extends WildType {
        private List<Type> upperBound;

        public Upper(TypeSystem typeSystem, List<Type> upperBound) {
            super(typeSystem);
            this.upperBound = List.copyOf(upperBound);
        }

        @Override
        public List<Type> upperBounds() {
            return this.upperBound;
        }

        @Override
        public List<Type> lowerBounds() {
            return List.of(this.typeSystem().constants().nullType());
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("? extends ");
            for (int i = 0; i < this.upperBound.size(); i++) {
                sb.append(this.upperBound.get(i));
                if (i < this.upperBound.size() - 1) {
                    sb.append(" & ");
                }
            }
            return sb.toString();
        }
    }

    public static final class Lower extends WildType {
        private List<Type> lowerBound;

        public Lower(TypeSystem typeSystem, List<Type> lowerBound) {
            super(typeSystem);
            this.lowerBound = List.copyOf(lowerBound);
        }

        @Override
        public List<Type> upperBounds() {
            return List.of(this.typeSystem().constants().object());
        }

        @Override
        public List<Type> lowerBounds() {
            return this.lowerBound;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("? super ");
            for (int i = 0; i < this.lowerBound.size(); i++) {
                sb.append(this.lowerBound.get(i));
                if (i < this.lowerBound.size() - 1) {
                    sb.append(" & ");
                }
            }
            return sb.toString();
        }
    }

    public abstract List<Type> upperBounds();

    public abstract List<Type> lowerBounds();

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitWild(this, context);
    }
}
