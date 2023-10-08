package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;

import java.util.*;
import java.util.stream.Collectors;

public interface TypeSolver {

    boolean supports(TypeBound bound);

    TypeSolver bind(TypeBound bound);

    TypeSolver assume(TypeBound bound);

    default TypeSolver bind(TypeBound... bounds) {
        for (TypeBound bound : bounds) {
            this.bind(bound);
        }
        return this;
    }

    default TypeSolver assume(TypeBound... bounds) {
        for (TypeBound bound : bounds) {
            this.assume(bound);
        }
        return this;
    }

    Result solve(TypeSystem system);

    final class Result {
        private final boolean success;
        private final Set<TypeBound.Result> bounds;
        private final Set<TypeBound> insights;
        private final Set<TypeBound> assumptions;

        public Result(boolean success, Set<TypeBound.Result> bounds, Set<TypeBound> insights, Set<TypeBound> assumptions) {
            this.success = success;
            this.bounds =  Collections.unmodifiableSet(bounds);
            this.insights = Collections.unmodifiableSet(insights);
            this.assumptions = assumptions;
        }

        private Set<TypeBound.Result> originators;
        public Set<TypeBound.Result> originators() {
            if (this.originators == null) {
                Set<TypeBound.Result> originators = new LinkedHashSet<>();
                for (TypeBound.Result bound : this.bounds) {
                    TypeBound.Result curr = bound;
                    while (curr.originator() != null) {
                        curr = curr.originator();
                    }
                    originators.add(curr);
                }
                this.originators = Collections.unmodifiableSet(originators);
            }
            return this.originators;
        }

        private Set<TypeBound.Result> leaves;
        public Set<TypeBound.Result> leaves() {
            if (this.leaves == null) {
                Set<TypeBound.Result> leaves = new LinkedHashSet<>();
                this.bounds.forEach(r -> leavesImpl(r, leaves));
                this.leaves = Collections.unmodifiableSet(leaves);
            }
            return this.leaves;
        }

        private void leavesImpl(TypeBound.Result result, Set<TypeBound.Result> building) {
            if (result.children().isEmpty()) {
                building.add(result);
            } else {
                result.children().forEach(r -> leavesImpl(r, building));
            }
        }

        private Set<TypeBound.Result> all;
        public Set<TypeBound.Result> all() {
            if (this.all == null) {
                Set<TypeBound.Result> all = new LinkedHashSet<>();
                this.originators().forEach(r -> allResultsImpl(r, all));
                this.all = Collections.unmodifiableSet(all);
            }
            return this.all;
        }

        private void allResultsImpl(TypeBound.Result result, Set<TypeBound.Result> building) {
            building.add(result);
            result.children().forEach(r -> allResultsImpl(r, building));
        }

        private Set<TypeBound.Result> satisfied;
        public Set<TypeBound.Result> satisfied() {
            if (this.satisfied == null) {
                this.satisfied = this.originators().stream()
                        .filter(TypeBound.Result::satisfied)
                        .collect(Collectors.toCollection(() -> Collections.unmodifiableSet(new LinkedHashSet<>())));
            }
            return this.satisfied;
        }

        private Set<TypeBound.Result> unsatisfied;
        public Set<TypeBound.Result> unsatisfied() {
            if (this.unsatisfied == null) {
                this.unsatisfied = this.originators().stream()
                        .filter(TypeBound.Result::unsatisfied)
                        .collect(Collectors.toCollection(() -> Collections.unmodifiableSet(new LinkedHashSet<>())));
            }
            return this.unsatisfied;
        }

        private Set<TypeBound.Result> allSatisfied;
        public Set<TypeBound.Result> allSatisfied() {
            if (this.allSatisfied == null) {
                this.allSatisfied = this.all().stream()
                        .filter(TypeBound.Result::satisfied)
                        .collect(Collectors.toCollection(() -> Collections.unmodifiableSet(new LinkedHashSet<>())));
            }
            return this.allSatisfied;
        }

        private Set<TypeBound.Result> allUnsatisfied;
        public Set<TypeBound.Result> allUnsatisfied() {
            if (this.allUnsatisfied == null) {
                this.allUnsatisfied = this.all().stream()
                        .filter(TypeBound.Result::unsatisfied)
                        .collect(Collectors.toCollection(() -> Collections.unmodifiableSet(new LinkedHashSet<>())));
            }
            return this.allUnsatisfied;
        }

        public boolean success() {
            return success;
        }

        public Set<TypeBound.Result> bounds() {
            return bounds;
        }

        public Set<TypeBound> insights() {
            return insights;
        }

        public Set<TypeBound> assumptions() {
            return assumptions;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("== Insights: ").append(this.insights.size()).append(" ==\n");
            this.insights.forEach(t -> sb.append(t).append("\n"));

            if (!this.assumptions.isEmpty()) {
                sb.append("\n== Assumptions: ").append(this.assumptions.size()).append(" ==\n");
                this.assumptions.forEach(t -> sb.append(t).append("\n"));
            }

            Set<TypeBound.Result> originators = this.originators();
            sb.append("\n")
                    .append("== Detailed Results: ").append(originators.size()).append(" ==\n");

            Iterator<TypeBound.Result> iter = originators.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next().toString());
                if (iter.hasNext()) {
                    sb.append("\n\n");
                }
            }

            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Result) obj;
            return this.success == that.success &&
                    Objects.equals(this.originators(), that.originators());
        }

        @Override
        public int hashCode() {
            return Objects.hash(success, this.originators());
        }

    }

}
