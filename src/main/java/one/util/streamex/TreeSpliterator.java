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

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import one.util.streamex.StreamExInternals.PairBox;

/**
 * @author Tagir Valeev
 *
 */
/* package */ class TreeSpliterator<T> implements Spliterator<T>, Consumer<T> {
    private T cur;
    private ArrayList<PairBox<Spliterator<T>, Stream<T>>> spliterators; 
    private final Function<T, Stream<T>> mapper;

    TreeSpliterator(T root, Function<T, Stream<T>> mapper) {
        this.cur = root;
        this.mapper = mapper;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        List<PairBox<Spliterator<T>, Stream<T>>> spltrs = spliterators;
        if(spltrs == null) {
            spliterators = new ArrayList<>();
            return processOne(action);
        }
        int lastIdx = spltrs.size()-1;
        while(lastIdx >= 0) {
            PairBox<Spliterator<T>, Stream<T>> pair = spltrs.get(lastIdx);
            Spliterator<T> spltr = pair.a;
            if(spltr.tryAdvance(this)) {
                return processOne(action);
            }
            pair.b.close();
            spltrs.remove(lastIdx--);
        }
        return false;
    }

    private boolean processOne(Consumer<? super T> action) {
        T e = this.cur;
        action.accept(e);
        Stream<T> stream = mapper.apply(e);
        if(stream != null) {
            spliterators.add(new PairBox<>(stream.spliterator(), stream));
        }
        return true;
    }

    @Override
    public Spliterator<T> trySplit() {
        // TODO Auto-generated method stub
        return null;
    }
    
    static class Acceptor<T> implements Consumer<T> {
        private final Consumer<? super T> action;
        private final Function<T, Stream<T>> mapper;
        
        public Acceptor(Consumer<? super T> action, Function<T, Stream<T>> mapper) {
            super();
            this.action = action;
            this.mapper = mapper;
        }

        @Override
        public void accept(T t) {
            action.accept(t);
            try(Stream<T> stream = mapper.apply(t)) {
                if(stream != null) {
                    stream.spliterator().forEachRemaining(this);
                }
            }
        }
    }
    
    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if(spliterators != null) {
            Spliterator.super.forEachRemaining(action);
        } else {
            new Acceptor<>(action, mapper).accept(cur);
        }
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED;
    }

    @Override
    public void accept(T t) {
        cur = t;
    }
    
    void close() {
        if(spliterators != null) {
            Throwable t = null;
            for(int i=spliterators.size()-1; i>=0; i--) {
                try {
                    spliterators.get(i).b.close();
                } catch (Error | RuntimeException e) {
                    if(t == null)
                        t = e;
                    else
                        t.addSuppressed(e);
                }
            }
            if(t instanceof RuntimeException)
                throw (RuntimeException)t;
            if(t instanceof Error)
                throw (Error)t;
        }
    }
}
