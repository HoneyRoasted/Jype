package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JConstraintMirrorTracker implements JConstraintTracker {
    private JConstraintTracker primary;
    private List<JConstraintTracker> mirrors;

    public JConstraintMirrorTracker(JConstraintTracker primary, List<JConstraintTracker> mirrors) {
        this.primary = primary;
        this.mirrors = mirrors;
    }

    public JConstraintMirrorTracker(JConstraintTracker primary, JConstraintTracker... mirrors) {
        this(primary, List.of(mirrors));
    }

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        this.primary.push(constraint, op);
        this.mirrors.forEach(mirror -> mirror.push(constraint, op));
        return this;
    }

    @Override
    public JConstraintTracker pop() {
        this.primary.pop();
        this.mirrors.forEach(JConstraintTracker::pop);
        return this;
    }

    @Override
    public JTypeConstraint peek() {
        return this.primary.peek();
    }

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        this.primary.with(constraint, value);
        this.mirrors.forEach(mirror -> mirror.with(constraint, value));
        return this;
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        this.primary.set(value);
        this.mirrors.forEach(mirror -> mirror.set(value));
        return this;
    }

    @Override
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        cons.accept(this);
        return this;
    }

    @Override
    public boolean canChange() {
        return this.primary.canChange();
    }

    @Override
    public JConstraintResult.Status status() {
        return this.primary.status();
    }

    @Override
    public JConstraintResult result() {
        return this.primary.result();
    }

    @Override
    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator() {
        return this.primary.branchIterator();
    }
}
