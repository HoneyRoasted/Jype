package honeyroasted.jype.system.solver;

public record JSolverConfig(boolean relationTrees, boolean failedAttempts, int maxRelationDepth) {
    public static final JSolverConfig FAST = new JSolverConfig(false, false, JRelationTracker.DEFAULT_MAX_DEPTH);
    public static final JSolverConfig PROOFS = new JSolverConfig(true, false, JRelationTracker.DEFAULT_MAX_DEPTH);
    public static final JSolverConfig EXPLAINED = new JSolverConfig(true, true, JRelationTracker.DEFAULT_MAX_DEPTH);

    public JSolverConfig withRelationTrees(boolean relationTrees) {
        return new JSolverConfig(relationTrees, this.failedAttempts, this.maxRelationDepth);
    }

    public JSolverConfig withFailedAttempts(boolean failedAttempts) {
        return new JSolverConfig(this.relationTrees, failedAttempts, this.maxRelationDepth);
    }

    public JSolverConfig withMaxRelationDepth(int maxRelationDepth) {
        return new JSolverConfig(this.relationTrees, this.failedAttempts, maxRelationDepth);
    }

    public JRelationTracker untrackedLimit() {
        return this.maxRelationDepth == JRelationTracker.DEFAULT_MAX_DEPTH ? null : JRelationTracker.depthLimit(this.maxRelationDepth);
    }
}
