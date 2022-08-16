package honeyroasted.jype.test;

import honeyroasted.jype.TypeString;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeOr;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DescriptorTest extends TypeTest {

    @Test
    public void testDescriptor() {
        assertEquals(this.typeSystem.INT.toDescriptor(TypeString.Context.CONCRETE).value(), "I");
        assertEquals(this.typeSystem.NONE.toDescriptor(TypeString.Context.CONCRETE).value(), "V");

        assertEquals(this.typeSystem.OBJECT.toDescriptor(TypeString.Context.CONCRETE).value(), "Ljava/lang/Object;");
    }

    public void testDescriptorFails() {
        assertFalse(new TypeOr(this.typeSystem, Set.of(this.typeSystem.INT_BOX, this.typeSystem.BYTE_BOX))
                .toDescriptor(TypeString.Context.CONCRETE).successful());

        assertFalse(this.typeSystem.NULL.toDescriptor(TypeString.Context.CONCRETE).successful());
    }

}
