package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.solvers.NoOpTypeSolver;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface TypeSolver {

    TypeSolver NO_OP = new NoOpTypeSolver();

    boolean supports(TypeBound bound);

    TypeSolver bind(TypeBound bound);

    void reset();

    default TypeSolver bind(TypeBound... bounds) {
        for (TypeBound bound : bounds) {
            this.bind(bound);
        }
        return this;
    }

    Result solve(TypeSystem system);

    final class Result {
        private boolean success;
        private final Set<TypeBound.Result> bounds;

        public Result(boolean success, Set<TypeBound.Result> bounds) {
            this.success = success;
            this.bounds = Collections.unmodifiableSet(bounds);
        }

        public Result(Set<TypeBound.Result> bounds) {
            this(false, bounds);
            this.success = this.parents().stream().allMatch(TypeBound.Result::satisfied);
        }

        public boolean satisfied(TypeBound bound) {
            return this.all().stream().anyMatch(r -> r.satisfied() && r.bound().equals(bound));
        }

        public VarTypeResolveVisitor varTypeResolver() {
            Map<VarType, Type> cache = new LinkedHashMap<>();
            return new VarTypeResolveVisitor(v -> {
                Optional<Type> res = resolution(v);
                if (res.isPresent()) {
                    cache.put(v, res.get());
                    return true;
                }
                return false;
            }, v -> cache.computeIfAbsent(v, k -> resolution(v).get()));
        }

        public Optional<Type> resolution(VarType var) {
            Set<Type> results = this.all().stream().filter(r -> r.satisfied() && r.bound() instanceof TypeBound.Equal eq &&
                    (eq.left().equals(var) || eq.right().equals(var))).map(r -> (TypeBound.Equal) r.bound())
                    .map(eq -> eq.left().equals(var) ? eq.right() : eq.left())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (results.isEmpty()) {
                return Optional.empty();
            } else if (results.size() == 1) {
                return Optional.of(results.iterator().next());
            } else {
                IntersectionType type = new IntersectionTypeImpl(var.typeSystem());
                type.setChildren(IntersectionType.flatten(results));
                type.setUnmodifiable(true);
                return Optional.of(type);
            }
        }

        public Result and(Result other) {
            Set<TypeBound.Result> bounds = new LinkedHashSet<>();

            bounds.addAll(this.bounds);
            bounds.addAll(other.bounds());

            return new Result(this.success && other.success(), bounds);
        }

        private Set<TypeBound.Result> parents;

        public Set<TypeBound.Result> parents() {
            if (this.parents == null) {
                Set<TypeBound.Result> parents = new LinkedHashSet<>();
                this.bounds.forEach(r -> parentsImpl(r, parents));
                this.parents = Collections.unmodifiableSet(parents);
            }
            return this.parents;
        }

        private void parentsImpl(TypeBound.Result result, Set<TypeBound.Result> building) {
            if (result.parents().isEmpty()) {
                building.add(result);
            } else {
                result.parents().forEach(r -> parentsImpl(r, building));
            }
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
                this.parents().forEach(r -> allResultsImpl(r, all));
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
                this.satisfied = this.parents().stream()
                        .filter(TypeBound.Result::satisfied)
                        .collect(Collectors.toCollection(() -> Collections.unmodifiableSet(new LinkedHashSet<>())));
            }
            return this.satisfied;
        }

        private Set<TypeBound.Result> unsatisfied;

        public Set<TypeBound.Result> unsatisfied() {
            if (this.unsatisfied == null) {
                this.unsatisfied = this.parents().stream()
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

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean useSimpleName) {
            StringBuilder sb = new StringBuilder();
            Set<TypeBound.Result> originators = this.parents();
            sb.append("\n")
                    .append("== Results: ").append(originators.size()).append(" ==\n");

            Iterator<TypeBound.Result> iter = originators.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next().toString(useSimpleName));
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
                    Objects.equals(this.parents(), that.parents());
        }

        @Override
        public int hashCode() {
            return Objects.hash(success, this.parents());
        }

    }

}
