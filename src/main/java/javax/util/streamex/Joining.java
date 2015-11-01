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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static javax.util.streamex.StreamExInternals.*;

/**
 * @author Tagir Valeev
 */
public class Joining extends CancellableCollector<CharSequence, Joining.Accumulator, String>{
    static class Accumulator {
        List<CharSequence> data = new ArrayList<>();
        int count = 0;
    }
    
    private final String delimiter;
    
    private Joining(CharSequence delimiter) {
        this.delimiter = delimiter.toString();
    }
    
    public static Joining with(CharSequence delimiter) {
        return new Joining(delimiter);
    }

    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, CharSequence> accumulator() {
        return (acc, str) -> {
            if(!acc.data.isEmpty())
                acc.count+=delimiter.length();
            acc.count+=str.length();
            acc.data.add(str);
        };
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        return (acc1, acc2) -> {
            if(acc1.data.isEmpty())
                return acc2;
            if(acc2.data.isEmpty())
                return acc1;
            acc1.count+=delimiter.length()+acc2.count;
            acc1.data.addAll(acc2.data);
            return acc1;
        };
    }

    @Override
    public Function<Accumulator, String> finisher() {
        return acc -> {
            char[] buf = new char[acc.count];
            int pos = 0;
            int size = acc.data.size();
            for(int i=0; i<size; i++) {
                if(i > 0) {
                    delimiter.getChars(0, delimiter.length(), buf, pos);
                    pos+=delimiter.length();
                }
                String cs = acc.data.get(i).toString();
                cs.getChars(0, cs.length(), buf, pos);
                pos+=cs.length();
            }
            return new String(buf);
        };
    }

    @Override
    public Set<java.util.stream.Collector.Characteristics> characteristics() {
        return Collections.emptySet();
    }

    @Override
    Predicate<Accumulator> finished() {
        return null;
    }
}
