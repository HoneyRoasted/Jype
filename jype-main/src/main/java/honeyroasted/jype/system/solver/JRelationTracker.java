package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;

public interface JRelationTracker {
    int DEFAULT_MAX_DEPTH = 64;

    default int maxDepth() {
        return DEFAULT_MAX_DEPTH;
    }

    void enter(Constraint judgment);

    void rule(String rule);

    void exit(boolean result);

    static int maxDepth(JRelationTracker tracker) {
        return tracker == null ? DEFAULT_MAX_DEPTH : tracker.maxDepth();
    }

    static JRelationTracker depthLimit(int maxDepth) {
        return new JRelationTracker() {
            @Override
            public int maxDepth() {
                return maxDepth;
            }

            @Override
            public void enter(Constraint judgment) {
            }

            @Override
            public void rule(String rule) {
            }

            @Override
            public void exit(boolean result) {
            }
        };
    }
}
