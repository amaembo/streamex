/*
 * Copyright 2015 Tagir Valeev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.util.streamex;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Tagir Valeev
 */
/* package */ final class CancellableCollectSpliterator<T, A> implements Spliterator<A>, Consumer<T>, Cloneable {
    private volatile Spliterator<T> source;
    private final BiConsumer<A, ? super T> accumulator;
    private final Predicate<A> cancelPredicate;
    private final Supplier<A> supplier;
    private AtomicBoolean cancelled = null;
    private final boolean ordered;
    private CancellableCollectSpliterator<T, A> prefix;
	private A acc;

	CancellableCollectSpliterator(Spliterator<T> source,
	        Supplier<A> supplier,
			BiConsumer<A, ? super T> accumulator,
			Predicate<A> cancelPredicate) {
		this.source = source;
		this.supplier = supplier;
		this.accumulator = accumulator;
		this.cancelPredicate = cancelPredicate;
		this.ordered = source.hasCharacteristics(ORDERED);
	}

	@Override
	public boolean tryAdvance(Consumer<? super A> action) {
	    Spliterator<T> source = this.source;
	    if(source == null)
	        return false;
	    acc = supplier.get();
	    if(cancelled == null) {
	        this.source = null;
	        // sequential mode
            while(!cancelPredicate.test(acc) && source.tryAdvance(this)) {
                // empty
            }
	    } else {
	        do {
                if(cancelPredicate.test(acc)) {
                    this.source = null;
                    if(isFinished())
                        cancelled.set(true);
                    break;
                }
	        } while(!cancelled.get() && source.tryAdvance(this));
	        this.source = null;
	    }
	    action.accept(acc);
		return true;
	}

	@Override
	public void forEachRemaining(Consumer<? super A> action) {
	    tryAdvance(action);
	}

	@Override
	public Spliterator<A> trySplit() {
	    Spliterator<T> prefix = source.trySplit();
	    if(prefix == null)
	        return null;
	    if(cancelled == null)
	        cancelled = new AtomicBoolean();
	    try {
			@SuppressWarnings("unchecked")
			CancellableCollectSpliterator<T, A> result = (CancellableCollectSpliterator<T, A>) this.clone();
			result.source = prefix;
			if(ordered)
			    this.prefix = result;
			return result;
		} catch (CloneNotSupportedException e) {
		    throw new InternalError();
		}
	}
	
	private boolean isFinished() {
	    return source == null && (prefix == null || prefix.isFinished());
	}

	@Override
	public long estimateSize() {
		return source == null ? 0 : source.estimateSize();
	}

	@Override
	public int characteristics() {
		return source == null ? SIZED : ordered ? ORDERED : 0;
	}

	@Override
	public void accept(T t) {
	    accumulator.accept(this.acc, t);
	}
}
