package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JConstraintResult {
    private boolean success;
    private Constraint constraint;
    private Operator operator;
    private List<JConstraintResult> children;

    public JConstraintResult(boolean success, Constraint constraint, Operator operator, List<JConstraintResult> children) {
        this.success = success;
        this.constraint = constraint;
        this.operator = operator;
        this.children = children;
    }

    public boolean success() {
        return this.success;
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
        building.add("Success: " + this.success);

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
}
