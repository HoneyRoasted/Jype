package honeyroasted.jype.system.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a result of attempting to solve some {@link TypeConstraint}. It permits a tree structure,
 * to provide children {@link TypeVerification} that may be the cause of this {@link TypeVerification}'s failure.
 * Each {@link TypeVerification} has a {@link TypeConstraint} it is associated with, a boolean indicating whether it
 * was sucessfully resolved, and zero or more children.
 *
 * @param constraint The {@link TypeConstraint} associated with this {@link TypeVerification}
 * @param children   The children of this {@link TypeVerification}
 * @param success    Whether this {@link TypeVerification} was successful
 */
public record TypeVerification(TypeConstraint constraint, List<TypeVerification> children, boolean success) {

    /**
     * Creates a new {@link TypeVerification} with the given constraint, zero children, and a successful result value.
     *
     * @param constraint The {@link TypeConstraint} associated with this {@link TypeVerification}
     * @return A new {@link TypeVerification}
     */
    public static TypeVerification success(TypeConstraint constraint) {
        return new TypeVerification(constraint, Collections.emptyList(), true);
    }

    /**
     * Creates a new {@link TypeVerification} with the given constraint, zero children, and a unsuccessful result value.
     *
     * @param constraint The {@link TypeConstraint} associated with this {@link TypeVerification}
     * @return A new {@link TypeVerification}
     */
    public static TypeVerification failure(TypeConstraint constraint) {
        return new TypeVerification(constraint, new ArrayList<>(), false);
    }

    /**
     * Creates a new {@link TypeVerification} with the given constraint, zero children, and the given result value.
     *
     * @param success    Whether this {@link TypeVerification} was successful
     * @param constraint The {@link TypeConstraint} associated with this {@link TypeVerification}
     * @return A new {@link TypeVerification}
     */
    public static TypeVerification of(boolean success, TypeConstraint constraint) {
        return new TypeVerification(constraint, new ArrayList<>(), success);
    }

    /**
     * @return A new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Formats this {@link TypeVerification} as a potentially multi-lined message. It is functionally
     * equivalent to {@code toMessage(true)}
     *
     * @return A formatted {@link String} containing information about this {@link TypeVerification} and its children
     * @see TypeVerification#toMessage(boolean)
     */
    public String toMessage() {
        return toMessage(true);
    }

    /**
     * Formats this {@link TypeVerification} as a potentially multi-lined message, with the success value,
     * the constraint, and its children listed. Each child will be listed on a new line, with an indent. This method
     * is mostly for debugging purposes, and if a full explanation of why a {@link TypeVerification} failed (or
     * succeeded) is needed, the user should inspect the {@link TypeVerification} and its associated {@link TypeConstraint}s
     * themselves. More complex {@link TypeVerification}s have significantly more complicated messages, often with
     * much unneeded information.
     *
     * @param forceChildren true if children {@link TypeVerification}s should be included when their parents are
     *                      successful.
     * @return A formatted {@link String} containing information about this {@link TypeVerification} and its children
     */
    public String toMessage(boolean forceChildren) {
        StringBuilder sb = new StringBuilder();
        buildMessage(sb, 0, forceChildren);
        return sb.toString();
    }

    private void buildMessage(StringBuilder sb, int indent, boolean forceChildren) {
        sb.append("    ".repeat(indent))
                .append(this.success ? "Succeeded" : "Failed")
                .append(": ")
                .append(this.constraint);

        if (!this.children.isEmpty() && (forceChildren || !this.success)) {
            sb.append(", Caused by:").append(System.lineSeparator());
            this.children.forEach(t -> t.buildMessage(sb, indent + 1, forceChildren));
        } else {
            sb.append(System.lineSeparator());
        }
    }

    /**
     * This class is useful for building instances of {@link TypeVerification}s.
     */
    public static class Builder {
        private TypeConstraint constraint;
        private List<TypeVerification> children = new ArrayList<>();
        private boolean success = true;


        /**
         * Marks the building {@link TypeVerification} as a failure. Overrides previous changes to the
         * result value.
         *
         * @return This, for method chaining
         */
        public Builder failure() {
            this.success = false;
            return this;
        }

        /**
         * Marks the building {@link TypeVerification} as a success. Overrides previous changes to the
         * result value.
         *
         * @return This, for method chaining
         */
        public Builder success() {
            this.success = true;
            return this;
        }

        /**
         * Sets the result value of the building {@link TypeVerification} to the given boolean. Overrides previous
         * changes to the result value.
         *
         * @param success Whether the building {@link TypeVerification} was successful
         * @return This, for method chaining
         */
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        /**
         * Sets the {@link TypeConstraint} associated with the building {@link TypeVerification}. Overrides previous
         * changes to the associated {@link TypeConstraint}.
         *
         * @param constraint The {@link TypeConstraint} to associated with the building {@link TypeVerification}
         * @return This, for method chaining
         */
        public Builder constraint(TypeConstraint constraint) {
            this.constraint = constraint;
            return this;
        }

        /**
         * Adds the given children to the children of the building {@link TypeConstraint}. Note that this method
         * <i>does not</i> overwrite previous invocations. Each invocation of this method will simply append the
         * new children to the current list of children.
         *
         * @param children The children to add to the building {@link TypeConstraint}
         * @return This, for method chaining
         */
        public Builder children(TypeVerification... children) {
            Collections.addAll(this.children, children);
            return this;
        }

        /**
         * Adds the given children to the children of the building {@link TypeConstraint}. Note that this method
         * <i>does not</i> overwrite previous invocations. Each invocation of this method will simply append the
         * new children to the current list of children.
         *
         * @param children The children to add to the building {@link TypeConstraint}
         * @return This, for method chaining
         */
        public Builder children(Collection<TypeVerification> children) {
            this.children.addAll(children);
            return this;
        }

        /**
         * Sets the result value of the building {@link TypeVerification} to true if <i>every</i> current child has a true
         * result value, or false otherwise. Overrides previous changes to the result value.
         *
         * @return This, for method chaining
         */
        public Builder and() {
            return this.success(this.children.stream().allMatch(TypeVerification::success));
        }

        /**
         * Sets the result value of the building {@link TypeVerification} to true if <i>any</i> current child has a true
         * result value, or false otherwise. Overrides previous changes to the result value.
         *
         * @return This, for method chaining
         */
        public Builder or() {
            return this.success(this.children.stream().anyMatch(TypeVerification::success));
        }

        /**
         * @return A new {@link TypeVerification} with the settings currently set in this {@link Builder}
         */
        public TypeVerification build() {
            return new TypeVerification(this.constraint, List.copyOf(this.children), this.success);
        }

        /**
         * @return The current result value of the building {@link TypeVerification}
         */
        public boolean isSuccessful() {
            return this.success;
        }
    }

}
