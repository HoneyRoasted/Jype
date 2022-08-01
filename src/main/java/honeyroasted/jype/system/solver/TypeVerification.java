package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record TypeVerification(Kind kind, TypeConstraint constraint, List<TypeVerification> children, boolean success) {

    public static TypeVerification success(Kind kind, TypeConstraint constraint) {
        return new TypeVerification(kind, constraint, Collections.emptyList(), true);
    }

    public static TypeVerification failure(Kind kind, TypeConstraint constraint) {
        return new TypeVerification(kind, constraint, new ArrayList<>(), false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Kind {
        NONE,
        OR,
        AND,
        EQUAL,
        SUBTYPE
    }

    public static class Builder {
        private Kind kind;
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

        public Builder kind(Kind kind) {
            this.kind = kind;
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
            return new TypeVerification(this.kind, this.constraint, List.copyOf(this.children), this.success);
        }

        public boolean isSuccessful() {
            return this.success;
        }
    }

}
