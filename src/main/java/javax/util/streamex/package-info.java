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
/**
 * This library provides enhancements for Java 8 Stream API. Public API contains the following classes and interfaces:
 * 
 * <p>
 * {@link javax.util.streamex.StreamEx}: implements {@link java.util.stream.Stream} 
 * and provides additional functionality for object streams.
 * 
 * <p>
 * {@link javax.util.streamex.IntStreamEx}, {@link javax.util.streamex.LongStreamEx}, {@link javax.util.streamex.DoubleStreamEx}: 
 * implement corresponding interfaces for primitive streams and enhancing them with additional functionality.
 *  
 * <p>
 * {@link javax.util.streamex.EntryStream}: implements {@link java.util.stream.Stream} of {@link java.util.Map.Entry} 
 * objects providing specific methods for operate on keys or values independently.
 * 
 * <p>
 * Each of these classes contain a bunch of static methods to create the corresponding stream using different sources: 
 * collections, arrays, {@link java.io.Reader}, {@link java.util.Random} and so on.
 * 
 * <p>
 * {@link javax.util.streamex.IntCollector}, {@link javax.util.streamex.LongCollector}, {@link javax.util.streamex.DoubleCollector}: 
 * specialized collectors to work efficiently with primitive streams.
 * 
 * <p>
 * {@link javax.util.streamex.MoreCollectors}: utility class which provides a number of useful collectors 
 * which are absent in JDK {@link java.util.stream.Collectors} class.
 * 
 * <h2><a name="StreamOps">Stream operations and pipelines</a></h2>
 * <p>StreamEx operations are divided into <em>intermediate</em>, <em>quasi-intermediate</em> and
 * <em>terminal</em> operations, and are combined to form <em>stream
 * pipelines</em>.  For more information about <em>intermediate</em> and <em>terminal</em> see the {@linkplain java.util.stream Stream API} documentation.
 * 
 * <p>
 * In addition due to the API limitations new operation type is defined in StreamEx library which called "quasi-intermediate". 
 * In the most of the cases they behave as intermediate operations: for sequential stream there 
 * should be no visible difference between intermediate and quasi-intermediate operation. The only known difference
 * is handling a parallel and unordered stream status. For intermediate operation there's no difference on calling {@code parallel()} 
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
 * like {@linkplain javax.util.streamex.StreamEx#foldLeft(Object, java.util.function.BiFunction) foldLeft} and {@linkplain javax.util.streamex.StreamEx#foldRight(Object, java.util.function.BiFunction) foldRight}. 
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
 * StreamEx provides better support for mutable reduction on primitive streams. There are primitive collector classes {@link javax.util.streamex.IntCollector}, 
 * {@link javax.util.streamex.LongCollector} and {@link javax.util.streamex.DoubleCollector}
 * which extend {@link java.util.stream.Collector} interface, but capable to process primitive streams in more efficient way compared to using the boxed stream.
 * 
 * <p>
 * Also StreamEx library defines a number of collectors absent in JDK. See {@link javax.util.streamex.MoreCollectors} class.
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
package javax.util.streamex;