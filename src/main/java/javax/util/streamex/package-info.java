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
 * This library provides enhancements for Java 8 Stream API. Public API contains five classes and three interfaces:
 * 
 * <p>
 * {@link javax.util.streamex.StreamEx}: implements {@link java.util.stream.Stream} and provides additional functionality for object streams.
 * 
 * <p>
 * {@link javax.util.streamex.IntStreamEx}, {@link javax.util.streamex.LongStreamEx}, {@link javax.util.streamex.DoubleStreamEx}: implement corresponding interfaces for primitive streams and enhancing them with additional functionality.
 *  
 * <p>
 * {@link javax.util.streamex.EntryStream}: implements {@link java.util.stream.Stream} of {@link java.util.Map.Entry} objects providing specific methods for operate on keys or values independently.
 * 
 * <p>
 * Each of these classes contain a bunch of static methods to create the corresponding stream using different sources: collections, arrays, {@link java.io.Reader}, {@link java.util.Random} and so on.
 * 
 * <p>
 * {@link javax.util.streamex.IntCollector}, {@link javax.util.streamex.LongCollector}, {@link javax.util.streamex.DoubleCollector}: specialized collectors to work efficiently with primitive streams.
 * 
 * <p>
 * Most of new stream operations are either intermediate or terminal like it's defined in Java 8 Stream API. However due to the API limitations
 * a few operations are called "quasi-intermediate". In the most of the cases they behave as intermediate operations: for sequential stream there 
 * should be no visible difference between intermediate and quasi-intermediate operation. The only known difference
 * is handling a parallel stream status. For intermediate operation there's no difference on calling {@code parallel()} 
 * before or after any intermediate operation. For quasi-intermediate operations if you call {@code parallel()} after the operation, then previous
 * steps will remain sequential. Similarly if you create a parallel stream, perform some intermediate operations, use quasi-intermediate operation,
 * then call {@code sequential()}, the steps before quasi-intermediate operation may still be executed in parallel. 
 * 
 * @author Tagir Valeev
 */
package javax.util.streamex;