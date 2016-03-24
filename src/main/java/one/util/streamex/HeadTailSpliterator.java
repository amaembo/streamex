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
import java.util.stream.Stream;

import one.util.streamex.StreamExInternals.TailSpliterator;
import static one.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
/*package*/ final class HeadTailSpliterator<T, U> extends AbstractSpliterator<U> implements TailSpliterator<U> {
    private Spliterator<T> source;
    private BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<U>> mapper;
    private Supplier<? extends Stream<U>> emptyMapper;
    private Spliterator<U> target;
    StreamContext context;
    
    HeadTailSpliterator(Spliterator<T> source, BiFunction<? super T, ? super StreamEx<T>, ? extends Stream<U>> mapper,
            Supplier<? extends Stream<U>> emptyMapper) {
        super(Long.MAX_VALUE, ORDERED);
        this.source = source;
        this.mapper = mapper;
        this.emptyMapper = emptyMapper;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super U> action) {
        if(!init())
            return false;
        target = TailSpliterator.tryAdvanceWithTail(target, action);
        if(target == null) {
            context = null;
            return false;
        }
        return true;
    }

    @Override
    public Spliterator<U> tryAdvanceOrTail(Consumer<? super U> action) {
        if(!init())
            return null;
        Spliterator<U> tail = target;
        target = null;
        context = null;
        return tail;
    }

    @Override
    public void forEachRemaining(Consumer<? super U> action) {
        if(!init())
            return;
        TailSpliterator.forEachWithTail(target, action);
        target = null;
        context = null;
    }

    @Override
    public Spliterator<U> forEachOrTail(Consumer<? super U> action) {
        return tryAdvanceOrTail(action);
    }

    private boolean init() {
        if(context == null)
            return false;
        if(target == null) {
            Box<T> first = new Box<>();
            source = TailSpliterator.tryAdvanceWithTail(source, first);
            Stream<U> stream = source == null ? emptyMapper.get() : mapper.apply(first.a, StreamEx.of(source));
            source = null;
            mapper = null;
            emptyMapper = null;
            if(stream == null) {
                target = Spliterators.emptySpliterator();
            } else {
                StreamContext ctx = StreamContext.of(stream);
                if(ctx.closeHandler != null)
                    context.onClose(ctx.closeHandler);
                target = stream.spliterator();
            }
        }
        return true;
    }
    
    @Override
    public long estimateSize() {
        if(context == null)
            return 0;
        return (target == null ? source : target).estimateSize();
    }
}
