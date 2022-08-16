package honeyroasted.jype.test;

import honeyroasted.jype.TypeConcrete;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleAssignabilityTest extends TypeTest {

    @Test
    public void testPrimitiveAssignability() {
        assertTrue(this.typeSystem.isAssignableTo(
                this.typeSystem.BYTE,
                this.typeSystem.INT
        ));

        assertTrue(this.typeSystem.isAssignableTo(
                this.typeSystem.INT,
                this.typeSystem.DOUBLE
        ));

        assertFalse(this.typeSystem.isAssignableTo(
                this.typeSystem.DOUBLE,
                this.typeSystem.LONG
        ));
    }

    @Test
    public void testObjectAssignability() {
        TypeConcrete string = this.typeSystem.of(String.class).get();
        TypeConcrete charsequence = this.typeSystem.of(CharSequence.class).get();
        TypeConcrete object = this.typeSystem.OBJECT;

        assertTrue(this.typeSystem.isAssignableTo(string, charsequence));
        assertTrue(this.typeSystem.isAssignableTo(charsequence, object));

        assertFalse(this.typeSystem.isAssignableTo(charsequence, string));
        assertFalse(this.typeSystem.isAssignableTo(object, string));
    }

}
