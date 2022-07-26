package honeyroasted.jype;

import java.util.Arrays;

public class Namespace {
    private String[] path;
    private String name;

    public Namespace(String[] path, String name) {
        this.path = path;
        this.name = name;
    }

    public Namespace(String... elements) {
        this.path = Arrays.copyOfRange(elements, 0, elements.length - 1);
        this.name = elements[elements.length - 1];
    }

}
