/*
 * Copyright 2015, 2023 StreamEx contributors
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

package one.util.streamex;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/* package */ class IfEmptySpliterator<T> extends Internals.CloneableSpliterator<T, IfEmptySpliterator<T>> {
    // alt == null --> decision is made
    private Spliterator<? extends T> spltr, alt;
    // positive = number of non-exhausted spliterators; negative = surely non-empty
    private final AtomicInteger state = new AtomicInteger(1);

    public IfEmptySpliterator(Spliterator<? extends T> spltr, Spliterator<? extends T> alt) {
        this.spltr = spltr;
        this.alt = alt;
    }

    void tryInit() {
        if (alt != null && spltr.hasCharacteristics(SIZED)) {
            if (spltr.estimateSize() == 0) {
                spltr = alt;
            }
            alt = null;
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (alt != null) {
            if (spltr.tryAdvance(action)) {
                state.set(-1);
                alt = null;
                return true;
            }
            if (drawState()) {
                spltr = alt;
            }
            alt = null;
        }
        return spltr.tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        tryInit();
        if (alt == null) {
            spltr.forEachRemaining(action);
        } else {
            boolean[] empty = {true};
            spltr.forEachRemaining(e -> {
                empty[0] = false;
                action.accept(e);
            });
            if (empty[0]) {
                if (drawState()) {
                    (spltr = alt).forEachRemaining(action);
                }
            } else {
                state.set(-1);
            }
            alt = null;
        }
    }

    boolean drawState() {
        return state.updateAndGet(x -> x > 0 ? x - 1 : x) == 0;
    }

    @Override
    public Spliterator<T> trySplit() {
        Spliterator<? extends T> prefix = spltr.trySplit();
        if (prefix == null) {
            return null;
        }
        tryInit();
        if (alt != null) {
            state.updateAndGet(x -> x > 0 ? x + 1 : x);
        }
        IfEmptySpliterator<T> clone = doClone();
        clone.spltr = prefix;
        return clone;
    }

    @Override
    public long estimateSize() {
        tryInit();
        long size = spltr.estimateSize();
        return alt != null && size == 0 ? alt.estimateSize() : size;
    }

    @Override
    public int characteristics() {
        if (alt == null) {
            return spltr.characteristics() & (~SORTED);
        }
        return (spltr.characteristics() & alt.characteristics() & (~SORTED)) |
                ((spltr.characteristics() | alt.characteristics()) & (ORDERED));
    }
}
