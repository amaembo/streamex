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
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static javax.util.streamex.StreamExInternals.*;

/**
 * An advanced implementation of joining {@link Collector} which is capable to
 * join the input {@code CharSequence} elements with given delimiter optionally
 * wrapping into given prefix and suffix and optionally limiting the length of
 * the resulting string (in UTF-16 chars, codepoints or Unicode symbols) adding
 * the specified ellipsis symbol. This collector supercedes the standard JDK
 * {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
 * collectors family.
 * 
 * <p>
 * This collector is <a
 * href="package-summary.html#ShortCircuitReduction">short-circuiting</a> when
 * the string length is limited in either of ways. Otherwise it's not
 * short-circuiting.
 * 
 * <p>
 * Every specific collector represented by this class is immutable, so you can
 * share it. A bunch of methods is provided to create a new collector based on
 * this one.
 * 
 * <p>
 * To create {@code Joining} collector use {@link #with(CharSequence)} static
 * method and specify the delimiter. For further setup use specific instance
 * methods which return new {@code Joining} objects like this:
 * 
 * <pre>{@code
 * StreamEx.of(source).collect(Joining.with(", ").wrap("[", "]")
 *         .maxCodePoints(100).cutAfterWord());
 * }</pre>
 * 
 * <p>
 * The intermediate accumulation type of this collector is the implementation
 * detail and not exposed to the API. If you want to cast it to {@code Collector}
 * type, use ? as accumulator type variable:
 * 
 * <pre>{@code
 * Collector<CharSequence, ?, String> joining = Joining.with(", ");
 * }</pre>
 * 
 * @author Tagir Valeev
 * @since 0.4.1
 */
public class Joining extends CancellableCollector<CharSequence, Joining.Accumulator, String> {
    static final class Accumulator {
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
    private int limit, delimCount = -1;

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
    
    private void init() {
        if(delimCount == -1) {
            limit = maxLength - length(prefix) - length(suffix);
            delimCount = length(delimiter);
        }
    }

    private int length(CharSequence s) {
        switch (lenStrategy) {
        case LENGTH_CHARS:
            return s.length();
        case LENGTH_CODEPOINTS:
            if (s instanceof String)
                return ((String) s).codePointCount(0, s.length());
            return (int) s.codePoints().count();
        case LENGTH_SYMBOLS:
            BreakIterator bi = BreakIterator.getCharacterInstance();
            bi.setText(s.toString());
            int count = 0;
            for (int end = bi.next(); end != BreakIterator.DONE; end = bi.next())
                count++;
            return count;
        default:
            throw new InternalError();
        }
    }

    private static int copy(char[] buf, int pos, String str) {
        str.getChars(0, str.length(), buf, pos);
        return pos + str.length();
    }

    private int copyCut(char[] buf, int pos, String str, int limit, int cutStrategy) {
        if (limit <= 0)
            return pos;
        int endPos = str.length();
        switch (lenStrategy) {
        case LENGTH_CHARS:
            if (limit < str.length())
                endPos = limit;
            break;
        case LENGTH_CODEPOINTS:
            if (limit < str.codePointCount(0, str.length()))
                endPos = str.offsetByCodePoints(0, limit);
            break;
        case LENGTH_SYMBOLS:
            BreakIterator bi = BreakIterator.getCharacterInstance();
            bi.setText(str);
            int count = limit, end = 0;
            while (true) {
                end = bi.next();
                if (end == BreakIterator.DONE)
                    break;
                if (--count == 0) {
                    endPos = end;
                    break;
                }
            }
            break;
        default:
            throw new InternalError();
        }
        if (endPos < str.length()) {
            BreakIterator bi;
            switch (cutStrategy) {
            case CUT_BEFORE_DELIMITER:
            case CUT_AFTER_DELIMITER:
                endPos = 0;
                break;
            case CUT_WORD:
                bi = BreakIterator.getWordInstance();
                bi.setText(str);
                endPos = bi.preceding(endPos + 1);
                break;
            case CUT_SYMBOL:
                bi = BreakIterator.getCharacterInstance();
                bi.setText(str);
                endPos = bi.preceding(endPos + 1);
                break;
            case CUT_ANYWHERE:
                break;
            case CUT_CODEPOINT:
                if (Character.isHighSurrogate(str.charAt(endPos - 1)) && Character.isLowSurrogate(str.charAt(endPos)))
                    endPos--;
                break;
            default:
                throw new InternalError();
            }
        }
        str.getChars(0, endPos, buf, pos);
        return pos + endPos;
    }

