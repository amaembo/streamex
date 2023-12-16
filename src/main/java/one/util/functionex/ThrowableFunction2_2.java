package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function with two arguments and returning two values.
 *
 * @param <X> the exception which may throw
 * @param <T1> argument 1 of the function
 * @param <T2> argument 2 of the function
 * @param <R1> return type 1 of the function
 * @param <R2> return type 2 of the function
 */

@FunctionalInterface
public interface ThrowableFunction2_2<X extends Throwable, T1, T2, R1, R2> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given arguments and returns a Tuple of 2 values.
     *
     * @param t1 the first function argument
     * @param t2 the second function argument
     * @return a @{@link Tuple} of 2 values
     * @throws X Exception that function may throw
     */
    Tuple<R1, R2> apply(T1 t1, T2 t2) throws X;


    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction2_2<? extends Throwable, ? super T1, ? super T2, ? extends R1, ? extends R2>} to
     * {@code one.util.functionex.ThrowableFunction2_2<? extends Throwable,T1,T2,R1,R2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction2_2}
     * @param <X>  Exception that f may declare to throw
     * @param <T1> The first parameter type
     * @param <T2> The second parameter type
     * @param <R1> The first return type
     * @param <R2> The second return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction2_2<? extends Throwable,T1,T2,R1,R2>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T1, T2, R1, R2> ThrowableFunction2_2<X, T1, T2, R1, R2> narrow(ThrowableFunction2_2<X, ? super T1, ? super T2, ? extends R1, ? extends R2> f) {
        return (ThrowableFunction2_2<X, T1, T2, R1, R2>) f;
    }

    /**
     * Applies this function to the given arguments wrapped in a Tuple of 2 values.
     *
     * @param value tuple of 2 function argument
     * @return a @{@link Tuple} of 2 values
     * @throws X Exception that function may throw
     */
    default Tuple<R1, R2> apply(Map.Entry<T1, T2> value) throws X {
        return apply(value.getKey(), value.getValue());
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
    default ThrowableFunction2_0<X, T1, T2> andConsume(ThrowableFunction2_0<X, ? super R1, ? super R2> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            Tuple<R1, R2> tuple = apply(t1, t2);
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
    default <V> ThrowableFunction2_1<X, T1, T2, V> andReduce(ThrowableFunction2_1<X, ? super R1, ? super R2, ? extends V> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            Tuple<R1, R2> tuple = apply(t1, t2);
            return after.apply(tuple.t1(), tuple.t2());
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
    default <V1, V2> ThrowableFunction2_2<X, T1, T2, V1, V2> andMap(ThrowableFunction2_2<X, ? super R1, ? super R2, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            Tuple<R1, R2> tuple = apply(t1, t2);
            return (Tuple<V1, V2>) after.apply(tuple.t1(), tuple.t2());
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
    default ThrowableFunction0_2<X, R1, R2> compose(ThrowableFunction0_2<X, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return () -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply();
            return apply(tuple.t1(), tuple.t2());
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
    default <I> ThrowableFunction1_2<X, I, R1, R2> compose(ThrowableFunction1_2<X, ? super I, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return i1 -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply(i1);
            return apply(tuple.t1(), tuple.t2());
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
    default <I1, I2> ThrowableFunction2_2<X, I1, I2, R1, R2> compose(ThrowableFunction2_2<X, ? super I1, ? super I2, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply(i1, i2);
            return apply(tuple.t1(), tuple.t2());
        };
    }
}
