import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.TypeToken;

import java.util.List;
import java.util.Map;

public class Test {

    public static <K> void main(String[] args) {
        TypeSystem system = new TypeSystem();

        TypeConcrete type = system.token(new TypeToken<List<? extends Map<String, List<K>>>>() {});
        System.out.println(type.toSource(TypeString.Context.CONCRETE));
    }

}
