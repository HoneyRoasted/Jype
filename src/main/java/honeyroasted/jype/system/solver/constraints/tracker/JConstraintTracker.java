package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface JConstraintTracker {

    JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op);

    JConstraintTracker pop(JConstraintResult.Operator op);

    JTypeConstraint peek();

    JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value);

    JConstraintTracker set(JConstraintResult.Status value);

    JConstraintTracker then(Consumer<JConstraintTracker> cons);

    boolean canChange();

    JConstraintResult.Status status();

    JConstraintResult result();

    Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator();

    default Spliterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchSpliterator() {
        return Spliterators.spliteratorUnknownSize(branchIterator(), 0);
    }

    default Stream<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchStream() {
        return StreamSupport.stream(branchSpliterator(), false);
    }

    default JConstraintTracker or(JTypeConstraint constraint, Consumer<JConstraintTracker>... actions) {
        push(constraint, JConstraintResult.Operator.OR);
        then(actions);
        pop(JConstraintResult.Operator.OR);
        return this;
    }

    default JConstraintTracker inheritOr(Consumer<JConstraintTracker>... actions) {
        return or(peek(), actions);
    }

    default JConstraintTracker and(JTypeConstraint constraint, Consumer<JConstraintTracker>... actions) {
        push(constraint, JConstraintResult.Operator.AND);
        then(actions);
        pop(JConstraintResult.Operator.AND);
        return this;
    }

    default JConstraintTracker inheritAnd(Consumer<JConstraintTracker>... actions) {
        return and(peek(), actions);
    }

    default JConstraintTracker inherit(JTypeConstraint constraint, Consumer<JConstraintTracker> action) {
        return this.and(constraint, action);
    }

    default JConstraintTracker set(boolean value) {
        return set(JConstraintResult.Status.known(value));
    }

    default JConstraintTracker with(JTypeConstraint constraint) {
        return with(constraint, JConstraintResult.Status.UNKNOWN);
    }

    default JConstraintTracker with(JTypeConstraint constraint, boolean value) {
        return with(constraint, JConstraintResult.Status.known(value));
    }

    default JConstraintTracker then(Consumer<JConstraintTracker>... actions) {
        for (Consumer<JConstraintTracker> action : actions) {
            then(action);
        }
        return this;
    }

}
