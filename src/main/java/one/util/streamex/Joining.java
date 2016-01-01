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

import static one.util.streamex.StreamExInternals.*;

/**
 * An advanced implementation of joining {@link Collector}. This collector is
 * capable to join the input {@code CharSequence} elements with given delimiter
 * optionally wrapping into given prefix and suffix and optionally limiting the
 * length of the resulting string (in Unicode code units, code points or
 * grapheme clusters) adding the specified ellipsis sequence. This collector
 * supersedes the standard JDK
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
 *         .maxCodePoints(100).cutAtWord());
 * }</pre>
 * 
 * <p>
 * The intermediate accumulation type of this collector is the implementation
 * detail and not exposed to the API. If you want to cast it to
 * {@code Collector} type, use ? as accumulator type variable:
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
    private static final int CUT_GRAPHEME = 2;
    private static final int CUT_WORD = 3;
    private static final int CUT_BEFORE_DELIMITER = 4;
    private static final int CUT_AFTER_DELIMITER = 5;

    private static final int LENGTH_CHARS = 0;
    private static final int LENGTH_CODEPOINTS = 1;
    private static final int LENGTH_GRAPHEMES = 2;

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
        if (delimCount == -1) {
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
        case LENGTH_GRAPHEMES:
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
        case LENGTH_GRAPHEMES:
            BreakIterator bi = BreakIterator.getCharacterInstance();
            bi.setText(str);
            int count = limit,
            end = 0;
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
            case CUT_GRAPHEME:
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

    /**
     * Returns a {@code Collector} that concatenates the input elements,
     * separated by the specified delimiter, in encounter order.
     * 
     * <p>
     * This collector is similar to {@link Collectors#joining(CharSequence)},
     * but can be further set up in a flexible way, for example, specifying the
     * maximal allowed length of the resulting {@code String}.
     *
     * @param delimiter the delimiter to be used between each element
     * @return A {@code Collector} which concatenates CharSequence elements,
     *         separated by the specified delimiter, in encounter order
     * @see Collectors#joining(CharSequence)
     */
    public static Joining with(CharSequence delimiter) {
        return new Joining(delimiter.toString(), "...", "", "", CUT_GRAPHEME, LENGTH_CHARS, -1);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but
     * additionally wraps the result with the specified prefix and suffix.
     * 
     * <p>
     * The collector returned by
     * {@code Joining.with(delimiter).wrap(prefix, suffix)} is equivalent to
     * {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}, but
     * can be further set up in a flexible way, for example, specifying the
     * maximal allowed length of the resulting {@code String}.
     * 
     * <p>
     * If length limit is specified for the collector, the prefix length and the
     * suffix length are also counted towards this limit. If the length of the
     * prefix and the suffix exceed the limit, the resulting collector will not
     * accumulate any elements and produce the same output. For example,
     * {@code stream.collect(Joining.with(",").wrap("prefix", "suffix").maxChars(9))}
     * will produce {@code "prefixsuf"} string regardless of the input stream
     * content.
     * 
     * <p>
     * You may wrap several times:
     * {@code Joining.with(",").wrap("[", "]").wrap("(", ")")} is equivalent to
     * {@code Joining.with(",").wrap("([", "])")}.
     * 
     * @param prefix the sequence of characters to be used at the beginning of
     *        the joined result
     * @param suffix the sequence of characters to be used at the end of the
     *        joined result
     * @return a new {@code Collector} which wraps the result with the specified
     *         prefix and suffix.
     */
    public Joining wrap(CharSequence prefix, CharSequence suffix) {
        return new Joining(delimiter, ellipsis, prefix.toString().concat(this.prefix), this.suffix.concat(suffix
                .toString()), cutStrategy, lenStrategy, maxLength);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but uses
     * the specified ellipsis {@code CharSequence} instead of default
     * {@code "..."} when the string limit (if specified) is reached.
     * 
     * @param ellipsis the sequence of characters to be used at the end of the
     *        joined result to designate that not all of the input elements are
     *        joined due to the specified string length restriction.
     * @return a new {@code Collector} which will use the specified ellipsis
     *         instead of current setting.
     */
    public Joining ellipsis(CharSequence ellipsis) {
        return new Joining(delimiter, ellipsis.toString(), prefix, suffix, cutStrategy, lenStrategy, maxLength);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but sets
     * the maximal length of the resulting string to the specified number of
     * UTF-16 characters (or Unicode code units). This setting overwrites any
     * limit previously set by {@link #maxChars(int)},
     * {@link #maxCodePoints(int)} or {@link #maxGraphemes(int)} call.
     * 
     * <p>
     * The {@code String} produced by the resulting collector is guaranteed to
     * have {@link String#length() length} which does not exceed the specified
     * limit. An ellipsis sequence (by default {@code "..."}) is used to
     * designate whether the limit was reached. Use
     * {@link #ellipsis(CharSequence)} to set custom ellipsis sequence.
     * 
     * <p>
     * The collector returned by this method is <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting</a>:
     * it may not process all the input elements if the limit is reached.
     * 
     * @param limit the maximal number of UTF-16 characters in the resulting
     *        String.
     * @return a new {@code Collector} which will produce String no longer than
     *         given limit.
     */
    public Joining maxChars(int limit) {
        return withLimit(LENGTH_CHARS, limit);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but sets
     * the maximal number of Unicode code points of the resulting string. This
     * setting overwrites any limit previously set by {@link #maxChars(int)},
     * {@link #maxCodePoints(int)} or {@link #maxGraphemes(int)} call.
     * 
     * <p>
     * The {@code String} produced by the resulting collector is guaranteed to
     * have no more code points than the specified limit. An ellipsis sequence
     * (by default {@code "..."}) is used to designate whether the limit was
     * reached. Use {@link #ellipsis(CharSequence)} to set custom ellipsis
     * sequence.
     * 
     * <p>
     * The collector returned by this method is <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting</a>:
     * it may not process all the input elements if the limit is reached.
     * 
     * @param limit the maximal number of code points in the resulting String.
     * @return a new {@code Collector} which will produce String no longer than
     *         given limit.
     */
    public Joining maxCodePoints(int limit) {
        return withLimit(LENGTH_CODEPOINTS, limit);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but sets
     * the maximal number of grapheme clusters. This setting overwrites any
     * limit previously set by {@link #maxChars(int)},
     * {@link #maxCodePoints(int)} or {@link #maxGraphemes(int)} call.
     * 
     * <p>
     * The grapheme cluster is defined in <a
     * href="http://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries"
     * >Unicode Text Segmentation</a> technical report. Basically, it counts
     * base character and the following combining characters as single object.
     * The {@code String} produced by the resulting collector is guaranteed to
     * have no more grapheme clusters than the specified limit. An ellipsis
     * sequence (by default {@code "..."}) is used to designate whether the
     * limit was reached. Use {@link #ellipsis(CharSequence)} to set custom
     * ellipsis sequence.
     * 
     * <p>
     * The collector returned by this method is <a
     * href="package-summary.html#ShortCircuitReduction">short-circuiting</a>:
     * it may not process all the input elements if the limit is reached.
     * 
     * @param limit the maximal number of grapheme clusters in the resulting
     *        String.
     * @return a new {@code Collector} which will produce String no longer than
     *         given limit.
     */
    public Joining maxGraphemes(int limit) {
        return withLimit(LENGTH_GRAPHEMES, limit);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string at any point when limit is reached.
     * 
     * <p>
     * The resulting collector will produce {@code String} which length is
     * exactly equal to the specified limit if the limit is reached. If used
     * with {@link #maxChars(int)}, the resulting string may be cut in the
     * middle of surrogate pair.
     * 
     * @return a new {@code Collector} which cuts the resulting string at any
     *         point when limit is reached.
     */
    public Joining cutAnywhere() {
        return withCut(CUT_ANYWHERE);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string between any code points when limit is reached.
     * 
     * <p>
     * The resulting collector will not split the surrogate pair when used with
     * {@link #maxChars(int)} or {@link #maxCodePoints(int)}. However it may
     * remove the combining character which may result in incorrect rendering of
     * the last displayed grapheme.
     * 
     * @return a new {@code Collector} which cuts the resulting string between
     *         code points.
     */
    public Joining cutAtCodePoint() {
        return withCut(CUT_CODEPOINT);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string at grapheme cluster boundary when limit is reached.
     * This is the default behavior.
     * 
     * <p>
     * The grapheme cluster is defined in <a
     * href="http://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries"
     * >Unicode Text Segmentation</a> technical report. Thus the resulting
     * collector will not split the surrogate pair and will preserve any
     * combining characters or remove them with the base character.
     * 
     * @return a new {@code Collector} which cuts the resulting string at
     *         grapheme cluster boundary.
     */
    public Joining cutAtGrapheme() {
        return withCut(CUT_GRAPHEME);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string at word boundary when limit is reached.
     * 
     * <p>
     * The beginning and end of every input stream element or delimiter is
     * always considered as word boundary, so the stream of
     * {@code "one", "two three"} collected with
     * {@code Joining.with("").maxChars(n).ellipsis("").cutAtWord()} may produce
     * the following strings depending on {@code n}:
     * 
     * <pre>{@code
     * ""
     * "one"
     * "onetwo"
     * "onetwo "
     * "onetwo three"
     * }</pre>
     * 
     * @return a new {@code Collector} which cuts the resulting string at word
     *         boundary.
     */
    public Joining cutAtWord() {
        return withCut(CUT_WORD);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string before the delimiter when limit is reached.
     * 
     * @return a new {@code Collector} which cuts the resulting string at before
     *         the delimiter.
     */
    public Joining cutBeforeDelimiter() {
        return withCut(CUT_BEFORE_DELIMITER);
    }

    /**
     * Returns a {@code Collector} which behaves like this collector, but cuts
     * the resulting string after the delimiter when limit is reached.
     * 
     * @return a new {@code Collector} which cuts the resulting string at after
     *         the delimiter.
     */
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
