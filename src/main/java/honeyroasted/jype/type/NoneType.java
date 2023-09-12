package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;

public class NoneType extends AbstractType {
    private final String name;

    public NoneType(TypeSystem system, String name) {
        super(system);
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoneType noneType = (NoneType) o;
        return Objects.equals(name, noneType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "#" + this.name;
    }
}
