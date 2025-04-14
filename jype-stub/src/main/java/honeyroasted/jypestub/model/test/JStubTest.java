package honeyroasted.jypestub.model.test;

import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;

import java.util.ArrayList;
import java.util.List;

public record JStubTest(Solver solver, List<JTestCondition.Wrapper> expect, List<JTestConstraint.Wrapper> constraints) {

    public enum Solver {
        INFERENCE, COMPATIBILITY, NO_OP
    }

    public record Result(boolean result, ConstraintTree tree, List<JTestCondition.Result> results) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, "", "");
            return sb.toString();
        }

        public void toString(StringBuilder sb, String indent, String prefix) {
            sb.append(indent).append(prefix).append(result).append("\n");
            this.results.forEach(sub -> sb.append(indent).append("    ").append(sub.result()).append(": ").append(sub.message()).append("\n"));
        }
    }

    public Result test(JTypeSystem system) {
        ConstraintSolver solver;

        if (this.solver == Solver.COMPATIBILITY) {
            solver = system.operations().compatibilitySolver();
        } else if (this.solver == Solver.NO_OP) {
            solver = system.operations().noOpSolver();
        } else {
            solver = system.operations().inferenceSolver();
        }

        this.constraints.forEach(cons -> cons.resolve(system).forEach(solver::bind));

        ConstraintTree solved = solver.solve();

        boolean success = true;
        List<JTestCondition.Result> results = new ArrayList<>();

        for (JTestCondition.Wrapper test : this.expect) {
            JTestCondition.Result condRes = test.test(system, solved);
            success &= condRes.result();
            results.add(condRes);
        }

        return new Result(success, solved, results);
    }

}
