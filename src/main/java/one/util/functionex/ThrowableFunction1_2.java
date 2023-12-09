package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function with one argument and returning two values.
 *
 * @param <X> the exception which may throw
 * @param <T1> argument 1 of the function
 * @param <R1> return type 1 of the function
 * @param <R2> return type 2 of the function
 */
@FunctionalInterface
public interface ThrowableFunction1_2<X extends Throwable, T1, R1, R2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given argument and returns a Tuple of 2 values.
     *
     * @param t1 the function argument
     * @return a @{@link Map.Entry} of 2 values
     */
    Map.Entry<R1, R2> apply(T1 t1) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable, ? super T1, ? extends R1, ? extends R2>} to
     * {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable,T1,R1,R2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_2}
     * @param <T1> The parameter type
     * @param <R1> The first return type
     * @param <R2> The second return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable,T1,R1,R2>}
     */
    @SuppressWarnings("unchecked")
    static <T1, R1, R2> ThrowableFunction1_2<? extends Throwable, T1, R1, R2> narrow(ThrowableFunction1_2<? extends Throwable, ? super T1, ? extends R1, ? extends R2> f) {
        return (ThrowableFunction1_2<? extends Throwable, T1, R1, R2>) f;
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V1> the type of the first output of the {@code after} function
     * @param <V2> the type of the second output of the {@code after} function
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    @SuppressWarnings("unchecked")
    default <V1, V2> ThrowableFunction1_2<X, T1, V1, V2> andThen(ThrowableFunction2_2<X, ? super R1, ? super R2, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return t1 -> {
            Map.Entry<R1, R2> value = apply(t1);
            return (Map.Entry<V1, V2>) after.apply(value.getKey(), value.getValue());
        };
    }

}
