package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JConstraintTreeTracker implements JRelationTracker {
    private final int maxDepth;
    private final Deque<Node> stack = new ArrayDeque<>();
    private Node root;

    public JConstraintTreeTracker(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public int maxDepth() {
        return this.maxDepth;
    }

    @Override
    public void enter(Constraint judgment) {
        Node node = new Node(judgment);
        Node parent = this.stack.peek();
        if (parent == null) {
            if (this.root == null) {
                this.root = node;
            }
        } else {
            parent.children.add(node);
        }
        this.stack.push(node);
    }

    @Override
    public void rule(String rule) {
        Node top = this.stack.peek();
        if (top != null && top.rule == null) {
            top.rule = rule;
        }
    }

    @Override
    public void exit(boolean result) {
        this.stack.pop().result = result;
    }

    public void apply(ConstraintBranch branch, Constraint asked, boolean result, boolean failedAttempts) {
        PropertySet askedMeta = new PropertySet();
        if (this.root != null && this.root.rule != null) {
            askedMeta.attach(new JTypeContext.RelationRule(this.root.rule));
        }
        branch.reduce(asked, Constraint.Status.known(result), askedMeta);
        if (this.root == null) {
            return;
        }

        Map<Constraint, Constraint.Status> premises = new LinkedHashMap<>();
        Map<Constraint, PropertySet> premiseMeta = new LinkedHashMap<>();
        List<ConstraintBranch.Snapshot> failed = new ArrayList<>();
        this.collect(this.root, asked, premises, premiseMeta, new ArrayList<>(), failed, failedAttempts);

        List<ConstraintBranch.Snapshot> snapshots = new ArrayList<>();
        if (!premises.isEmpty() || !failed.isEmpty()) {
            snapshots.add(new ConstraintBranch.Snapshot(new PropertySet(), premises, premiseMeta));
        }
        snapshots.addAll(failed);
        branch.divergeBranches(snapshots);
    }

    private void collect(Node node, Constraint judgment, Map<Constraint, Constraint.Status> premises, Map<Constraint, PropertySet> premiseMeta, List<Node> path, List<ConstraintBranch.Snapshot> failed, boolean failedAttempts) {
        for (Node child : node.children) {
            if (node.result && !child.result) {
                if (failedAttempts) {
                    Map<Constraint, Constraint.Status> attempt = new LinkedHashMap<>();
                    Map<Constraint, PropertySet> attemptMeta = new LinkedHashMap<>();
                    Constraint parent = judgment;
                    for (Node ancestor : path) {
                        put(attempt, attemptMeta, ancestor, parent);
                        parent = ancestor.judgment;
                    }
                    putAll(attempt, attemptMeta, child, parent);
                    failed.add(new ConstraintBranch.Snapshot(new PropertySet(), attempt, attemptMeta));
                }
                continue;
            }
            put(premises, premiseMeta, child, judgment);
            List<Node> childPath = new ArrayList<>(path);
            childPath.add(child);
            this.collect(child, child.judgment, premises, premiseMeta, childPath, failed, failedAttempts);
        }
    }

    private static void putAll(Map<Constraint, Constraint.Status> constraints, Map<Constraint, PropertySet> meta, Node node, Constraint parent) {
        put(constraints, meta, node, parent);
        for (Node child : node.children) {
            putAll(constraints, meta, child, node.judgment);
        }
    }

    private static void put(Map<Constraint, Constraint.Status> constraints, Map<Constraint, PropertySet> meta, Node node, Constraint parent) {
        constraints.putIfAbsent(node.judgment, Constraint.Status.known(node.result));
        PropertySet md = meta.computeIfAbsent(node.judgment, k -> new PropertySet());
        md.attach(new JTypeContext.PremiseOf(parent));
        if (node.rule != null) {
            md.attach(new JTypeContext.RelationRule(node.rule));
        }
    }

    private static final class Node {
        private final Constraint judgment;
        private final List<Node> children = new ArrayList<>();
        private String rule;
        private boolean result;

        private Node(Constraint judgment) {
            this.judgment = judgment;
        }
    }
}
