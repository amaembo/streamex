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
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Tagir Valeev
 */
final class TakeDropSpliterators {
    static final class TDOfRef<T> extends AbstractSpliterator<T> implements Consumer<T> {
        private final Predicate<? super T> predicate;
        private final boolean drop;
        private boolean checked;
        private final Spliterator<T> source;
        private T cur;

        public TDOfRef(Spliterator<T> source, boolean drop, Predicate<? super T> predicate) {
            super(source.estimateSize(), source.characteristics() & (ORDERED | SORTED | CONCURRENT | IMMUTABLE | NONNULL | DISTINCT));
            this.drop = drop;
            this.predicate = predicate;
            this.source = source;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if(checked) {
                if(!drop) return false;
                return source.tryAdvance(action);
            }
            if(drop) {
                while(source.tryAdvance(this)) {
                    if(!predicate.test(cur)) {
                        checked = true;
                        action.accept(cur);
                        return true;
                    }
                }
                return false;
            }
            if(!source.tryAdvance(this))
                return false;
            if(predicate.test(cur)) {
                action.accept(cur);
                return true;
            }
            checked = true;
            return false;
        }

        @Override
        public void accept(T t) {
            this.cur = t;
        }
    }
}
