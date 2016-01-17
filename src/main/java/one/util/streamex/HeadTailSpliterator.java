/*
 * Copyright 2015, 2016 Tagir Valeev
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
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/*package*/ final class HeadTailSpliterator<T, U> extends AbstractSpliterator<U> implements TailCallSpliterator<U> {
    private Spliterator<T> source;
    private BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<U>> mapper;
    private Supplier<? extends Stream<U>> emptyMapper;
    private Spliterator<U> target;
    private boolean finished;
    BaseStream<?, ?> owner;
    
    HeadTailSpliterator(Spliterator<T> source, BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<U>> mapper,
            Supplier<? extends Stream<U>> emptyMapper) {
        super(Long.MAX_VALUE, ORDERED);
        this.source = source;
        this.mapper = mapper;
        this.emptyMapper = emptyMapper;
    }
    
    private static <T> Spliterator<T> traverseTail(Spliterator<T> spltr) {
        Spliterator<T> current = spltr;
        while (current instanceof TailCallSpliterator) {
            Spliterator<T> next = ((TailCallSpliterator<T>) current).tail();
            if (next == current)
                break;
            current = next;
        }
        return current;
    }

    @Override
    public boolean tryAdvance(Consumer<? super U> action) {
        if(!init())
            return false;
        target = traverseTail(target);
        return finishUnless(target.tryAdvance(action));
    }

    @Override
    public void forEachRemaining(Consumer<? super U> action) {
        if(init()) {
            Spliterator<U> t = target;
            while(t instanceof TailCallSpliterator) {
                t = traverseTail(t);
                if(!finishUnless(t.tryAdvance(action)))
                    return;
            }
            t.forEachRemaining(action);
            finishUnless(false);
        }
    }

    private boolean finishUnless(boolean cont) {
        if(cont)
            return true;
        target = null;
        finished = true;
        return false;
    }

    private boolean init() {
        if(finished)
            return false;
        if(target == null) {
            Box<T> first = new Box<>(null);
            Stream<U> stream = source.tryAdvance(x -> first.a = x) ? mapper.apply(first.a, StreamEx
                    .of(traverseTail(source))) : emptyMapper.get();
            source = null;
            mapper = null;
            emptyMapper = null;
            if(owner != null && mayHaveCloseAction(stream))
                owner.onClose(stream::close);
            target = stream == null ? Spliterators.emptySpliterator() : stream.spliterator();
        }
        return true;
    }
    
    @Override
    public long estimateSize() {
        if(finished)
            return 0;
        return (target == null ? source : target).estimateSize();
    }

    @Override
    public Spliterator<U> tail() {
        if(!init())
            return Spliterators.emptySpliterator();
        Spliterator<U> t = target;
        finishUnless(false);
        return t;
    }
}
