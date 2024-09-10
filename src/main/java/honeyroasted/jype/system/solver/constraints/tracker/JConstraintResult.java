package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class JConstraintResult {
    private Status status;
    private JTypeConstraint constraint;
    private Operator operator;
    private List<JConstraintResult> children;

    public JConstraintResult(Status status, JTypeConstraint constraint, Operator operator, List<JConstraintResult> children) {
        this.status = status;
        this.constraint = constraint;
        this.operator = operator;
        this.children = children;
    }

    public JConstraintResult simplify() {
        JConstraintResult simplified = doSimplify();
        return simplified != null ? simplified : new JConstraintResult(status, constraint, operator, Collections.emptyList());
    }

    private JConstraintResult doSimplify() {
        if (this.children.size() == 1 && this.constraint.isMetadata()) return this.children.getFirst();
        if (this.children.isEmpty() && this.constraint.isMetadata()) return null;

        return new JConstraintResult(status, constraint, children.size() == 1 ? children.getFirst().operator() : operator,
                children.stream()
                        .flatMap(child -> child.constraint.isMetadata() && !child.children.isEmpty() &&
                                (child.operator == operator() || children.size() == 1) ?
                                child.children.stream() : Stream.of(child))
                        .map(JConstraintResult::doSimplify)
                        .filter(Objects::nonNull)
                        .toList());
    }

    public Status status() {
        return this.status;
    }

    public boolean success() {
        return this.status.isTrue();
    }

    public JTypeConstraint constraint() {
        return this.constraint;
    }

    public Operator operator() {
        return this.operator;
    }

    public List<JConstraintResult> children() {
        return this.children;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean useSimpleName) {
        List<String> building = new ArrayList<>();
        this.toString(building, useSimpleName);
        return String.join("\n", building);
    }

    public void toString(List<String> building, boolean useSimpleName) {
        building.add("Condition: " + (useSimpleName ? this.constraint().simpleName() : this.constraint().toString()));
        building.add("Status: " + this.status);

        if (!this.children.isEmpty()) {
            if (this.children.size() > 1) {
                building.add("Operation: " + this.operator);
                building.add("Children: " + this.children.size());
            } else {
                building.add("Inherits:");
            }

            List<String> children = new ArrayList<>();
            Iterator<JConstraintResult> iterator = this.children().iterator();
            while (iterator.hasNext()) {
                iterator.next().toString(children, useSimpleName);
                if (iterator.hasNext()) {
                    children.add("");
                }
            }

            int maxLen = children.stream().mapToInt(String::length).max().getAsInt();
            String content = "-".repeat(maxLen + 8);
            String top = "+" + content + "+";
            building.add(top);
            for (String c : children) {
                building.add("|    " + c + (" ".repeat(maxLen - c.length() + 4)) + "|");
            }
            building.add(top);
        }
    }

    public enum Operator {
        SET(Status.TRUE), AND(Status.TRUE), OR(Status.FALSE);

        private Status identity;

        Operator(Status identity) {
            this.identity = identity;
        }

        public Status identity() {
            return this.identity;
        }
    }

    public static enum Status {
        TRUE {
            public boolean isTrue() {
                return true;
            }

            public boolean isKnown() {
                return true;
            }

            public Status and(Status other) {
                return other;
            }

            public Status or(Status other) {
                return this;
            }
        },
        ASSUMED {
            public boolean isTrue() {
                return true;
            }

            public boolean isKnown() {
                return false;
            }

            public Status and(Status other) {
                return other == TRUE ? this : other;
            }

            public Status or(Status other) {
                return other == TRUE ? other : this;
            }
        },
        FALSE {
            public boolean isTrue() {
                return false;
            }

            public boolean isKnown() {
                return true;
            }

            public Status and(Status other) {
                return this;
            }

            public Status or(Status other) {
                return other;
            }
        },
        UNKNOWN {
            public boolean isTrue() {
                return false;
            }

            public boolean isKnown() {
                return false;
            }

            public Status and(Status other) {
                return other == FALSE ? other : this;
            }

            public Status or(Status other) {
                return other != TRUE && other != ASSUMED ? this : other;
            }
        };

        public static Status known(boolean value) {
            return value ? TRUE : FALSE;
        }

        public static Status unknown(boolean value) {
            return value ? ASSUMED : UNKNOWN;
        }

        public abstract boolean isTrue();

        public boolean isFalse() {
            return !this.isTrue();
        }

        public abstract boolean isKnown();

        public boolean isUnknown() {
            return !this.isKnown();
        }

        public abstract Status and(Status other);

        public abstract Status or(Status other);
    }
}
