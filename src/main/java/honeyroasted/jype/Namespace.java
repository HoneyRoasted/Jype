package honeyroasted.jype;

import honeyroasted.jype.type.TypeDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the Namespace of a {@link TypeDeclaration}, that is, it represents a package name and
 * a class name.
 */
public class Namespace {
    private List<String> path;
    private String name;

    /**
     * Creates a new {@link Namespace}.
     *
     * @param path The package name
     * @param name The class name
     */
    public Namespace(List<String> path, String name) {
        this.path = List.copyOf(path);
        this.name = name;
    }

    /**
     * Creates a new {@link Namespace} from a {@link String} array. The last element of the array
     * will be used as the class name.
     *
     * @param elements The package names followed by the class name
     */
    public Namespace(String... elements) {
        List<String> path = new ArrayList<>();
        for (int i = 0; i < elements.length - 1; i++) {
            path.add(elements[i]);
        }

        this.path = Collections.unmodifiableList(path);
        this.name = elements[elements.length - 1];
    }

    /**
     * Creates a new {@link Namespace} from the binary name of a class.
     *
     * @param binaryName The binary name
     * @return A new {@link Namespace}
     */
    public static Namespace binary(String binaryName) {
        return of(binaryName.split("/"));
    }

    /**
     * Creates a new {@link Namespace} from a {@link String} array.
     *
     * @param elements The package names, followed by the class name.
     * @return A new {@link Namespace}
     */
    public static Namespace of(String... elements) {
        return new Namespace(elements);
    }

    /**
     * Creates a new {@link Namespace} from a {@link Class} object.
     *
     * @param clazz The {@link Class} object
     * @return A new {@link Namespace}
     */
    public static Namespace of(Class<?> clazz) {
        return new Namespace(Arrays.asList(clazz.getPackage().getName().split("\\.")),
                clazz.getName().replace(clazz.getPackage().getName() + ".", ""));
    }

    /**
     * @return The full package name, broken up into individual parts
     */
    public List<String> path() {
        return this.path;
    }

    /**
     * @return The class name
     */
    public String simpleName() {
        return this.name;
    }

    /**
     * @return The fully qualified name
     */
    public String name() {
        return String.join(".", this.path) +
                (this.path.isEmpty() ? "" : ".") +
                this.name;
    }

    /**
     * @return The internal name
     */
    public String internalName() {
        return String.join("/", this.path) +
                (this.path.isEmpty() ? "" : "/") +
                this.name.replace('.', '$');
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
