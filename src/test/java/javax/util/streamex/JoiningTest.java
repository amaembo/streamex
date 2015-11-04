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

import static javax.util.streamex.TestHelpers.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tagir Valeev
 */
public class JoiningTest {
    @Test
    public void testSimple() {
        Supplier<Stream<String>> s = () -> IntStream.range(0, 100).mapToObj(String::valueOf);
        checkCollector("joiningSimple", s.get().collect(Collectors.joining(", ")), s, Joining.with(", "));
        checkCollector("joiningWrap", s.get().collect(Collectors.joining(", ", "[", "]")), s,
            Joining.with(", ").wrap("[", "]"));
        checkCollector("joiningWrap2", s.get().collect(Collectors.joining(", ", "[(", ")]")), s, Joining.with(", ")
                .wrap("(", ")").wrap("[", "]"));
    }

    @Test
    public void testCutSimple() {
        List<String> input = Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten");
        assertEquals("", StreamEx.of(input).peek(Assert::fail).collect(Joining.with(", ").maxChars(0).cutAnywhere()));
        assertEquals("",
            StreamEx.of(input).parallel().peek(Assert::fail).collect(Joining.with(", ").maxChars(0).cutAnywhere()));
        String expected = "one, two, three, four, five, six, seven, eight, nine, ten";
        for (int i = 3; i < expected.length() + 5; i++) {
            String exp = expected;
            if (exp.length() > i) {
                exp = exp.substring(0, i - 3) + "...";
            }
            String exp2 = expected;
            while (exp2.length() > i) {
                int pos = exp2.lastIndexOf(", ", exp2.endsWith(", ...") ? exp2.length() - 6 : exp2.length());
                exp2 = pos >= 0 ? exp2.substring(0, pos + 2) + "..." : "...";
            }
            for (StreamExSupplier<String> supplier : streamEx(input::stream)) {
                assertEquals(supplier + "/#" + i, exp,
                    supplier.get().collect(Joining.with(", ").maxChars(i).cutAnywhere()));
                assertEquals(supplier + "/#" + i, expected.substring(0, Math.min(i, expected.length())), supplier.get()
                        .collect(Joining.with(", ").ellipsis("").maxChars(i).cutAnywhere()));
                assertEquals(supplier + "/#" + i, exp2,
                    supplier.get().collect(Joining.with(", ").maxChars(i).cutAfterDelimiter()));
            }
        }

        byte[] data = { (byte) 0xFF, 0x30, 0x40, 0x50, 0x70, 0x12, (byte) 0xF0 };
        assertEquals(
            "FF 30 40 50 ...",
            IntStreamEx.of(data).mapToObj(b -> String.format(Locale.ENGLISH, "%02X", b & 0xFF))
                    .collect(Joining.with(" ").maxChars(15).cutAfterDelimiter()));
    }

