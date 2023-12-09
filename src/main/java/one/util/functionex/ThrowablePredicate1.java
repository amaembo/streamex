package one.util.functionex;

import java.io.Serializable;
import java.util.Objects;

/**
 * A {@linkplain java.util.function.Predicate} which may throw taking one argument.
 *
 * @param <X> the exception which may throw
 * @param <T> the type of the input to the predicate
 */
@FunctionalInterface
public interface ThrowablePredicate1<X extends Throwable, T> extends Serializable {
    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     * @throws X if an exception occured
     */
    boolean test(T t) throws X;

    /**
     * Narrows the given {@code one.util.functionex.ThrowablePredicate1<? extends Throwable, ? super T>} to
     * {@code one.util.functionex.ThrowablePredicate1<? extends Throwable,T>}
     *
     * @param f A {@code one.util.functionex.ThrowablePredicate1}
     * @param <T> The parameter type
     * @return the given {@code f} instance as narrowed type {@code one.util.functionex.ThrowablePredicate1<? extends Throwable,T>}
     */
    @SuppressWarnings("unchecked")
    static <T> ThrowablePredicate1<? extends Throwable, T> narrow(ThrowablePredicate1<? extends Throwable, ? super T> f) {
        return (ThrowablePredicate1<? extends Throwable, T>) f;
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * AND of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code false}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * AND of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ThrowablePredicate1<X, T> and(ThrowablePredicate1<X, ? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    /**
     * Returns a predicate that represents the logical negation of this
     * predicate.
     *
     * @return a predicate that represents the logical negation of this
     * predicate
     */
    default ThrowablePredicate1<X, T> negate() {
        return (t) -> !test(t);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical
     * OR of this predicate and another.  When evaluating the composed
     * predicate, if this predicate is {@code true}, then the {@code other}
     * predicate is not evaluated.
     *
     * <p>Any exceptions thrown during evaluation of either predicate are relayed
     * to the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this
     *              predicate
     * @return a composed predicate that represents the short-circuiting logical
     * OR of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default ThrowablePredicate1<X, T> or(ThrowablePredicate1<X, ? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * Returns a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}.
     *
     * @param <X>     the type of exception the test can throw
     * @param <T> the type of arguments to the predicate
     * @param targetRef the object reference with which to compare for equality,
     *               which may be {@code null}
     * @return a predicate that tests if two arguments are equal according
     * to {@link Objects#equals(Object, Object)}
     */
    static <X extends Throwable, T> ThrowablePredicate1<X, T> isEqual(Object targetRef) {
        return (null == targetRef)
            ? Objects::isNull
            : targetRef::equals;
    }

    /**
     * Returns a predicate that is the negation of the supplied predicate.
     * This is accomplished by returning result of the calling
     * {@code target.negate()}.
     *
     * @param <X>     the type of exception the test can throw
     * @param <T>     the type of arguments to the specified predicate
     * @param target  predicate to negate
     *
     * @return a predicate that negates the results of the supplied
     *         predicate
     *
     * @throws NullPointerException if target is null
     *
     * @since 11
     */
    @SuppressWarnings("unchecked")
    static <X extends Throwable, T> ThrowablePredicate1<X, T> not(ThrowablePredicate1<X, ? super T> target) {
        Objects.requireNonNull(target);
        return (ThrowablePredicate1<X, T>)target.negate();
    }

}
