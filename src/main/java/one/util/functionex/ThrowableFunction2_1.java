package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with two arguments and returning one value.
 *
 * @param <X> the exception which may throw
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <R> return type 1 of the function
 */

@FunctionalInterface
public interface ThrowableFunction2_1<X extends Throwable, T1, T2, R> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given arguments.
     *
     * @param t1 the first function argument
     * @param t2 the second function argument
     * @return the function result
     */
    R apply(T1 t1, T2 t2) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends R>} to
     * {@code one.util.functionex.ThrowableFunction2_1<? extends Throwable,T1,T2,R>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction2_1}
     * @param <T1> The first parameter type
     * @param <T2> The second parameter type
     * @param <R> The return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction2_1<? extends Throwable,T1,T2,R>}
     */
    @SuppressWarnings("unchecked")
    static <T1, T2, R> ThrowableFunction2_1<? extends Throwable, T1, T2, R> narrow(ThrowableFunction2_1<? extends Throwable, ? super T1, ? super T2, ? extends R> f) {
        return (ThrowableFunction2_1<? extends Throwable, T1, T2, R>) f;
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction2_1<X, T1, T2, V> andThen(ThrowableFunction1_1<X, ? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T1 t1, T2 t2) -> after.apply(apply(t1, t2));
    }
}