    @Test
    public void testCuts() {
        List<String> input = Arrays.asList("one two", "three four", "five", "six seven");

        checkShortCircuitCollector("cutBefore", "one two, three four...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutBeforeDelimiter());
        checkShortCircuitCollector("cutBefore", "one two...", 2, input::stream, Joining.with(", ").maxChars(10)
                .cutBeforeDelimiter());
        checkShortCircuitCollector("cutBefore", "...", 2, input::stream, Joining.with(", ").maxChars(9)
                .cutBeforeDelimiter());

        checkShortCircuitCollector("cutAfter", "one two, three four, ...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutAfterDelimiter());
        checkShortCircuitCollector("cutAfter", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12)
                .cutAfterDelimiter());
        checkShortCircuitCollector("cutAfter", "...", 2, input::stream, Joining.with(", ").maxChars(11)
                .cutAfterDelimiter());

        checkShortCircuitCollector("cutWord", "one two, three four, ...", 4, input::stream, Joining.with(", ")
                .maxChars(25).cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two,...", 2, input::stream, Joining.with(", ").maxChars(11)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one two...", 2, input::stream, Joining.with(", ").maxChars(10)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one ...", 2, input::stream, Joining.with(", ").maxChars(9)
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "one...", 1, input::stream, Joining.with(", ").maxChars(6).cutAfterWord());

        checkShortCircuitCollector("cutCodePoint", "one two, three four, f...", 4, input::stream, Joining.with(", ")
                .maxChars(25));
        checkShortCircuitCollector("cutCodePoint", "one two, ...", 2, input::stream, Joining.with(", ").maxChars(12));
        checkShortCircuitCollector("cutCodePoint", "one two,...", 2, input::stream, Joining.with(", ").maxChars(11));
        checkShortCircuitCollector("cutCodePoint", "one two...", 2, input::stream, Joining.with(", ").maxChars(10));
        checkShortCircuitCollector("cutCodePoint", "one tw...", 2, input::stream, Joining.with(", ").maxChars(9));
        checkShortCircuitCollector("cutCodePoint", "one...", 1, input::stream, Joining.with(", ").maxChars(6));
    }

    @Test
    public void testPrefixSuffix() {
        List<String> input = Arrays.asList("one two", "three four", "five", "six seven");
        checkShortCircuitCollector("cutWord", "[one two, three four,...]", 3, input::stream,
            Joining.with(", ").wrap("[", "]").maxChars(25).cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one two...]", 2, input::stream,
            Joining.with(", ").maxChars(12).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one ...]", 2, input::stream,
            Joining.with(", ").maxChars(11).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one ...]", 2, input::stream,
            Joining.with(", ").maxChars(10).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[one...]", 1, input::stream,
            Joining.with(", ").maxChars(8).wrap("[", "]").cutAfterWord());
        checkShortCircuitCollector("cutWord", "[...]", 1, input::stream, Joining.with(", ").maxChars(6).wrap("[", "]")
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "[..]", 1, input::stream, Joining.with(", ").maxChars(4).wrap("[", "]")
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "[.]", 1, input::stream, Joining.with(", ").maxChars(3).wrap("[", "]")
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "[]", 0, input::stream, Joining.with(", ").maxChars(2).wrap("[", "]")
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "[", 0, input::stream, Joining.with(", ").maxChars(1).wrap("[", "]")
                .cutAfterWord());
        checkShortCircuitCollector("cutWord", "", 0, input::stream, Joining.with(", ").maxChars(0).wrap("[", "]")
                .cutAfterWord());

        checkShortCircuitCollector("cutWord", "a prefix  a ", 0, input::stream,
            Joining.with(" ").maxChars(15).wrap("a prefix ", " a suffix").cutAfterWord());
        checkShortCircuitCollector("cutWord", "a prefix  ", 0, input::stream,
            Joining.with(" ").maxChars(10).wrap("a prefix ", " a suffix").cutAfterWord());
        checkShortCircuitCollector("cutWord", "a ", 0, input::stream,
            Joining.with(" ").maxChars(5).wrap("a prefix ", " a suffix").cutAfterWord());
    }

    @Test
    public void testCodePoints() {
        List<String> input = Collections.nCopies(3,
            "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49\ud801\udc2f\ud801\udc3b");
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f\ud801", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(5).cutAnywhere());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(5).cutAfterCodePoint());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(4).cutAfterSymbol());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(4).cutAnywhere());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(4).cutAfterCodePoint());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(4).cutAfterSymbol());
        checkShortCircuitCollector("maxChars", "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28", 1, input::stream,
            Joining.with(",").ellipsis("").maxChars(9));

        checkShortCircuitCollector("maxCodePoints", "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49", 1,
            input::stream, Joining.with(",").ellipsis("").maxCodePoints(5).cutAnywhere());
        checkShortCircuitCollector("maxCodePoints", "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49", 1,
            input::stream, Joining.with(",").ellipsis("").maxCodePoints(5).cutAfterCodePoint());
        checkShortCircuitCollector("maxCodePoints", "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49", 1,
            input::stream, Joining.with(",").ellipsis("").maxCodePoints(5).cutAfterSymbol());
        checkShortCircuitCollector("maxCodePoints",
            "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49\ud801\udc2f\ud801\udc3b", 2, input::stream,
            Joining.with(",").ellipsis("").maxCodePoints(7));
        checkShortCircuitCollector("maxCodePoints",
            "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49\ud801\udc2f\ud801\udc3b,", 2, input::stream,
            Joining.with(",").ellipsis("").maxCodePoints(8));
        checkShortCircuitCollector("maxCodePoints",
            "\ud801\udc14\ud801\udc2f\ud801\udc45\ud801\udc28\ud801\udc49\ud801\udc2f\ud801\udc3b,\ud801\udc14", 2,
            input::stream, Joining.with(",").ellipsis("").maxCodePoints(9).cutAfterCodePoint());
    }

    @Test
    public void testSymbols() {
        List<String> input = Collections.nCopies(3, "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a");
        checkShortCircuitCollector("maxChars", "aa\u0300\u0321e", 1, input::stream, Joining.with(",").ellipsis("")
                .maxChars(5).cutAnywhere());
        checkShortCircuitCollector("maxChars", "aa\u0300\u0321e", 1, input::stream, Joining.with(",").ellipsis("")
                .maxChars(5).cutAfterCodePoint());
        checkShortCircuitCollector("maxChars", "aa\u0300\u0321", 1, input::stream, Joining.with(",").ellipsis("")
                .maxChars(5).cutAfterSymbol());
        checkShortCircuitCollector("maxChars", "aa\u0300\u0321e\u0300", 1, input::stream, Joining.with(",")
                .ellipsis("").maxChars(6).cutAfterSymbol());
        checkShortCircuitCollector("maxChars", "aa\u0300\u0321", 1, input::stream, Joining.with(",").ellipsis("")
                .maxChars(4).cutAfterSymbol());
        checkShortCircuitCollector("maxChars", "a", 1, input::stream, Joining.with(",").ellipsis("").maxChars(3)
                .cutAfterSymbol());

        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300", 1, input::stream,
            Joining.with(",").ellipsis("").maxSymbols(3));
        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321", 1, input::stream, Joining
                .with(",").ellipsis("").maxSymbols(5));
        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a", 2, input::stream,
            Joining.with(",").ellipsis("").maxSymbols(6));
        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a,", 2, input::stream,
            Joining.with(",").ellipsis("").maxSymbols(7));
        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a,a", 2, input::stream,
            Joining.with(",").ellipsis("").maxSymbols(8));
        checkShortCircuitCollector("maxSymbols", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a,aa\u0300\u0321", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(9));

        checkShortCircuitCollector("maxSymbolsBeforeDelimiter", "", 1, input::stream, Joining.with(",").ellipsis("")
                .maxSymbols(5).cutBeforeDelimiter());
        checkShortCircuitCollector("maxSymbolsBeforeDelimiter", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(6).cutBeforeDelimiter());
        checkShortCircuitCollector("maxSymbolsBeforeDelimiter", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(7).cutBeforeDelimiter());
        checkShortCircuitCollector("maxSymbolsBeforeDelimiter", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(8).cutBeforeDelimiter());

        checkShortCircuitCollector("maxSymbolsAfterDelimiter", "", 1, input::stream, Joining.with(",").ellipsis("")
                .maxSymbols(5).cutAfterDelimiter());
        checkShortCircuitCollector("maxSymbolsAfterDelimiter", "", 2, input::stream, Joining.with(",").ellipsis("")
                .maxSymbols(6).cutAfterDelimiter());
        checkShortCircuitCollector("maxSymbolsAfterDelimiter", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a,", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(7).cutAfterDelimiter());
        checkShortCircuitCollector("maxSymbolsAfterDelimiter", "aa\u0300\u0321e\u0300a\u0321a\u0300\u0321a,", 2,
            input::stream, Joining.with(",").ellipsis("").maxSymbols(8).cutAfterDelimiter());
    }
}
