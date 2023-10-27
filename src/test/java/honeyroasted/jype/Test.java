package honeyroasted.jype;

import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws NoSuchMethodException {
        ClassType fooBar = new TypeToken<Foo.Bar>(){}.resolve();
        ClassType bafBorp = new TypeToken<Baf<Integer>.Borp>(){}.resolve();

        Type val = bafBorp.relativeSupertype(fooBar.classReference()).get();
        System.out.println(val);

        ClassType linkMap = new TypeToken<LinkedHashMap>(){}.resolve();
        ClassType map = new TypeToken<Map<String, String>>(){}.resolve();

        System.out.println(linkMap.relativeSupertype(map).get());
    }

}
