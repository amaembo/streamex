package one.util.functionex;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function with no arguments and returning one value.
 *
 * @param <X> the exception which may throw
 * @param <R> return type 1 of the function
 */
@FunctionalInterface
public interface ThrowableFunction0_1<X extends Throwable, R> extends Serializable {

    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Gets a result.
     *
     * @return a result
     */
    R apply() throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowableFunction0_1<? extends Throwable, ? extends R>} to
     * {@code one.util.functionex.ThrowableFunction0_1<? extends Throwable,R>}
     *
     * @param f A {@code one.util.functionex.ThrowableFunction0_1}
     * @param <R> The return type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowableFunction0_1<? extends Throwable,R>}
     */
    @SuppressWarnings("unchecked")
    static <R> ThrowableFunction0_1<? extends Throwable, R> narrow(ThrowableFunction0_1<? extends Throwable, ? extends R> f) {
        return (ThrowableFunction0_1<? extends Throwable, R>) f;
    }

    /**
     * Returns a composed function that first applies this function, and then applies
     * the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param after the function to apply after this function
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default ThrowableFunction0_0<X> andConsumes(ThrowableFunction1_0<X, ? super R> after) {
        Objects.requireNonNull(after);
        return () -> after.apply(apply());
    }
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> ThrowableFunction0_1<X, V> andMap(ThrowableFunction1_1<X, ? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return () -> after.apply(apply());
    }
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function resulting on an Entry.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V1> the type of key of the result entry
     * @param <V2> the type of value of the result entry
     * @param after the function to apply after this function
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    @SuppressWarnings("unchecked")
    default <V1, V2> ThrowableFunction0_2<X, V1, V2> andMapToEntry(ThrowableFunction1_2<X, ? super R, ? extends V1, ? extends V2> after) {
        Objects.requireNonNull(after);
        return () -> (Map.Entry<V1, V2>) after.apply(apply());
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
    default ThrowableFunction0_1<X, R> compose(ThrowableFunction0_0<X> before) {
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
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and
     * then applies this function
     * @throws NullPointerException if before is null
     */
    default <T> ThrowableFunction1_1<X, T, R> compose(ThrowableFunction1_0<X, ? super T> before) {
        Objects.requireNonNull(before);
        return t1 -> {
            before.apply(t1);
            return apply();
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
    default <T1, T2> ThrowableFunction2_1<X, T1, T2, R> compose(ThrowableFunction2_0<X, ? super T1, ? super T2> before) {
        Objects.requireNonNull(before);
        return (t1, t2) -> {
            before.apply(t1, t2);
            return apply();
        };
    }

}
