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

import java.text.BreakIterator;
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
public class Joining extends CancellableCollector<CharSequence, Joining.Accumulator, String> {
    static class Accumulator {
        List<CharSequence> data = new ArrayList<>();
        int chars = 0, count = 0;
    }

    private static final int CUT_ANYWHERE = 0;
    private static final int CUT_CODEPOINT = 1;
    private static final int CUT_SYMBOL = 2;
    private static final int CUT_WORD = 3;
    private static final int CUT_BEFORE_DELIMITER = 4;
    private static final int CUT_AFTER_DELIMITER = 5;

    private static final int LENGTH_CHARS = 0;
    private static final int LENGTH_CODEPOINTS = 1;
    private static final int LENGTH_SYMBOLS = 2;

    private final String delimiter, ellipsis, prefix, suffix;
    private final int cutStrategy, lenStrategy, maxLength;

    private Joining(String delimiter, String ellipsis, String prefix, String suffix, int cutStrategy, int lenStrategy,
            int maxLength) {
        this.delimiter = delimiter;
        this.ellipsis = ellipsis;
        this.prefix = prefix;
        this.suffix = suffix;
        this.cutStrategy = cutStrategy;
        this.lenStrategy = lenStrategy;
        this.maxLength = maxLength;
    }
    
    private int length(CharSequence s) {
        switch(lenStrategy) {
        case LENGTH_CHARS:
            return s.length();
        case LENGTH_CODEPOINTS:
            if(s instanceof String)
                return ((String)s).codePointCount(0, s.length());
            return (int) s.codePoints().count();
        case LENGTH_SYMBOLS:
            BreakIterator bi = BreakIterator.getCharacterInstance();
            bi.setText(s.toString());
            int count = 0;
            for (int end = bi.next();
                 end != BreakIterator.DONE;
                 end = bi.next()) count++;
            return count;
        default:
            throw new InternalError();
        }
    }
    
    private static int nonNegative(int limit) {
        if(limit < 0)
            throw new IllegalArgumentException(limit+": must be positive");
        return limit;
    }

    public static Joining on(CharSequence delimiter) {
        return new Joining(delimiter.toString(), "...", "", "", CUT_CODEPOINT, LENGTH_CHARS, -1);
    }

    public Joining wrap(CharSequence prefix, CharSequence suffix) {
        return new Joining(delimiter, ellipsis, prefix.toString().concat(this.prefix), this.suffix.concat(suffix
                .toString()), cutStrategy, lenStrategy, maxLength);
    }
    
    public Joining ellipsis(CharSequence ellipsis) {
        return new Joining(delimiter, ellipsis.toString(), prefix, suffix, cutStrategy, lenStrategy, maxLength);
    }
    
    public Joining maxChars(int limit) {
        return new Joining(delimiter, ellipsis, prefix, suffix, cutStrategy, LENGTH_CHARS, nonNegative(limit));
    }

    public Joining maxCodePoints(int limit) {
        return new Joining(delimiter, ellipsis, prefix, suffix, cutStrategy, LENGTH_CODEPOINTS, nonNegative(limit));
    }

    public Joining maxSymbols(int limit) {
        return new Joining(delimiter, ellipsis, prefix, suffix, cutStrategy, LENGTH_SYMBOLS, nonNegative(limit));
    }
    
    public Joining cutAnywhere() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_ANYWHERE, lenStrategy, maxLength);
    }
    
    public Joining cutAfterCodePoint() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_CODEPOINT, lenStrategy, maxLength);
    }
    
    public Joining cutAfterSymbol() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_SYMBOL, lenStrategy, maxLength);
    }
    
    public Joining cutAfterWord() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_WORD, lenStrategy, maxLength);
    }
    
    public Joining cutBeforeDelimiter() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_BEFORE_DELIMITER, lenStrategy, maxLength);
    }
    
    public Joining cutAfterDelimiter() {
        return new Joining(delimiter, ellipsis, prefix, suffix, CUT_AFTER_DELIMITER, lenStrategy, maxLength);
    }
    
    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, CharSequence> accumulator() {
        if(maxLength == -1)
            return (acc, str) -> {
                if (!acc.data.isEmpty())
                    acc.chars += delimiter.length();
                acc.chars += str.length();
                acc.data.add(str);
            };
        int delimCount = length(delimiter);
        int reducedMax = maxLength-length(prefix)-length(suffix);
        return (acc, str) -> {
            if(acc.count <= reducedMax) {
                if (!acc.data.isEmpty()) {
                    acc.chars += delimiter.length();
                    acc.count += delimCount;
                }
                acc.chars += str.length();
                acc.count += length(str);
                acc.data.add(str);
            }
        };
    }

    @Override
    public BinaryOperator<Accumulator> combiner() {
        if(maxLength == -1)
            return (acc1, acc2) -> {
                if (acc1.data.isEmpty())
                    return acc2;
                if (acc2.data.isEmpty())
                    return acc1;
                acc1.chars += delimiter.length() + acc2.chars;
                acc1.data.addAll(acc2.data);
                return acc1;
            };
        int delimCount = length(delimiter);
        int reducedMax = maxLength-length(prefix)-length(suffix);
        BiConsumer<Accumulator, CharSequence> accumulator = accumulator();
        return (acc1, acc2) -> {
            if (acc1.data.isEmpty())
                return acc2;
            if (acc2.data.isEmpty())
                return acc1;
            int len = acc1.count + acc2.count + delimCount;
            if (len <= reducedMax) {
                acc1.count = len;
                acc1.chars += delimiter.length() + acc2.chars;
                acc1.data.addAll(acc2.data);
            } else {
                for (CharSequence s : acc2.data) {
                    if (acc1.count > reducedMax)
                        break;
                    accumulator.accept(acc1, s);
                }
            }
            return acc1;
        };
    }

    @Override
    public Function<Accumulator, String> finisher() {
        if(maxLength == -1)
            return acc -> {
                char[] buf = new char[acc.chars+prefix.length()+suffix.length()];
                int size = acc.data.size();
                prefix.getChars(0, prefix.length(), buf, 0);
                int pos = prefix.length();
                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        delimiter.getChars(0, delimiter.length(), buf, pos);
                        pos += delimiter.length();
                    }
                    String cs = acc.data.get(i).toString();
                    cs.getChars(0, cs.length(), buf, pos);
                    pos += cs.length();
                }
                suffix.getChars(0, suffix.length(), buf, pos);
                return new String(buf);
            };
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Set<java.util.stream.Collector.Characteristics> characteristics() {
        return Collections.emptySet();
    }

    @Override
    Predicate<Accumulator> finished() {
        if(maxLength == -1)
            return null;
        int addCount = length(prefix)+length(suffix);
        if(maxLength <= addCount)
            return acc -> true;
        return acc -> acc.count > addCount;
    }
}
