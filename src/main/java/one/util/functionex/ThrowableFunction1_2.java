package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function with one argument and returning two values.
 *
 * @param <X> the exception which may throw
 * @param <T> argument 1 of the function
 * @param <R1> return type 1 of the function
 * @param <R2> return type 2 of the function
 */
@FunctionalInterface
public interface ThrowableFunction1_2<X extends Throwable, T, R1, R2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given argument and returns a Tuple of 2 values.
     *
     * @param t the function argument
     * @return a @{@link Tuple} of 2 values
     * @throws X Exception that function may throw
     */
    Tuple<R1, R2> apply(T t) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable, ? super T1, ? extends R1, ? extends R2>} to
     * {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable,T1,R1,R2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_2}
     * @param <X>  Exception that f may declare to throw
     * @param <T> The parameter type
     * @param <R1> The first return type
     * @param <R2> The second return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_2<? extends Throwable,T1,R1,R2>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T, R1, R2> ThrowableFunction1_2<X, T, R1, R2> narrow(ThrowableFunction1_2<X, ? super T, ? extends R1, ? extends R2> f) {
        return (ThrowableFunction1_2<X, T, R1, R2>) f;
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
    default ThrowableFunction1_0<X, T> andConsume(ThrowableFunction2_0<X, ? super R1, ? super R2> after) {
        Objects.requireNonNull(after);
        return t -> {
            Tuple<R1, R2> tuple = apply(t);
            after.apply(tuple.t1(), tuple.t2());
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
    default <V> ThrowableFunction1_1<X, T, V> andReduce(ThrowableFunction2_1<X, ? super R1, ? super R2, ? extends V> after) {
        Objects.requireNonNull(after);
        return t -> {
            Map.Entry<R1, R2> entry = apply(t);
            return after.apply(entry.getKey(), entry.getValue());
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
    @SuppressWarnings("unchecked")
    default <V1, V2> ThrowableFunction1_2<X, T, V1, V2> andMap(ThrowableFunction2_2<X, ? super R1, ? super R2, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return t -> {
            Map.Entry<R1, R2> entry = apply(t);
            return (Tuple<V1, V2>) after.apply(entry.getKey(), entry.getValue());
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
    default ThrowableFunction0_2<X, R1, R2> compose(ThrowableFunction0_1<X, ? extends T> before) {
        Objects.requireNonNull(before);
        return () -> apply(before.apply());
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
    default <I> ThrowableFunction1_2<X, I, R1, R2> compose(ThrowableFunction1_1<X, ? super I, ? extends T> before) {
        Objects.requireNonNull(before);
        return i1 -> apply(before.apply(i1));
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
    default <I1, I2> ThrowableFunction2_2<X, I1, I2, R1, R2> compose(ThrowableFunction2_1<X, ? super I1, ? super I2, ? extends T> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> apply(before.apply(i1, i2));
    }


}
