package one.util.functionex;

import java.util.Map;
import java.util.Objects;

/**
 * An object that contains 2 elements of any type
 * <br/>
 * This object can be used as an {@link Map.Entry} where :
 * <ul>
 *  <li>the type T1 is the key</li>
 *  <li>the type T2 is the value</li>
 * </ul>
 * @param <T1> type of the first element, type of key when used as an {@link Map.Entry}
 * @param <T2> type of the second element, type of value when used as an {@link Map.Entry}
 */
public class Tuple<T1, T2> implements Map.Entry<T1, T2> {
    private final T1 t1;
    private final T2 t2;

    /**
     * constructs a new Tuple
     * @param t1 first element of type T1
     * @param t2 second element of type T2
     */
    public Tuple(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    /**
     * constructs a new Tuple
     * @param <T1> Type of first element
     * @param <T2> Type of second element
     * @param t1 first element of type T1
     * @param t2 second element of type T2
     * @return a new Tuple
     */
    public static <T1, T2> Tuple<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple<>(t1, t2);
    }

    @Override
    public String toString() {
        return "Tuple{" +
            "t1=" + t1 +
            ", t2=" + t2 +
            '}';
    }

    /**
     * Get first element of the tuple
     * @return the first element
     */
    public T1 t1() {
        return t1;
    }

    /**
     * Get second element of the tuple
     * @return the second element
     */
    public T2 t2() {
        return t2;
    }

    @Override
    public T1 getKey() {
        return t1;
    }

    @Override
    public T2 getValue() {
        return t2;
    }

    @Override
    public T2 setValue(T2 value) {
        throw new UnsupportedOperationException("Tuple is immutable");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple<?, ?> tuple = (Tuple<?, ?>) o;

        if (!Objects.equals(t1, tuple.t1)) return false;
        return Objects.equals(t2, tuple.t2);
    }

    @Override
    public int hashCode() {
        int result = t1 != null ? t1.hashCode() : 0;
        result = 31 * result + (t2 != null ? t2.hashCode() : 0);
        return result;
    }

}
