package honeyroasted.jype.test;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public abstract class TypeTest {
    protected TypeSystem typeSystem;

    @BeforeEach
    public void setupTypeSystem() {
        this.typeSystem = new TypeSystem();
    }

    protected void assertAssignable(TypeConcrete from, TypeConcrete to) {
        Assertions.assertTrue(this.typeSystem.isAssignableTo(from, to),
                "Expected " + from.toReadable(TypeString.Context.CONCRETE) + " to be assignable to " +
                to.toReadable(TypeString.Context.CONCRETE) + ", but it wasn't");
    }

    protected void assertUnassignable(TypeConcrete from, TypeConcrete to) {
        Assertions.assertFalse(this.typeSystem.isAssignableTo(from, to),
                "Expected " + from.toReadable(TypeString.Context.CONCRETE) + " to NOT be assignable to " +
                        to.toReadable(TypeString.Context.CONCRETE) + ", but it was");
    }

    protected void assignabilityMatrix(TypeConcrete[] types, boolean[][] results) {
        for (int i = 0; i < types.length; i++) {
            for (int j = 0; j < types.length; j++) {
                TypeConcrete a = types[i];
                TypeConcrete b = types[j];

                if (results[i][j]) {
                    assertAssignable(a, b);
                } else {
                    assertUnassignable(a, b);
                }
            }
        }
    }

}
