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
 * @author Tagir Valeev
 */
package javax.util.streamex;