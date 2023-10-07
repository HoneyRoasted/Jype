package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public interface TypeSolver {

    boolean supports(TypeBound bound);

    TypeSolver bind(TypeBound bound);

    default TypeSolver bind(TypeBound... bounds) {
        for (TypeBound bound : bounds) {
            this.bind(bound);
        }
        return this;
    }

    Result solve(TypeSystem system);

    record Result(boolean success, Set<TypeBound.Result> bounds, Set<TypeBound> insights) {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("== Insights: ").append(this.insights.size()).append(" ==").append("\n");
            for (TypeBound insight : this.insights) {
                sb.append(insight).append("\n");
            }

            Set<TypeBound.Result> originators = this.originators();
            sb.append("\n")
                    .append("== Detailed Results: ").append(originators.size()).append(" ==").append("\n");

            Iterator<TypeBound.Result> iter = originators.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next().toString());
                if (iter.hasNext()) {
                    sb.append("\n\n");
                }
            }

            return sb.toString();
        }

        public Set<TypeBound.Result> originators() {
            Set<TypeBound.Result> originators = new LinkedHashSet<>();
            for (TypeBound.Result bound : this.bounds) {
                TypeBound.Result curr = bound;
                while (curr.originator() != null) {
                    curr = curr.originator();
                }
                originators.add(curr);
            }
            return originators;
        }

        public Set<TypeBound.Result> leaves() {
            Set<TypeBound.Result> leaves = new LinkedHashSet<>();
            this.bounds.forEach(r -> leavesImpl(r, leaves));
            return leaves;
        }

        private void leavesImpl(TypeBound.Result result, Set<TypeBound.Result> building) {
            if (result.children().isEmpty()) {
                building.add(result);
            } else {
                result.children().forEach(r -> leavesImpl(r, building));
            }
        }

        public Set<TypeBound.Result> allResults() {
            Set<TypeBound.Result> all = new LinkedHashSet<>();
            this.originators().forEach(r -> allResultsImpl(r, all));
            return all;
        }

        private void allResultsImpl(TypeBound.Result result, Set<TypeBound.Result> building) {
            building.add(result);
            result.children().forEach(r -> allResultsImpl(r, building));
        }
    }

}
