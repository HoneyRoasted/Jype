package honeyroasted.jype;

public class Test {

    public static class Foo {
        public static String bingo;
        public static void hi() {};
    }

    public static class Bar extends Foo {

    }

    public static void main(String[] args) {
        String k = Bar.bingo;
        Bar.hi();
    }

}
