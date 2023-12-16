package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with no arguments and no returning values.
 *
 * @param <X> the exception which may throw
 */
@FunctionalInterface
public interface ThrowableFunction0_0<X extends Throwable> extends Serializable {

    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function.
     * @throws X Exception that function may throw
     */
    void apply() throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction0_0<? extends Throwable>} to {@code one.util.functionex.ThrowableFunction0_0<? extends Throwable>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction0_0}
     * @param <X>  Exception that f may declare to throw
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction0_0<? extends Throwable>}
     */
    static <X extends Throwable> ThrowableFunction0_0<X> narrow(ThrowableFunction0_0<X> f) {
        return f;
    }

    /**
     * Returns a composed function that first runs this function, and then runs
     * the {@code after} runnable.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the runnable to apply after this
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default ThrowableFunction0_0<X> andRun(ThrowableFunction0_0<X> after) {
        Objects.requireNonNull(after);
        return () -> {
            apply();
            after.apply();
        };
    }
    /**
     * Returns a composed function that returns a supplied value by first applying this
     * runnable, and then applies the {@code after} function.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function returning
     *              a new value of type {@code V}
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @param <V> Type of parameter of the final result value supplied
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction0_1<X, V> andSupply(ThrowableFunction0_1<X, ? extends V> after) {
        Objects.requireNonNull(after);
        return () -> {
            apply();
            return after.apply();
        };
    }
    /**
     * Returns a composed function that returns a supplied entry by first applying this
     * function, and then applies the {@code after} function.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @param <V1> Type of key of the returned entry
     * @param <V2> Type of value of the returned entry
     * @throws NullPointerException if after is null
     */
    @SuppressWarnings("unchecked")
    default <V1, V2> ThrowableFunction0_2<X, V1, V2> andSupplyEntry(ThrowableFunction0_2<X, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return () -> {
            apply();
            return (Tuple<V1, V2>) after.apply();
        };
    }

    /**
     * Returns a composed function that first applies the {@code before} function,
     * and then applies this function.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default ThrowableFunction0_0<X> compose(ThrowableFunction0_0<X> before) {
        Objects.requireNonNull(before);
        return () -> {
            before.apply();
            apply();
        };
    }
    /**
     * Returns a composed function that first applies the {@code before} function with
     * one parameter, and then applies this function.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @param <I> Type of parameter that take the before function
     * @throws NullPointerException if before is null
     */
    default <I> ThrowableFunction1_0<X, I> compose(ThrowableFunction1_0<X, ? super I> before) {
        Objects.requireNonNull(before);
        return (i1) -> {
            before.apply(i1);
            apply();
        };
    }
    /**
     * Returns a composed function that first applies the {@code before} function with two
     * parameters, and then applies this function.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @param <I1> Type of first parameter that take the before function
     * @param <I2> Type of second parameter that take the before function
     * @throws NullPointerException if before is null
     */
    default <I1, I2> ThrowableFunction2_0<X, I1, I2> compose(ThrowableFunction2_0<X, ? super I1, ? super I2> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> {
            before.apply(i1, i2);
            apply();
        };
    }

}
