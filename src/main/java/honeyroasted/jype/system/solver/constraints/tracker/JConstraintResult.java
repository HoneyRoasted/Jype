package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JConstraintResult {
    private Status status;
    private Constraint constraint;
    private Operator operator;
    private List<JConstraintResult> children;

    public JConstraintResult(Status status, Constraint constraint, Operator operator, List<JConstraintResult> children) {
        this.status = status;
        this.constraint = constraint;
        this.operator = operator;
        this.children = children;
    }

    public Status status() {
        return this.status;
    }

    public boolean success() {
        return this.status.isTrue();
    }

    public Constraint constraint() {
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
            building.add("Operation: " + this.operator);
            building.add("Children: " + this.children.size());

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
        SET, AND, OR
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
