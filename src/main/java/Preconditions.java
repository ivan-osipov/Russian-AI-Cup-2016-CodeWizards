import java.util.Collection;

public class Preconditions {
    public static void notNull(Object o) {
        if(o == null) {
            throw new NullPointerException();
        }
    }

    public static void isNotEmpty(Collection<?> c) {
        if(c == null) {
            throw new IllegalStateException();
        }
    }

}
