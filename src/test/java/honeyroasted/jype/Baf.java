package honeyroasted.jype;

import java.io.IOException;

public class Baf<K> extends Foo {

    public static void test() throws IOException {
        throw new IOException("Yay");
    }

    public class Borp extends Foo<String>.Bar<String> {

    }

}
