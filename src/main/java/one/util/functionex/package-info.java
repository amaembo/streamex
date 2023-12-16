/**
 * This packaqe contains all throwable function interface needed in StreamEx.
 * The goal is to externalize this package in another module to be able to use it with
 * another library that I would like to propose after this.
 * <br/>
 * Maybe, the convention of class names won't be accepted, but it is not a problem for me to
 * adapt names like
 *  - {@link one.util.functionex.ThrowableFunction0_1} to {@code ThrowableSupplier}
 *  - {@link one.util.functionex.ThrowableFunction0_2} to {@code ThrowableBiSupplier} or {@code ThrowableEntrySupplier}
 *  - {@link one.util.functionex.ThrowableFunction1_0} to {@code ThrowableConsumer}
 *  - {@link one.util.functionex.ThrowableFunction1_1} to {@code ThrowableFunction}
 *  - {@link one.util.functionex.ThrowableFunction2_2} to {@code ThrowableBiToBiFunction} or {@code ThrowableBiToEntryFunction}for example
 *  - {@link one.util.functionex.ThrowableFunction1_2} to {@code ThrowableToBiFunction} for example
 *  - ...
 */
package one.util.functionex;

