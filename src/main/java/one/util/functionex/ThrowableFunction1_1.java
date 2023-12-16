package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a function with one argument and returning one value.
 *
 * @param <X> the exception which may throw
 * @param <T> argument 1 of the function
 * @param <R> return type 1 of the function
 */
@FunctionalInterface
public interface ThrowableFunction1_1<X extends Throwable, T, R> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws X Exception that function may throw
     */
    R apply(T t) throws X;


    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable, ? super T1, ? extends R1>} to
     * {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable,T1,R1>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction1_1}
     * @param <X>  Exception that f may declare to throw
     * @param <T> The parameter type
     * @param <R> The return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction1_1<? extends Throwable,T1,R1>}
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T, R> ThrowableFunction1_1<X, T, R> narrow(ThrowableFunction1_1<X, ? super T, ? extends R> f) {
        return (ThrowableFunction1_1<X, T, R>) f;
    }

    /**
     * Returns the identity one.util.functionex.ThrowableFunction1_1, i.e. the function that returns its input.
     *
     * @param <X> Exception type function returned may throw
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
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default ThrowableFunction1_0<X, T> andConsume(ThrowableFunction1_0<X, ? super R> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
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
    default <V> ThrowableFunction1_1<X, T, V> andMap(ThrowableFunction1_1<X, ? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
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
    default <V1, V2> ThrowableFunction1_2<X, T, V1, V2> andMapToEntry(ThrowableFunction1_2<X, ? super R, V1, V2> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
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
    default ThrowableFunction0_1<X, R> compose(ThrowableFunction0_1<X, ? extends T> before) {
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
    default <I> ThrowableFunction1_1<X, I, R> compose(ThrowableFunction1_1<X, ? super I, ? extends T> before) {
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
    default <I1, I2> ThrowableFunction2_1<X, I1, I2, R> compose(ThrowableFunction2_1<X, ? super I1, ? super I2, ? extends T> before) {
        Objects.requireNonNull(before);
        return (i1, i2) -> apply(before.apply(i1, i2));
    }

}
