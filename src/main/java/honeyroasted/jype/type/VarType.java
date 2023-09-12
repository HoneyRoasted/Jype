package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VarType extends AbstractPossiblyUnmodifiableType {
    private String name;
    private List<Type> bounds = new ArrayList<>();

    public VarType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.bounds = List.copyOf(this.bounds);
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        super.checkUnmodifiable();
        this.name = name;
    }

    public List<Type> bounds() {
        return this.bounds;
    }

    public void setBounds(List<Type> bounds) {
        super.checkUnmodifiable();
        this.bounds = bounds;
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
        sb.append(this.name);

        if (!this.bounds().isEmpty()) {
            sb.append(" extends ");
            for (int i = 0; i < this.bounds().size(); i++) {
                sb.append(this.bounds().get(i));
                if (i < this.bounds.size() - 1) {
                    sb.append(" & ");
                }
            }
        }

        return super.toString();
    }
}
