package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.operations.conversion.TypeConversion;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.GLOBAL;

        TypeConcrete listWild = system.declaration(List.class).get().withArguments(new TypeOut(system, system.OBJECT));
        TypeConcrete listT = system.declaration(List.class).get().withArguments(new TypeParameter(system, "T"));

        System.out.println(new TypeConversion.Capture(listWild).perform().buildMessage());
    }

}
