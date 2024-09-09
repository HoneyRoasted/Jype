package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

import java.util.Set;
import java.util.function.Function;

public class JIntersectionTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JIntersectionType> implements JIntersectionType {

    public JIntersectionTypeDelegate(JTypeSystem system, Function<JTypeSystem, JIntersectionType> factory) {
        super(system, factory);
    }

    @Override
    public Set<JType> children() {
        return delegate().children();
    }

    @Override
    public JType simplify() {
        return delegate().simplify();
    }

    @Override
    public boolean isSimplified() {
        return delegate().isSimplified();
    }

    @Override
    public void setChildren(Set<JType> children) {
        delegate().setChildren(children);
    }
}
