package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with one argument and returning one value.
 *
 * @param <X> the exception which may throw
 * @param <T1> argument 1 of the function
 * @param <R1> return type 1 of the function
 */
@FunctionalInterface
public interface ThrowableFunction1_1<X extends Throwable, T1, R1> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given argument.
     *
     * @param t1 the function argument
     * @return the function result
     */
    R1 apply(T1 t1) throws X;


    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable, ? super T1, ? extends R1>} to
     * {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable,T1,R1>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_1}
     * @param <T1> The parameter type
     * @param <R1> The return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable,T1,R1>}
     */
    @SuppressWarnings("unchecked")
    static <T1, R1> ThrowableFunction1_1<? extends Throwable, T1, R1> narrow(ThrowableFunction1_1<? extends Throwable, ? super T1, ? extends R1> f) {
        return (ThrowableFunction1_1<? extends Throwable, T1, R1>) f;
    }

    /**
     * Returns the identity one.util.functionex.ThrowableFunction1_1, i.e. the function that returns its input.
     *
     * @param <T> argument type (and return type) of the identity function
     * @return the identity one.util.functionex.ThrowableFunction1_1
     */
    static <X extends Throwable, T> ThrowableFunction1_1<X, T, T> identity() {
        return t -> t;
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction1_1<X, T1, V> andThen(ThrowableFunction1_1<X, ? super R1, ? extends V> after) {
        Objects.requireNonNull(after);
        return t1 -> after.apply(apply(t1));
    }
}
