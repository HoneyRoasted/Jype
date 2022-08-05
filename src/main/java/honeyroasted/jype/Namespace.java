package honeyroasted.jype;

import honeyroasted.jype.type.TypeDeclaration;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the Namespace of a {@link TypeDeclaration}. It consists of a path and a simple name.
 * For inner, anonymous, and local classes, the Namespace should be equivalent to the binary namespace. For example,
 * an anonymous class within a class called Test would be converted to Test$1. The namespace should be Test$1.
 */
public class Namespace {
    private List<String> path;
    private int simpleName;

    /**
     * Creates a new {@link Namespace} with the given path, and the number of elements that make up the simple name.
     *
     * @param path The path
     * @param simpleName The number of elements in the simple name
     */
    public Namespace(List<String> path, int simpleName) {
        this.path = List.copyOf(path);
        this.simpleName = simpleName;
    }

    /**
     * Creates a new {@link Namespace} from the given path. The simple name will be the last element of the path.
     *
     * @param path The path
     * @return A new {@link Namespace}
     */
    public static Namespace of(String... path) {
        return new Namespace(Arrays.asList(path), 1);
    }

    /**
     * Creates a new {@link Namespace} from the given path and the number of elements that make up the simple name.
     *
     * @param simpleName The number of elements in the simple name
     * @param path The path
     * @return A new {@link Namespace}
     */
    public static Namespace of(int simpleName, String... path) {
        return new Namespace(Arrays.asList(path), simpleName);
    }

    /**
     * Creates a new {@link Namespace} corresponding to a runtime class.
     *
     * @param cls The {@link Class} to generate a {@link Namespace} for
     * @return A new {@link Namespace}
     */
    public static Namespace of(Class cls) {
        return Namespace.of(cls.getTypeName().split("\\."));
    }

    /**
     * Creates a new {@link Namespace} corresponding to a source class from the Java annotation processing model.
     * Note that this method <b>may not</b> work for anonymous and local classes.
     *
     * @param type The {@link TypeElement} to generate a {@link Namespace} for
     * @return A new {@link Namespace}
     */
    public static Namespace of(TypeElement type) {
        List<String> path = new ArrayList<>();

        StringBuilder name = new StringBuilder();

        TypeElement current = type;
        while ((current.getEnclosingElement().getKind().isInterface() || current.getEnclosingElement().getKind().isClass()) &&
            current.getEnclosingElement() instanceof TypeElement element) {
            name.insert(0, current.getSimpleName().toString() + ((name.length() == 0) ? "" : "$"));
            current = element;
        }

        if (current.getEnclosingElement().getKind() == ElementKind.PACKAGE &&
                current.getEnclosingElement() instanceof PackageElement pack) {
            path.addAll(Arrays.asList(pack.getQualifiedName().toString().split("\\.")));
        }

        path.add(name.toString());

        return new Namespace(path, 1);
    }

    /**
     * @return A {@link Namespace} containing only the simple name of this {@link Namespace}
     */
    public Namespace toSimpleName() {
        return new Namespace(this.path.subList(this.path.size() - this.simpleName, this.path.size()), this.simpleName);
    }

    /**
     * @return A {@link Namespace} containing only the package of this {@link Namespace}
     */
    public Namespace toPackage() {
        return new Namespace(this.path.subList(0, this.path.size() - this.simpleName), 0);
    }

    /**
     * Creates a new {@link Namespace} with the package name of this {@link Namespace} and the simple name
     * of {@code other}.
     *
     * @param other The {@link Namespace} to use for the simple name
     * @return A new {@link Namespace}
     */
    public Namespace packageRelative(Namespace other) {
        return this.toPackage().relative(other.toSimpleName());
    }

    /**
     * Creates a new {@link Namespace} by appending the {@code other} to this {@link Namespace}
     *
     * @param other The {@link Namespace} to append
     * @return A new {@link Namespace}
     */
    public Namespace relative(Namespace other) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        res.addAll(other.path);
        return new Namespace(res, this.simpleName + other.simpleName);
    }

    /**
     * Creates a new {@link Namespace} by appending the given path to this {@link Namespace}.
     *
     * @param path The path to append
     * @return A new {@link Namespace}
     */
    public Namespace relative(String... path) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        Collections.addAll(res, path);
        return new Namespace(res, this.simpleName + path.length);
    }

    /**
     * Creates a new {@link Namespace} by appending the given path to this {@link Namespace}.
     *
     * @param path The path to append
     * @return A new {@link Namespace}
     */
    public Namespace relative(List<String> path) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        res.addAll(path);
        return new Namespace(res, this.simpleName + path.size());
    }

    /**
     * @return The {@link String} representation of the name represented by this {@link Namespace}
     */
    public String name() {
        return String.join(".", this.path);
    }

    /**
     * @return The {@link String} representation of the simple name of this {@link Namespace}
     */
    public String simpleName() {
        return String.join(".", this.path.subList(this.path.size() - this.simpleName, this.path.size()));
    }

    /**
     * @return The {@link String} representation of the package name of this {@link Namespace}
     */
    public String packageName() {
        return String.join("", this.path.subList(0, this.path.size() - this.simpleName));
    }

    /**
     * @return The {@link String} representation of the internal name of this {@link Namespace}
     */
    public String internalName() {
        return String.join("/", this.path);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Namespace namespace = (Namespace) o;

        if (simpleName != namespace.simpleName) return false;
        return Objects.equals(path, namespace.path);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + simpleName;
        return result;
    }
}
