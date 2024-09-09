package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JConstraintTree {
    private JConstraintTree parent;

    private Set<Object> metadata = Collections.newSetFromMap(new IdentityHashMap<>());
    private Map<Constraint, JConstraintResult.Status> constraints = new LinkedHashMap<>();
    private Set<JConstraintTree> children = new LinkedHashSet<>();

    public JConstraintTree(JConstraintTree parent) {
        this.parent = parent;
    }

    public JConstraintTree() {
        this(null);
    }

    private void propagateMetadata(Object data) {
        if (this.children.stream().allMatch(child -> child.metadata.contains(data))) {
            this.metadata.add(data);
            this.children.forEach(child -> child.metadata.remove(data));
        }
    }

    private boolean propagate(Constraint constraint, JConstraintResult.Status status) {
        if (this.children.stream().allMatch(child -> child.constraints.get(constraint) == status)) {
            //Constraint is present in all children and should be lifted to this

            //Manually call put so the status is not recalculated (lifting should not corrupt the and result)
            this.constraints.put(constraint, status);

            Iterator<JConstraintTree> iter = this.children.iterator();
            while (iter.hasNext()) {
                JConstraintTree child = iter.next();
                child.remove(constraint);
                if (child.constraints.isEmpty()) iter.remove();
            }

            this.invalidateStatus();
            if (this.parent != null) this.parent.propagate(constraint, status);
            return true;
        }
        return false;
    }

    private JConstraintResult.Status status;

    private void calculateStatus() {
        //And all the constraints in this, then && that with all the children which were || together
        this.status = this.constraints.values().stream().reduce(JConstraintResult.Status.ASSUMED, JConstraintResult.Status::and)
                .and(this.children.stream().map(JConstraintTree::status).reduce(JConstraintResult.Status.UNKNOWN, JConstraintResult.Status::or));
    }

    private void invalidateStatus() {
        this.status = null;
        if (this.parent != null && this.parent.status().isTrue()) {
            //If the parent status is true, it could be changed if this was the only true status and has become false
            this.parent.invalidateStatus();
        }
    }

    public JConstraintResult.Status status() {
        if (this.status == null) calculateStatus();
        return this.status;
    }

    public boolean remove(Constraint constraint) {
        JConstraintResult.Status prev = this.constraints.remove(constraint);
        if (prev != null && prev.isFalse()) {
            //If a false status was removed, the current and result becomes corrupted
            this.invalidateStatus();
        }
        return prev != null;
    }

    public boolean put(Constraint constraint, JConstraintResult.Status status) {
        JConstraintResult.Status prev = this.constraints.put(constraint, status);
        if (prev == null || prev.isTrue()) {
            //Either there was no previous status, or the previous status was true and would not affect the current and result
            this.status = this.status().and(status);
        } else {
            //Previous status was false so the current and result is corrupted
            this.invalidateStatus();
        }

        if (this.parent != null) {
            //Propagate the status upwards, so it can be lifted into the parent if necessary
            return this.parent.propagate(constraint, status) || prev != status;
        }
        return prev != status;
    }

    private Map<Constraint, JConstraintResult.Status> queue = new LinkedHashMap<>();

    public void queue(Constraint constraint, JConstraintResult.Status status) {
        this.queue.put(constraint, status);
    }

    public boolean integrateQueue() {
        boolean mod = false;
        for (Map.Entry<Constraint, JConstraintResult.Status> entry : this.queue.entrySet()) {
            mod |= put(entry.getKey(), entry.getValue());
        }
        this.queue.clear();
        return mod;
    }

    public JConstraintTree or(Consumer<JConstraintTree>... actions) {
        if (this.children.isEmpty()) {
            if (actions.length == 1) {
                actions[0].accept(this);
            } else {
                for (Consumer<JConstraintTree> action : actions) {
                    JConstraintTree child = new JConstraintTree(this);
                    this.children.add(child);
                    action.accept(child);
                }
            }
        } else {
            this.children.forEach(tree -> tree.or(actions));
        }
        return this;
    }

    public <T> Set<T> allMetadata(Class<T> type) {
        return (Set<T>) this.metadataStream().filter(type::isInstance).collect(Collectors.toSet());
    }

    public <T> Optional<T> firstMetadata(Class<T> type) {
        return (Optional<T>) this.metadataStream().filter(type::isInstance).findFirst();
    }

    public <T> T firstMetadata(Class<T> type, T def) {
        return this.firstMetadata(type).orElse(def);
    }

    public void attachMetadata(Object data) {
        this.metadata.add(data);
        this.propagateMetadata(data);
    }

    public Iterator<Object> metadataIterator() {
        return new MetadataIterator(this);
    }

    public Spliterator<Object> metadataSpliterator() {
        return Spliterators.spliteratorUnknownSize(metadataIterator(), Spliterator.NONNULL);
    }

    public Stream<Object> metadataStream() {
        return StreamSupport.stream(metadataSpliterator(), false);
    }

    public Iterator<Map.Entry<Constraint, JConstraintResult.Status>> andIterator() {
        return new AndIterator(this);
    }

    public Spliterator<Map.Entry<Constraint, JConstraintResult.Status>> andSpliterator() {
        return Spliterators.spliteratorUnknownSize(andIterator(), Spliterator.NONNULL);
    }

    public Stream<Map.Entry<Constraint, JConstraintResult.Status>> andStream() {
        return StreamSupport.stream(andSpliterator(), false);
    }

    private static class MetadataIterator implements Iterator<Object> {
        private JConstraintTree current;
        private Iterator<Object> currIter;

        public MetadataIterator(JConstraintTree current) {
            this.current = current;
            nextIterator();
        }

        private void nextIterator() {
            JConstraintTree current = this.current;
            while (current != null && current.constraints.isEmpty()) {
                current = current.parent;
            }

            this.current = current;
            this.currIter = this.current.metadata.iterator();
        }

        @Override
        public boolean hasNext() {
            return currIter != null && currIter.hasNext();
        }

        @Override
        public Object next() {
            Object next = this.currIter.next();
            if (!this.currIter.hasNext()) nextIterator();
            return next;
        }
    }

    private static class AndIterator implements Iterator<Map.Entry<Constraint, JConstraintResult.Status>> {
        private JConstraintTree current;
        private Iterator<Map.Entry<Constraint, JConstraintResult.Status>> currIter;

        public AndIterator(JConstraintTree current) {
            this.current = current;
            nextIterator();
        }

        private void nextIterator() {
            JConstraintTree current = this.current;
            while (current != null && current.constraints.isEmpty()) {
                current = current.parent;
            }

            this.current = current;
            this.currIter = this.current.constraints.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return currIter != null && currIter.hasNext();
        }

        @Override
        public Map.Entry<Constraint, JConstraintResult.Status> next() {
            Map.Entry<Constraint, JConstraintResult.Status> next = this.currIter.next();
            if (!this.currIter.hasNext()) nextIterator();
            return next;
        }
    }
}
