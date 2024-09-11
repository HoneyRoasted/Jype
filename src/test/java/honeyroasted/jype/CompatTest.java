package honeyroasted.jype;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintMirrorTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintResultTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintStatusTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTreeTracker;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.List;

public class CompatTest {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;

        JType left = new JTypeToken<ArrayList<String>>(){}.resolve(system);
        JType right = new JTypeToken<Iterable<? extends String>>(){}.resolve(system);

        JConstraintTracker tracker1 = new JConstraintResultTracker();
        JConstraintTracker tracker2 = new JConstraintTreeTracker();
        JConstraintTracker tracker3 = new JConstraintStatusTracker();

        JConstraintTracker tracker = new JConstraintMirrorTracker(tracker1, List.of(tracker2, tracker3));
        system.operations().checkCompatible(left, right, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, tracker);

        List.of(tracker1, tracker2, tracker3).forEach(tr -> {
            System.out.println(tr.result().toString(true));
            System.out.println("##".repeat(400));
            System.out.println("##".repeat(400));
            System.out.println("##".repeat(400));
        });
    }

}
