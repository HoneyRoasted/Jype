package honeyroasted.jype.type;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public enum Access {
    PRIVATE() {
        @Override
        public boolean isMarked(int modifiers) {
            return Modifier.isPrivate(modifiers);
        }
    },
    PACKAGE_PROTECTED(PRIVATE) {
        @Override
        public boolean isMarked(int modifiers) {
            return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers);
        }
    },
    PROTECTED(PACKAGE_PROTECTED, PRIVATE) {
        @Override
        public boolean isMarked(int modifiers) {
            return Modifier.isProtected(modifiers);
        }
    },
    PUBLIC(PROTECTED, PACKAGE_PROTECTED, PRIVATE) {
        @Override
        public boolean isMarked(int modifiers) {
            return Modifier.isPublic(modifiers);
        }
    };

    private Access[] children;

    Access(Access... children) {
        this.children = children;
    }

    public abstract boolean isMarked(int modifiers);

    public boolean isMoreRestrictiveThan(Access other) {
        return other != this && other.canAccess(this);
    }

    public boolean canAccess(Access other) {
        return this == other || Arrays.stream(children).anyMatch(ac -> ac == other);
    }

    public boolean canAccess(int modifiers) {
        return this.isMarked(modifiers) ||
                Arrays.stream(this.children).anyMatch(ac -> ac.isMarked(modifiers));
    }

    public static Access fromFlags(int modifiers) {
        for (Access ac : Access.values()) {
            if (ac.isMarked(modifiers)) {
                return ac;
            }
        }
        return Access.PACKAGE_PROTECTED;
    }

}
