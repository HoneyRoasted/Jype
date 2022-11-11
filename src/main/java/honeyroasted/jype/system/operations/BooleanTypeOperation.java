package honeyroasted.jype.system.operations;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;

import java.util.stream.Stream;

public interface BooleanTypeOperation extends TypeOperation<Boolean> {

    class Equal extends AbstractTypeOperation<Boolean> implements BooleanTypeOperation {
        public Equal(Type left, Type right) {
            super(String.format("Equality comparison between {%s} and {%s}.", left, right),
                    left, right);
        }

        @Override
        public Boolean value() {
            return this.types.get(0).equals(this.types.get(1));
        }
    }

    class Kind extends AbstractTypeOperation<Boolean> implements BooleanTypeOperation {
        private Type type;
        private Class<?>[] targets;

        public Kind(Type type, Class<?>... targets) {
            super(String.format("Type kind requirement on {%s}, which has kind %s. Required kind in %s.",
                    type, (type == null ? null : type.kind()),
                    Stream.of(targets).map(c -> Namespace.of(c).simpleName().replace('$', '.')).toList()), type);
            this.type = type;
            this.targets = targets;
        }

        @Override
        public Boolean value() {
            for (Class<?> type : this.targets) {
                if (type.isInstance(this.type)) {
                    return true;
                }
            }
            return false;
        }
    }

}
