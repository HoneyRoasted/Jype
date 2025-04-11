package honeyroasted.jype.basic;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;

public class FieldResolveTest {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = system.tryResolve(String.class);

        string.declaredFields().forEach(j -> System.out.println(j.simpleName()));
    }

}
