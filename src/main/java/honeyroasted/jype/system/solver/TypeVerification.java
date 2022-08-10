package honeyroasted.jype.system.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record TypeVerification(TypeConstraint constraint, List<TypeVerification> children, boolean success) {

    public static TypeVerification success(TypeConstraint constraint) {
        return new TypeVerification(constraint, Collections.emptyList(), true);
    }

    public static TypeVerification failure(TypeConstraint constraint) {
        return new TypeVerification(constraint, new ArrayList<>(), false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String buildMessage() {
        StringBuilder sb = new StringBuilder();
        buildMessage(sb, 0);
        return sb.toString();
    }

    private void buildMessage(StringBuilder sb, int indent) {
        sb.append("    ".repeat(indent))
                .append(this.success ? "Succeeded" : "Failed")
                .append(": ")
                .append(this.constraint);

        if (!this.children.isEmpty()) {
            sb.append(", Caused by:").append(System.lineSeparator());
            this.children.forEach(t -> t.buildMessage(sb, indent + 1));
        } else {
            sb.append(System.lineSeparator());
        }
    }

    public static class Builder {
        private TypeConstraint constraint;
        private List<TypeVerification> children = new ArrayList<>();
        private boolean success = true;


        public Builder failure() {
            this.success = false;
            return this;
        }

        public Builder success() {
            this.success = true;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder constraint(TypeConstraint constraint) {
            this.constraint = constraint;
            return this;
        }

        public Builder children(TypeVerification... children) {
            Collections.addAll(this.children, children);
            return this;
        }

        public Builder children(Collection<TypeVerification> children) {
            this.children.addAll(children);
            return this;
        }

        public Builder and() {
            return this.success(this.children.stream().allMatch(TypeVerification::success));
        }

        public Builder or() {
            return this.success(this.children.stream().anyMatch(TypeVerification::success));
        }

        public TypeVerification build() {
            return new TypeVerification(this.constraint, List.copyOf(this.children), this.success);
        }

        public boolean isSuccessful() {
            return this.success;
        }
    }

}
