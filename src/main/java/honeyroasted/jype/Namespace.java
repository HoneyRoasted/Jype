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
import java.util.stream.Collectors;

/**
 * This class represents the Namespace of a {@link TypeDeclaration}.
 */
public class Namespace {
    private List<String> path;
    private int simpleName;

    public Namespace(List<String> path, int simpleName) {
        this.path = List.copyOf(path);
        this.simpleName = simpleName;
    }

    public static Namespace of(String... path) {
        return new Namespace(Arrays.asList(path), 1);
    }

    public static Namespace of(int simpleName, String... path) {
        return new Namespace(Arrays.asList(path), simpleName);
    }

    public static Namespace of(Class cls) {
        List<String> path = new ArrayList<>();
        String[] pack = cls.getPackage().getName().split("\\.");

        int simpleName = 1;
        Class current = cls;

        while (current.isAnonymousClass()) {
            String[] parts = current.getTypeName().split("\\.");
            path.add(0, parts[parts.length - 1]);

            current = current.getEnclosingClass();
            simpleName++;
        }
        path.add(0, current.getSimpleName());
        path.addAll(0, Arrays.asList(pack));

        return new Namespace(path, simpleName);
    }

    public static Namespace of(TypeElement type) {
        List<String> path = new ArrayList<>();

        int simpleName = 1;
        TypeElement current = type;
        while ((current.getEnclosingElement().getKind().isInterface() || current.getEnclosingElement().getKind().isClass()) &&
            current.getEnclosingElement() instanceof TypeElement element) {

            path.add(0, current.getSimpleName().toString());

            current = element;
            simpleName++;
        }

        if (current.getEnclosingElement().getKind() == ElementKind.PACKAGE &&
                current.getEnclosingElement() instanceof PackageElement pack) {
            path.addAll(0, Arrays.asList(pack.getQualifiedName().toString().split("\\.")));
        }

        return new Namespace(path, simpleName);
    }

    public Namespace relative(Namespace other) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        res.addAll(other.path);
        return new Namespace(res, this.simpleName + other.simpleName);
    }

    public Namespace relative(String... path) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        Collections.addAll(res, path);
        return new Namespace(res, this.simpleName + path.length);
    }

    public Namespace relative(List<String> path) {
        List<String> res = new ArrayList<>();
        res.addAll(this.path);
        res.addAll(path);
        return new Namespace(res, this.simpleName + path.size());
    }

    public String name() {
        return String.join(".", this.path);
    }

    public String simpleName() {
        return String.join(".", this.path.subList(this.path.size() - this.simpleName, this.path.size()));
    }

    public String packageName() {
        return String.join("", this.path.subList(0, this.path.size() - this.simpleName));
    }

    public String internalName() {
        return "";
    }

}
