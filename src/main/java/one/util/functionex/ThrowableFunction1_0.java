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
     */
    void apply(T t) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable, ? super T1>} to
     * {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable,T1>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_0}
     * @param <T1> The parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_0<? extends Throwable,T1>}
     */
    @SuppressWarnings("unchecked")
    static <T1> ThrowableFunction1_0<? extends Throwable, T1> narrow(ThrowableFunction1_0<? extends Throwable, ? super T1> f) {
        return (ThrowableFunction1_0<? extends Throwable, T1>) f;
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
     * @param <E> type of parameter of before function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default <E> ThrowableFunction1_0<X, E> compose(ThrowableFunction1_1<X, ? super E, ? extends T> before) {
        Objects.requireNonNull(before);
        return t1 -> {
            apply(before.apply(t1));
        };
    }
//    /**
//     * Returns a composed function that first applies the {@code before} function to
//     * its input, and then applies this function to the result.
//     * If evaluation of either function throws an exception, it is relayed to
//     * the caller of the composed function.
//     *
//     * @param <T1> type of first parameter of before function
//     * @param <T2> type of second parameter of before function
//     * @param before the function to apply before this function is applied
//     * @return a composed function that first applies the {@code before} function and
//     * then applies this function
//     * @throws NullPointerException if before is null
//     */
//    default <T1, T2> ThrowableFunction2_2<X, T1, T2, R1, R2> compose(ThrowableFunction2_0<X, ? super T1, ? super T2> before) {
//        Objects.requireNonNull(before);
//        return (t1, t2) -> {
//            before.apply(t1, t2);
//            return apply();
//        };
//    }

}