    private String finisherNoOverflow(Accumulator acc) {
        char[] buf = new char[acc.chars + prefix.length() + suffix.length()];
        int size = acc.data.size();
        int pos = copy(buf, 0, prefix);
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                pos = copy(buf, pos, delimiter);
            }
            pos = copy(buf, pos, acc.data.get(i).toString());
        }
        copy(buf, pos, suffix);
        return new String(buf);
    }

    private Joining withLimit(int lenStrategy, int maxLength) {
        if (maxLength < 0)
            throw new IllegalArgumentException(maxLength + ": must be positive");
        return new Joining(delimiter, ellipsis, prefix, suffix, cutStrategy, lenStrategy, maxLength);
    }

    private Joining withCut(int cutStrategy) {
        return new Joining(delimiter, ellipsis, prefix, suffix, cutStrategy, lenStrategy, maxLength);
    }
    
    public static Joining with(CharSequence delimiter) {
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
        return withLimit(LENGTH_CHARS, limit);
    }

    public Joining maxCodePoints(int limit) {
        return withLimit(LENGTH_CODEPOINTS, limit);
    }

    public Joining maxSymbols(int limit) {
        return withLimit(LENGTH_SYMBOLS, limit);
    }

    public Joining cutAnywhere() {
        return withCut(CUT_ANYWHERE);
    }

    public Joining cutAfterCodePoint() {
        return withCut(CUT_CODEPOINT);
    }

    public Joining cutAfterSymbol() {
        return withCut(CUT_SYMBOL);
    }

    public Joining cutAfterWord() {
        return withCut(CUT_WORD);
    }

    public Joining cutBeforeDelimiter() {
        return withCut(CUT_BEFORE_DELIMITER);
    }

    public Joining cutAfterDelimiter() {
        return withCut(CUT_AFTER_DELIMITER);
    }

    @Override
    public Supplier<Accumulator> supplier() {
        return Accumulator::new;
    }

    @Override
    public BiConsumer<Accumulator, CharSequence> accumulator() {
        if (maxLength == -1)
            return (acc, str) -> {
                if (!acc.data.isEmpty())
                    acc.chars += delimiter.length();
                acc.chars += str.length();
                acc.data.add(str);
            };
        init();
        return (acc, str) -> {
            if (acc.count <= limit) {
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
        if (maxLength == -1)
            return (acc1, acc2) -> {
                if (acc1.data.isEmpty())
                    return acc2;
                if (acc2.data.isEmpty())
                    return acc1;
                acc1.chars += delimiter.length() + acc2.chars;
                acc1.data.addAll(acc2.data);
                return acc1;
            };
        init();
        BiConsumer<Accumulator, CharSequence> accumulator = accumulator();
        return (acc1, acc2) -> {
            if (acc1.data.isEmpty())
                return acc2;
            if (acc2.data.isEmpty())
                return acc1;
            int len = acc1.count + acc2.count + delimCount;
            if (len <= limit) {
                acc1.count = len;
                acc1.chars += delimiter.length() + acc2.chars;
                acc1.data.addAll(acc2.data);
            } else {
                for (CharSequence s : acc2.data) {
                    if (acc1.count > limit)
                        break;
                    accumulator.accept(acc1, s);
                }
            }
            return acc1;
        };
    }
    
    @Override
    public Function<Accumulator, String> finisher() {
        if (maxLength == -1) {
            return this::finisherNoOverflow;
        }
        init();
        if (limit <= 0) {
            char[] buf = new char[prefix.length() + suffix.length()];
            int pos = copyCut(buf, 0, prefix, maxLength, cutStrategy);
            pos = copyCut(buf, pos, suffix, maxLength - length(prefix), cutStrategy);
            String result = new String(buf, 0, pos);
            return acc -> result;
        }
        return acc -> {
            if (acc.count <= limit)
                return finisherNoOverflow(acc);
            char[] buf = new char[acc.chars + prefix.length() + suffix.length()];
            int size = acc.data.size();
            int pos = copy(buf, 0, prefix);
            int ellipsisCount = length(ellipsis);
            int rest = limit - ellipsisCount;
            if (rest < 0) {
                pos = copyCut(buf, pos, ellipsis, limit, CUT_ANYWHERE);
            } else {
                for (int i = 0; i < size; i++) {
                    String s = acc.data.get(i).toString();
                    int count = length(s);
                    if (i > 0) {
                        if (cutStrategy == CUT_BEFORE_DELIMITER && delimCount + count > rest) {
                            break;
                        }
                        if (delimCount > rest) {
                            pos = copyCut(buf, pos, delimiter, rest, cutStrategy);
                            break;
                        }
                        rest -= delimCount;
                        pos = copy(buf, pos, delimiter);
                    }
                    if (cutStrategy == CUT_AFTER_DELIMITER && delimCount + count > rest) {
                        break;
                    }
                    if (count > rest) {
                        pos = copyCut(buf, pos, s, rest, cutStrategy);
                        break;
                    }
                    pos = copy(buf, pos, s);
                    rest -= count;
                }
                pos = copy(buf, pos, ellipsis);
            }
            pos = copy(buf, pos, suffix);
            return new String(buf, 0, pos);
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        init();
        if (limit <= 0)
            return Collections.singleton(Characteristics.UNORDERED);
        return Collections.emptySet();
    }

    @Override
    Predicate<Accumulator> finished() {
        if (maxLength == -1)
            return null;
        init();
        if (limit <= 0)
            return acc -> true;
        return acc -> acc.count > limit;
    }
}
