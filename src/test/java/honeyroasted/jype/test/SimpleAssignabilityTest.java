package honeyroasted.jype.test;

import honeyroasted.jype.TypeConcrete;
import org.junit.jupiter.api.Test;

public class SimpleAssignabilityTest extends TypeTest {

    @Test
    public void testPrimitiveAssignability() {
        assignabilityMatrix(new TypeConcrete[]{
                        this.typeSystem.BYTE,
                        this.typeSystem.INT,
                        this.typeSystem.DOUBLE,
                        this.typeSystem.LONG
                },
                new boolean[][]{
                        {true, true, true, true},
                        {false, true, true, true},
                        {false, false, true, false},
                        {false, false, true, true}
                });
    }

    @Test
    public void testObjectAssignability() {
        TypeConcrete string = this.typeSystem.of(String.class).get();
        TypeConcrete charsequence = this.typeSystem.of(CharSequence.class).get();
        TypeConcrete object = this.typeSystem.OBJECT;

        assertAssignable(string, charsequence);
        assertAssignable(charsequence, object);

        assertUnassignable(charsequence, string);
        assertUnassignable(object, string);
    }

}
