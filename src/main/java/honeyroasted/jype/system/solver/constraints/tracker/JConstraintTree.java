package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JConstraintTree {
    private JConstraintTree parent;

    private Set<Object> metadata = Collections.newSetFromMap(new IdentityHashMap<>());
    private Map<JTypeConstraint, JConstraintResult.Status> constraints = new LinkedHashMap<>();
    private Map<JConstraintTree, JConstraintTree> children = new LinkedHashMap<>();

    public JConstraintTree(JConstraintTree parent) {
        this.parent = parent;
    }

    public JConstraintTree() {
        this(null);
    }

    public JConstraintTree root() {
        return this.parent == null ? this : this.parent.root();
    }

    public JConstraintResult toResult() {
        List<JConstraintResult> building = this.constraints.entrySet().stream().map(ent -> new JConstraintResult(ent.getValue(), ent.getKey(), JConstraintResult.Operator.SET, Collections.emptyList())).collect(Collectors.toList());

        if (this.children.size() == 1) {
            building.add(this.children.keySet().iterator().next().toResult());
        } else if (!this.children.isEmpty()) {
            building.add(new JConstraintResult(children.keySet().stream().map(JConstraintTree::status).reduce(JConstraintResult.Status.FALSE, JConstraintResult.Status::or),
                    JTypeConstraint.or(), JConstraintResult.Operator.OR, this.children.keySet().stream().map(JConstraintTree::toResult).toList()));
        }

        return building.size() == 1 ? building.getFirst() : new JConstraintResult(status(), JTypeConstraint.and(), JConstraintResult.Operator.AND, building);
    }

    private boolean doChange(BooleanSupplier supplier) {
        if (this.parent != null) this.parent.trackChange(this, supplier);
        return supplier.getAsBoolean();
    }

    private boolean trackChange(JConstraintTree child, BooleanSupplier supplier) {
        JConstraintTree val = this.children.get(child);
        if (val == null) {
            supplier.getAsBoolean();
            return false;
        } else {
            this.children.remove(child);
            if (supplier.getAsBoolean()) {
                JConstraintTree curr = this.children.get(child);
                if (curr == null) {
                    this.children.put(child, child);
                } else {
                    curr.merge(child);
                }
                this.invalidateStatus();
                return true;
            } else {
                this.children.put(child, child);
                return false;
            }
        }
    }

    private void merge(JConstraintTree other) {
        this.metadata.addAll(other.metadata);
    }

    private JConstraintResult.Status status;

    private void calculateStatus() {
        //And all the constraints in this, then && that with all the children which were || together
        JConstraintResult.Status status = this.constraints.values().stream().reduce(JConstraintResult.Status.TRUE, JConstraintResult.Status::and);
        if (!this.children.isEmpty()) {
            status = status.and(this.children.keySet().stream().map(JConstraintTree::status).reduce(JConstraintResult.Status.FALSE, JConstraintResult.Status::or));
        }
        this.status = status;
    }

    private void invalidateStatus() {
        this.status = null;
        if (this.parent != null && this.parent.status().isTruthy()) {
            //If the parent status is true, it could be changed if this was the only true status and has become false
            this.parent.invalidateStatus();
        }
    }

    public JConstraintResult.Status status() {
        if (this.status == null) calculateStatus();
        return this.status;
    }

    public boolean remove(JTypeConstraint constraint) {
        return this.doChange(() -> doRemove(constraint));
    }

    private boolean doRemove(JTypeConstraint constraint) {
        JConstraintResult.Status prev = this.constraints.remove(constraint);
        if (prev != null && prev.isFalsey()) {
            //If a false status was removed, the current and result becomes corrupted
            this.invalidateStatus();
        }
        return prev != null;
    }

    public boolean put(JTypeConstraint constraint, JConstraintResult.Status status) {
        return this.doChange(() -> doPut(constraint, status));
    }

    private boolean doPut(JTypeConstraint constraint, JConstraintResult.Status status) {
        JConstraintResult.Status prev = this.constraints.put(constraint, status);
        if (prev == null || prev.isTruthy()) {
            //Either there was no previous status, or the previous status was true and would not affect the current and result
            this.status = this.status().and(status);
        } else {
            //Previous status was false so the current and result is corrupted
            this.invalidateStatus();
        }
        return prev != status;
    }

    private Map<JTypeConstraint, JConstraintResult.Status> queue = new LinkedHashMap<>();

    public void queue(JTypeConstraint constraint, JConstraintResult.Status status) {
        this.queue.put(constraint, status);
    }

    public boolean integrateQueue() {
        return this.doChange(() -> {
            boolean mod = false;
            for (Map.Entry<JTypeConstraint, JConstraintResult.Status> entry : this.queue.entrySet()) {
                mod |= doPut(entry.getKey(), entry.getValue());
            }
            this.queue.clear();
            return mod;
        });
    }

    public JConstraintTree addAndGetChild(JConstraintTree child) {
        JConstraintTree[] rs = new JConstraintTree[1];
        this.doChange(() -> {
            JConstraintTree newChild = doAddAndGetChild(child);
            rs[0] = newChild;
            return newChild == child;
        });
        return rs[0];
    }

    private JConstraintTree doAddAndGetChild(JConstraintTree child) {
        JConstraintTree curr = this.children.get(child);
        if (curr == null) {
            this.children.put(child, child);
            return child;
        } else {
            curr.merge(child);
            return curr;
        }
    }

    public JConstraintTree or(Consumer<JConstraintTree>... actions) {
        if (this.children.isEmpty()) {
            if (actions.length == 1) {
                actions[0].accept(this);
            } else {
                for (Consumer<JConstraintTree> action : actions) {
                    JConstraintTree child = new JConstraintTree(this);
                    child = this.doAddAndGetChild(child);
                    action.accept(child);
                }
            }
        } else {
            this.children.forEach((tree, val) -> tree.or(actions));
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

    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> andIterator() {
        return new AndIterator(this);
    }

    public Spliterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> andSpliterator() {
        return Spliterators.spliteratorUnknownSize(andIterator(), Spliterator.NONNULL);
    }

    public Stream<Map.Entry<JTypeConstraint, JConstraintResult.Status>> andStream() {
        return StreamSupport.stream(andSpliterator(), false);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        JConstraintTree that = (JConstraintTree) object;
        return Objects.equals(constraints, that.constraints) && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints, children);
    }

    public JConstraintTree parent() {
        return this.parent;
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

    private static class AndIterator implements Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> {
        private JConstraintTree current;
        private Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> currIter;

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
        public Map.Entry<JTypeConstraint, JConstraintResult.Status> next() {
            Map.Entry<JTypeConstraint, JConstraintResult.Status> next = this.currIter.next();
            if (!this.currIter.hasNext()) nextIterator();
            return next;
        }
    }
}