package honeyroasted.jype;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;

public class Test {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;
        System.out.println(system.resolve(JClassLocation.of("not/a/real/ClassLocation")));
    }

}
