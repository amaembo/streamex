package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
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
     * @throws X Exception that function may throw
     */
    void apply(T1 t1, T2 t2) throws X;


    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable, ? super T1, ? super T2>} to
     * {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable,T1,T2>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction2_0}
     * @param <X>  Exception that f may declare to throw
     * @param <T1> The first parameter type
     * @param <T2> The second parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction2_0<? extends Throwable,T1,T2>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T1, T2> ThrowableFunction2_0<X, T1, T2> narrow(ThrowableFunction2_0<X, ? super T1, ? super T2> f) {
        return (ThrowableFunction2_0<X, T1, T2>) f;
    }

    /**
     * Applies this function to the given arguments wrapped in a Tuple of 2 values.
     *
     * @param value tuple of 2 function argument
     * @throws X Exception that function may throw
     */
    default void apply(Map.Entry<T1, T2> value) throws X {
        apply(value.getKey(), value.getValue());
    }

    /**
     * Returns a composed function that first applies this function to
     * its inputs, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default ThrowableFunction2_0<X, T1, T2> andRun(ThrowableFunction0_0<X> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            apply(t1, t2);
            after.apply();
        };
    }
    /**
     * Returns a composed function that first applies this function to
     * its inputs, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of the result of the {@code after} function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction2_1<X, T1, T2, V> andSupply(ThrowableFunction0_1<X, V> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            apply(t1, t2);
            return after.apply();
        };
    }
    /**
     * Returns a composed function that first applies this function to
     * its inputs, and then applies the {@code after} function to the result.
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
    default <V1, V2> ThrowableFunction2_2<X, T1, T2, V1, V2> andSupplyEntry(ThrowableFunction0_2<X, V1, V2> after) {
        Objects.requireNonNull(after);
        return (t1, t2) -> {
            apply(t1, t2);
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
    default ThrowableFunction0_0<X> compose(ThrowableFunction0_2<X, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return () -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply();
            apply(tuple.t1(), tuple.t2());
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
    default <I> ThrowableFunction1_0<X, I> compose(ThrowableFunction1_2<X, ? super I, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return t -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply(t);
            apply(tuple.t1(), tuple.t2());
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
    default <I1, I2> ThrowableFunction2_0<X, I1, I2> compose(ThrowableFunction2_2<X, ? super I1, ? super I2, ? extends T1, ? extends T2> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> {
            Tuple<? extends T1, ? extends T2> tuple = before.apply(i1, i2);
            apply(tuple.t1(), tuple.t2());
        };
    }

}
