package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with one argument and no returning values.
 *
 * @param <X> the exception which may throw
 * @param <T> argument 1 of the function
 */
@FunctionalInterface
public interface ThrowableFunction1_0<X extends Throwable, T> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws X Exception that function may throw
     */
    void apply(T t) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable, ? super T1>} to
     * {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable,T1>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_0}
     * @param <X>  Exception that f may declare to throw
     * @param <T> The parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable,T1>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T> ThrowableFunction1_0<X, T> narrow(ThrowableFunction1_0<X, ? super T> f) {
        return (ThrowableFunction1_0<X, T>) f;
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
    default ThrowableFunction1_0<X, T> andRun(ThrowableFunction0_0<X> after) {
        Objects.requireNonNull(after);
        return t -> {
            apply(t);
            after.apply();
        };
    }
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of the result of the {@code after} function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction1_1<X, T, V> andSupply(ThrowableFunction0_1<X, V> after) {
        Objects.requireNonNull(after);
        return t -> {
            apply(t);
            return after.apply();
        };
    }
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V1> the type of key of the result entry
     * @param <V2> the type of value of the result entry
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V1, V2> ThrowableFunction1_2<X, T, V1, V2> andSupplyEntry(ThrowableFunction0_2<X, V1, V2> after) {
        Objects.requireNonNull(after);
        return t -> {
            apply(t);
            return after.apply();
        };
    }

    /**
     * Returns a composed function that first applies the {@code before} function to
     * its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default ThrowableFunction0_0<X> compose(ThrowableFunction0_1<X, ? extends T> before) {
        Objects.requireNonNull(before);
        return () -> {
            apply(before.apply());
        };
    }
    /**
     * Returns a composed function that first applies the {@code before} function to
     * its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <I> type of parameter of before function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default <I> ThrowableFunction1_0<X, I> compose(ThrowableFunction1_1<X, ? super I, ? extends T> before) {
        Objects.requireNonNull(before);
        return i1 -> {
            apply(before.apply(i1));
        };
    }
    /**
     * Returns a composed function that first applies the {@code before} function to
     * its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <I1> type of first parameter of before function
     * @param <I2> type of second parameter of before function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default <I1, I2> ThrowableFunction2_0<X, I1, I2> compose(ThrowableFunction2_1<X, ? super I1, ? super I2, ? extends T> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> apply(before.apply(i1, i2));
    }

}
