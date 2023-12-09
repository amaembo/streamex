package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with two arguments and no returning values.
 *
 * @param <X> the exception which may throw
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 */
@FunctionalInterface
public interface ThrowableFunction2_0<X extends Throwable, T1, T2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Performs this operation on the given arguments.
     *
     * @param t1 first input argument
     * @param t2 second input argument
     */
    void apply(T1 t1, T2 t2) throws X;


    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable, ? super T1, ? super T2>} to
     * {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable,T1,T2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction2_0}
     * @param <T1> The first parameter type
     * @param <T2> The second parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable,T1,T2>}
     */
    @SuppressWarnings("unchecked")
    static <T1, T2> ThrowableFunction2_0<? extends Throwable, T1, T2> narrow(ThrowableFunction2_0<? extends Throwable, ? super T1, ? super T2> f) {
        return (ThrowableFunction2_0<? extends Throwable, T1, T2>) f;
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default ThrowableFunction2_0<X, T1, T2> andThen(ThrowableFunction0_0<X> after) {
        Objects.requireNonNull(after);
        return (T1 t1, T2 t2) -> {
            apply(t1, t2);
            after.apply();
        };
    }
}
