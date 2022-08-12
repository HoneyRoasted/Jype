package honeyroasted.jype.test;

import honeyroasted.jype.system.TypeSystem;
import org.junit.jupiter.api.BeforeEach;

public abstract class TypeTest {
    protected TypeSystem typeSystem;

    @BeforeEach
    public void setupTypeSystem() {
        this.typeSystem = new TypeSystem();
    }

}
