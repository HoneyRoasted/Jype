package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.JType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record JResolutionResult<I, O extends JType>(O value, String message, I lookup, Exception ex,
                                                    List<JResolutionResult<?, ?>> children) {

    public static <T, V extends JType> JResolutionResult<T, V> inherit(T lookup, JResolutionResult<?, ? extends V> result, String failedMessage) {
        if (result.success()) {
            return new JResolutionResult<>(result.value, lookup, List.of(result));
        } else {
            return new JResolutionResult<>(failedMessage, lookup, List.of(result));
        }
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(T lookup, JResolutionResult<?, ? extends V> result) {
        return inherit(lookup, result, "Child lookup failed");
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(T lookup, List<? extends JResolutionResult<?, ? extends V>> result, String failedMessage) {
        Optional<V> resolve = result.stream().filter(JResolutionResult::success).findFirst().map(JResolutionResult::value);
        if (resolve.isPresent()) {
            return new JResolutionResult(resolve.get(), lookup, (List) result);
        } else {
            return new JResolutionResult(failedMessage, lookup, (List) result);
        }
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(T lookup, List<? extends JResolutionResult<?, ? extends V>> result) {
        return inherit(lookup, result, result.isEmpty() ? "No child lookups could be performed" :
                result.size() == 1 ? "Child lookup failed" : "All child lookups failed");
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(V value, T lookup, List<? extends JResolutionResult<?, ?>> result, String failedMessage) {
        if (result.stream().allMatch(JResolutionResult::success)) {
            return new JResolutionResult(value, lookup, (List) result);
        } else {
            return new JResolutionResult(failedMessage, lookup, (List) result);
        }
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(V value, T lookup, List<? extends JResolutionResult<?, ?>> result) {
        return inherit(value, lookup, result, "Failed to resolve one or more components");
    }

    public static <T, V extends JType> JResolutionResult<T, V> inherit(T lookup, Optional<? extends V> value, String failedMessage) {
        return value.<JResolutionResult<T, V>>map(v -> new JResolutionResult<>(v, lookup))
                .orElseGet(() -> new JResolutionResult<>(failedMessage, lookup));
    }

    public JResolutionResult(O value, I lookup, List<JResolutionResult<?, ?>> children) {
        this(value, "success", lookup, null, children);
    }

    public JResolutionResult(O value, I lookup) {
        this(value, lookup, Collections.emptyList());
    }

    public JResolutionResult(String message, I lookup, Exception ex, List<JResolutionResult<?, ?>> children) {
        this(null, message, lookup, ex, children);
    }

    public JResolutionResult(String message, I lookup, Exception ex) {
        this(message, lookup, ex, Collections.emptyList());
    }

    public JResolutionResult(String message, I lookup, List<JResolutionResult<?, ?>> children) {
        this(message, lookup, null, children);
    }

    public JResolutionResult(String message, I lookup) {
        this(message, lookup, Collections.emptyList());
    }

    public <T, V extends JType> JResolutionResult<T, V> map(Function<I, T> lookup, Function<O, V> mapper, String mappingFailed) {
        T mapLookup = lookup.apply(this.lookup);
        if (this.success()) {
            V mapped = mapper.apply(this.value);
            if (mapped != null) {
                return new JResolutionResult<>(mapped, mapLookup, List.of(this));
            }
        }

        return new JResolutionResult<>(mappingFailed, mapLookup, List.of(this));
    }

    public <T, V extends JType> JResolutionResult<T, V> map(T lookup, Function<O, V> mapper, String mappingFailed) {
        return map(i -> lookup, mapper, mappingFailed);
    }

    public <T, V extends JType> JResolutionResult<T, V> flatMap(Function<I, T> lookup, Function<O, Optional<V>> mapper, String mappingFailed) {
        return map(lookup, o -> mapper.apply(o).orElse(null), mappingFailed);
    }

    public <T, V extends JType> JResolutionResult<T, V> flatMap(T lookup, Function<O, Optional<V>> mapper, String mappingFailed) {
        return flatMap(i -> lookup, mapper, mappingFailed);
    }

    public O getOrThrow() {
        if (value == null) throw new JResolutionFailedException("Failed lookup: " + lookup + " -> " + message, ex, this);
        return value;
    }

    public O getOrDefault(O def) {
        if (value == null) return def;
        return value;
    }

    public boolean success() {
        return value != null;
    }

    public boolean failure() {
        return value == null;
    }

    private void toString(String indent, StringBuilder sb, boolean includeSuccessChildren) {
        if (value != null) {
            sb.append(indent).append("Successful lookup: [").append(lookup.getClass().getName()).append("] ").append(lookup).append(" -> ").append(value).append("\n");
        } else {
            sb.append(indent).append("Failed lookup: [").append(lookup.getClass().getName()).append("] ").append(lookup).append(" -> ").append(message).append("\n");
            if (ex != null) {
                sb.append(indent).append("EXCEPTION: ").append(ex).append("\n");
            }
        }

        if (!children.isEmpty() && (includeSuccessChildren || value == null)) {
            sb.append(indent).append("CHILDREN:\n");
            children.forEach(r -> r.toString(indent + "    ", sb, includeSuccessChildren));
        }
    }

    public String toString(boolean includeSuccessChildren) {
        StringBuilder sb = new StringBuilder();
        toString("", sb, includeSuccessChildren);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(false);
    }

}
