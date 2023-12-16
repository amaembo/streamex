package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function with no arguments and returning two values.
 *
 * @param <X> the exception which may throw
 * @param <R1> return type 1 of the function
 * @param <R2> return type 2 of the function
 */
@FunctionalInterface
public interface ThrowableFunction0_2<X extends Throwable, R1, R2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Gets 2 results in a Tuple.
     *
     * @return a @{@link Map.Entry} of 2 values
     * @throws X Exception that function may throw
     */
    Tuple<R1, R2> apply() throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction0_2<? extends Throwable, ? extends R1, ? extends R2>} to
     * {@code one.util.functionex.ThrowableFunction0_2<? extends Throwable,R1,R2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction0_2}
     * @param <X>  Exception that f may declare to throw
     * @param <R1> The first return type
     * @param <R2> The second return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction0_2<? extends Throwable,R1,R2>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, R1, R2> ThrowableFunction0_2<X, R1, R2> narrow(ThrowableFunction0_2<X, ? extends R1, ? extends R2> f) {
        return (ThrowableFunction0_2<X, R1, R2>) f;
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
    default ThrowableFunction0_0<X> andConsumes(ThrowableFunction2_0<X, ? super R1, ? super R2> after) {
        Objects.requireNonNull(after);
        return () -> {
            Map.Entry<R1, R2> value = apply();
            after.apply(value.getKey(), value.getValue());
        };
    }
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of the result of the {@code after} function
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction0_1<X, V> andReduce(ThrowableFunction2_1<X, ? super R1, ? super R2, ? extends V> after) {
        Objects.requireNonNull(after);
        return () -> {
            Map.Entry<R1, R2> value = apply();
            return after.apply(value.getKey(), value.getValue());
        };
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
    default <V1, V2> ThrowableFunction0_2<X, V1, V2> andMap(ThrowableFunction2_2<X, ? super R1, ? super R2, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return () -> {
            Tuple<R1, R2> value = apply();
            return (Tuple<V1, V2>) after.apply(value.getKey(), value.getValue());
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
    default ThrowableFunction0_2<X, R1, R2> compose(ThrowableFunction0_0<X> before) {
        Objects.requireNonNull(before);
        return () -> {
            before.apply();
            return apply();
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
    default <I> ThrowableFunction1_2<X, I, R1, R2> compose(ThrowableFunction1_0<X, ? super I> before) {
        Objects.requireNonNull(before);
        return i1 -> {
            before.apply(i1);
            return apply();
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
    default <I1, I2> ThrowableFunction2_2<X, I1, I2, R1, R2> compose(ThrowableFunction2_0<X, ? super I1, ? super I2> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> {
            before.apply(i1, i2);
            return apply();
        };
    }

}
