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
/**
 * This library provides enhancements for Java 8 Stream API. Public API contains the following classes and interfaces:
 * 
 * <p>
 * {@link one.util.streamex.StreamEx}: implements {@link java.util.stream.Stream} 
 * and provides additional functionality for object streams.
 * 
 * <p>
 * {@link one.util.streamex.IntStreamEx}, {@link one.util.streamex.LongStreamEx}, {@link one.util.streamex.DoubleStreamEx}: 
 * implement corresponding interfaces for primitive streams and enhancing them with additional functionality.
 *  
 * <p>
 * {@link one.util.streamex.EntryStream}: implements {@link java.util.stream.Stream} of {@link java.util.Map.Entry} 
 * objects providing specific methods for operate on keys or values independently.
 * 
 * <p>
 * Each of these classes contain a bunch of static methods to create the corresponding stream using different sources: 
 * collections, arrays, {@link java.io.Reader}, {@link java.util.Random} and so on.
 * 
 * <p>
 * {@link one.util.streamex.IntCollector}, {@link one.util.streamex.LongCollector}, {@link one.util.streamex.DoubleCollector}: 
 * specialized collectors to work efficiently with primitive streams.
 * 
 * <p>
 * {@link one.util.streamex.MoreCollectors}: utility class which provides a number of useful collectors 
 * which are absent in JDK {@link java.util.stream.Collectors} class.
 * 
 * <p>
 * {@link one.util.streamex.Joining}: an advanced implementation of joining collector.
 * 
 * <p>
 * {@link one.util.streamex.StreamEx.Emitter}, {@link one.util.streamex.IntStreamEx.IntEmitter}, {@link one.util.streamex.LongStreamEx.LongEmitter}, {@link one.util.streamex.DoubleStreamEx.DoubleEmitter}: 
 * helper interfaces to create custom stream sources.
 *
 * <h2><a name="StreamOps">Stream operations and pipelines</a></h2>
 * <p>StreamEx operations are divided into <em>intermediate</em>, <em>quasi-intermediate</em> and
 * <em>terminal</em> operations, and are combined to form <em>stream
 * pipelines</em>.  For more information about <em>intermediate</em> and <em>terminal</em> see the {@linkplain java.util.stream Stream API} documentation.
 * 
 * <p>
 * In addition, due to the API limitations, a new operation type is defined in StreamEx library which is called "quasi-intermediate". 
 * In most of the cases they behave as intermediate operations: for sequential stream there 
 * should be no visible difference between intermediate and quasi-intermediate operation. The only known difference
 * is when handling a parallel and unordered stream status. For intermediate operation there's no difference on calling {@code parallel()} 
 * before or after any intermediate operation. For quasi-intermediate operations if you call {@code parallel()} after the operation, then previous
 * steps will remain sequential. Similarly if you create a parallel stream, perform some intermediate operations, use quasi-intermediate operation,
 * then call {@code sequential()}, the steps before quasi-intermediate operation may still be executed in parallel.
 * 
 * <p>
 * Also the difference appears if you have an ordered stream source, but an unordered terminal operation (or collect using the unordered collector).
 * If you have only intermediate operations in-between, then all of them will be performed as unordered. However if you have a quasi-intermediate
 * operation, then unordered mode is not propagated through it, so the operations prior to the quasi-intermediate operation 
 * (including the quasi-intermediate operation itself) will remain ordered.
 * 
 * <h3><a name="TSO">Tail stream optimization</a></h3>
 *
 * A few quasi-intermediate operations are tail-stream optimized (TSO) which is important when using 
 * {@link one.util.streamex.StreamEx#headTail(java.util.function.BiFunction) headTail}
 * method recursively. When the TSO-compatible operation understands that it should just pass-through 
 * the rest of the stream as-is, it notifies the surrounding {@code headTail} operation, and {@code headTail} operation 
 * removes the TSO-compatible operation from the pipeline shortening the call stack.
 * This allows writing many recursively defined operations which consume constant amount of the call stack and the heap.
 *
 * <h3><a name="NonInterference">Non-interference</a></h3>
 * 
 * The function is called non-interfering if it does not modify the stream source. For more information see the {@linkplain java.util.stream Stream API} documentation.
 *
 * <h3><a name="Statelessness">Stateless behaviors</a></h3>
 * 
 * The function is called stateless if its result does not depend on any state which may be changed during the stream execution. For more information see the {@linkplain java.util.stream Stream API} documentation.
 *
 * <h2><a name="Reduction">Reduction operations</a></h2>
 * 
 * <p>
 * A <em>reduction</em> operation takes a sequence of input elements and combines them into a single summary result. For more information see the {@linkplain java.util.stream Stream API} documentation.
 * In addition to symmetrical reduction which requires reduction function to be associative, StreamEx library provides asymmetrical reduction methods
 * like {@linkplain one.util.streamex.StreamEx#foldLeft(Object, java.util.function.BiFunction) foldLeft} and {@linkplain one.util.streamex.StreamEx#foldRight(Object, java.util.function.BiFunction) foldRight}. 
 * These methods
 * can be safely used for parallel streams, but the absence of associativity may lead to the performance drawback. Use them only if you cannot provide
 * an associative reduction function.   
 * 
 * <h3><a name="MutableReduction">Mutable reduction</a></h3>
 *
 * <p>
 * A <em>mutable reduction operation</em> accumulates input elements into a
 * mutable result container, such as a {@code Collection},
 * as it processes the elements in the stream. The mutable reduction is usually performed via {@linkplain java.util.stream.Collector collectors}.
 * See the {@linkplain java.util.stream Stream API} documentation for more details.
 * 
 * <p>
 * StreamEx provides better support for mutable reduction on primitive streams. There are primitive collector classes {@link one.util.streamex.IntCollector}, 
 * {@link one.util.streamex.LongCollector} and {@link one.util.streamex.DoubleCollector}
 * which extend {@link java.util.stream.Collector} interface, but capable to process primitive streams in more efficient way compared to using the boxed stream.
 * 
 * <p>
 * Also StreamEx library defines a number of collectors absent in JDK. See {@link one.util.streamex.MoreCollectors} class.
 *
 * <h3><a name="ShortCircuitReduction">Short circuiting reduction</a></h3>
 * 
 * <p>
 * While Stream API has some <em>short-circuiting</em> operations which may process only some of input elements (in particular may allow to process an infinite stream in finite time),
 * a mutable reduction via {@link java.util.stream.Stream#collect(java.util.stream.Collector) Stream.collect(Collector)} is always non short-circuiting.
 * This method is extended in StreamEx library. A new type of collectors is introduced which is called <em>short-circuiting collectors</em>.
 * If such special collector is passed to {@code StreamEx.collect} or {@code EntryStream.collect} terminal operation, then this operation
 * becomes short-circuiting as well. If you however pass such collector to the normal {@code Stream.collect}, it will act as an ordinary 
 * non-short-circuiting collector. For example, this will process only one element from an input stream:
 * 
 * <pre>
 * Optional&lt;Integer&gt; result = IntStreamEx.range(100).boxed().collect(MoreCollectors.first());
 * </pre>
 * 
 * While this will process all the elements producing the same result:
 * 
 * <pre>
 * Optional&lt;Integer&gt; result = IntStream.range(0, 100).boxed().collect(MoreCollectors.first());
 * </pre>
 *
 * <p>
 * Note that when short-circuiting collector is used as the downstream, to standard JDK collectors like {@link java.util.stream.Collectors#mapping(java.util.function.Function, java.util.stream.Collector) Collectors.mapping(Function, Collector)}
 * or {@link java.util.stream.Collectors#partitioningBy(java.util.function.Predicate, java.util.stream.Collector) Collectors.partitioningBy(Predicate, Collector)}, the resulting collector will not be short-circuiting. 
 * Instead you can use the corresponding method from {@code MoreCollectors} class. 
 * For example, this way you can get up to two odd and even numbers from the input stream in short-circuiting manner: 
 *  
 * <pre>
 * Map&lt;Boolean, List&lt;Integer&gt;&gt; map = IntStreamEx.range(0, 100).boxed()
 *                .collect(MoreCollectors.partitioningBy(x -&gt; x % 2 == 0, MoreCollectors.head(2)));
 * </pre>
 * 
 * <p>
 * For some operations like {@code groupingBy} it's impossible to create a short-circuiting collector even if the downstream is short-circuiting, because it's not known whether 
 * all the possible groups are already created.
 * 
 * <p>
 * Currently there's no public API to create user-defined short-circuiting collectors. 
 * Also there are no short-circuiting collectors for primitive streams.
 *
 * <h3><a name="Associativity">Associativity</a></h3>
 *
 * An operator or function {@code op} is <em>associative</em> if the following
 * holds:
 * <pre>{@code
 *     (a op b) op c == a op (b op c)
 * }</pre>
 *
 * For more information see the {@linkplain java.util.stream Stream API} documentation.
 *
 * @author Tagir Valeev
 */
package one.util.streamex;

