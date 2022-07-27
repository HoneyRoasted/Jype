package honeyroasted.jype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Namespace {
    private List<String> path;
    private String name;

    public Namespace(List<String> path, String name) {
        this.path = List.copyOf(path);
        this.name = name;
    }

    public Namespace(String... elements) {
        List<String> path = new ArrayList<>();
        for (int i = 0; i < elements.length - 1; i++) {
            path.add(elements[i]);
        }

        this.path = Collections.unmodifiableList(path);
        this.name = elements[elements.length - 1];
    }

    public static Namespace of(String... elements) {
        return new Namespace(elements);
    }

    public static Namespace of(Class<?> clazz) {
        return new Namespace(Arrays.asList(clazz.getPackage().getName().split("\\.")),
                clazz.getName().replace(clazz.getPackage().getName() + ".", ""));
    }

    public List<String> path() {
        return this.path;
    }

    public String simpleName() {
        return this.name;
    }

    public String name() {
        return String.join(".", this.path) +
                (this.path.isEmpty() ? "" : ".") +
                this.name;
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Namespace namespace = (Namespace) o;

        if (!Objects.equals(path, namespace.path)) return false;
        return Objects.equals(name, namespace.name);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
