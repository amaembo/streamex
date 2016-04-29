# StreamEx changes

Check also [MIGRATION.md](MIGRATION.md) for possible compatibility problems.

### 0.6.1

* [#90] Changed: `AbstractStreamEx` class (which shares common functionality of `StreamEx` and `EntryStream`) is public now.
* [#92] Added: `IntStreamEx.of(InputStream)`.
* [#93] Added: `IntStreamEx.asByteInputStream()`.

### 0.6.0
Warning: this release introduces some changes which may break backwards compatibility.

* [#67] Changed: `StreamEx.withFirst()` now includes `(first, first)` pair into result as well. If you want to exclude it, use `.withFirst().skip(1)`. 
* [#70] Fixed: `MoreCollectors.least()/greatest()` now support null element (if the supplied `Comparator` supports nulls).
* [#70] Fixed: `MoreCollectors.least()/greatest()` now returns stable result (the order of equal elements is not changed).
* [#70] Optimized: `MoreCollectors.least()/greatest()` is usually faster now, especially when the selected elements are close to the stream end.
* [#74] Added: `EntryStream.removeKeyValue()`.
* [#77] Added: `MoreCollectors.filtering()/mapping()/flatMapping()` with default downstream Collector `toList()`.
* [#79] Added: `iterate(seed, predicate, op)` for all stream types.
* [#82] Added: `Emitter` class and primitive friends to create non-trivial stream sources.
* [#83] Changed: `StreamEx.of(Collection)`, `of(Iterator)`, etc. now use `? extends T` type instead of just `T`.
* [#85] Added: `StreamEx.mapFirstOrElse()/mapLastOrElse()`.
* [#86] Added: `peekFirst()`, `peekLast()` for all stream types.
* [#89] Added: `produce()` method for all stream types.
* Updated documentation.

### 0.5.5
* [#41] Added: `IntStreamEx/LongStreamEx/StreamEx.without()` accepting several elements.
* [#60] Added: `StreamEx.zipWith()` to zip the stream with another.
* [#63] Optimized `IntStreamEx/LongStreamEx/DoubleStreamEx.reverseSorted()`: much faster now and take less memory.
* [#64] Added: `EntryStream.toMapAndThen()`.
* [#66] Added: `takeWhileInclusive()` for all stream types.
* [#71] Fixed: some collectors like `head(0)`, `greatest(0)` failed when using with JDK Stream.
* [#72] Added: `IntStreamEx.ints()` and `LongStreamEx.longs()`.
* [#73] Added: `EntryStream.collapseKeys()`.

### 0.5.4
* [#10] Added: `chain()` method to all stream types allowing fluently chaining custom operations.
* [#55] TailConcatSpliterator implemented; now all `StreamEx/EntryStream.append/prepend` methods are TSO-compatible. 
* [#56] Fixed: `StreamEx.append/prepend(Collection)` and `EntryStream.append/prepend(Map)` now properly append/prepend if supplied collection is concurrent (so it may be legally modified during the subsequent operations).
* [#57] JDK Stream creation is deferred until necessary. Now quasi-intermediate operations and especially `headTail()` may work faster.
* [#59] Added: `StreamEx.prepend(value)`, `StreamEx.append(value)` which might work faster than existing var-args methods.
* Updated documentation.

### 0.5.3
* [#50] Added: `StreamEx.withFirst()`: extract first stream element 
* [#51] Fixed: `StreamEx.parallel(fjp).runLengths()` fails to run the task in the specified pool.
* [#52] `StreamEx.append(T...)` and `prepend(T...)` are declared as @SafeVarargs and final now.
* [#53] Optimized: `mapFirst`/`mapLast` methods will have less overhead now, especially for primitive streams.
* [#54] Added: `StreamEx.headTail()`: map to the new stream using the first stream element and the stream of the rest elements. 

### 0.5.2
* [#3] Optimized: parallel performance of `StreamEx.ofLines` as well as `StreamEx.split`
* [#19] Optimized: pairMap and forPairs may work faster now, especially in the presence of upstream intermediate operations.
* [#42] Added: `EntryStream.ofTree` methods to stream the tree-like structure tracking nodes depth.
* [#46] Optimized: parallel performance of all `of(Iterator)` methods.
* [#47] Added: `EntryStream.flatMapToKey/flatMapToValue` methods.
* [#48] `EntryStream.of(key, value, key, value...)` now accepts up to 10 pairs.
* Fixed: `StreamEx.of(emptyList().iterator()).parallel()` failed with `NoSuchElementException`.

### 0.5.1

* [#13] Added: `StreamEx.split` to split with single character delimiter.
* [#28] Updated: now `StreamEx.select`, `EntryStream.selectKeys` and `EntryStream.selectValues` accept any type (not necessarily the subtype of current element).
* [#32] Added: `MoreCollectors.dominators` collector which collects the elements to the list leaving only "dominators".   
* [#33] Updated: `StreamEx.split("", pattern)` now returns stream of single `""` string instead of empty stream.
* [#35] Added: construction of all stream types from the `Iterator`.   
* [#36] Added: `StreamEx.of(Enumeration)` static method.
* [#38] Added: `scanLeft` for primitive streams.
* Updated documentation. 

### 0.5.0

Warning: this release introduces some changes which break the backward compatibility and will require the changes in source code if you used the previous StreamEx versions.

* The package `javax.util.streamex` is renamed to `one.util.streamex`. Every occurrence of `javax.util.streamex` in source files must be replaced with `one.util.streamex`.
* The OSGi Bundle-SymbolicName is changed from `javax.util.streamex` to `one.util.streamex`.
* The Maven groupID is changed from `io.github.amaembo` to `one.util`. Dependencies in pom.xml files should be updated accordingly.
* Added: `StreamEx.ofLines(Path)` and `StreamEx.ofLines(Path, Charset)`
* Added: `MoreCollectors.commonPrefix()/commonSuffix()` short-circuiting collectors.
* Added: `IntStreamEx.of(Integer[])`, `LongStreamEx.of(Long[])`, `DoubleStreamEx.of(Double[])` static methods.   
* Deprecated methods `StreamEx.ofEntries()` removed.
* Deprecated methods `collectingAndThen` in primitive collectors removed (use `andThen()` instead).
* Updated documentation. 

### 0.4.1

* Added: `StreamEx/IntStreamEx/LongStreamEx/DoubleStreamEx.mapLast/mapFirst` methods.
* Added: `MoreCollectors.flatMapping` collector.
* Added: `Joining` collector: an advanced version of `Collectors.joining` which may short-circuit.
* Fixed: `StreamEx.cross(mapper)` now correctly handles the case when mapper returns null instead of empty stream.
* Optimized: ordered stateful short-circuit collectors now may process less elements in parallel.
* Optimized: `StreamEx/EntryStream.toList()/toListAndThen()/foldRight()/scanRight()` now faster, especially for sized stream.
* Optimized: collapse-based operations like `StreamEx.collapse/groupRuns/runLengths/intervalMap` now may work faster,
especially when stream has more intermediate operations before them.
* Updated documentation. 

### 0.4.0

* Introduced the concept of short-circuiting collectors.
* `StreamEx/EntryStream.collect(Collector)` method works as short-circuit operation if short-circuiting collector is passed.
* `MoreCollectors.first/head` collectors are short-circuiting now.
* `MoreCollectors.groupingByEnum` collector may short-circuit if downstream collector is short-circuiting.
* `MoreCollectors.pairing` collector may short-circuit if both downstream collectors are short-circuiting.
* Added new short-circuiting collectors: `onlyOne`, `intersecting`, `toEnumSet`, `andingInt`, `andingLong`.
* Added new collectors: `filtering`, `groupingBy` (with domain specification) which short-circuit when downstream collector is short-circuiting.
* Added collectors `mapping`, `collectingAndThen`, `partitioningBy` which mimic standard JDK collectors, but short-circuit when downstream collector is short-circuiting.
* Added `indexOf` methods for all stream types. 
* Added `StreamEx/EntryStream.foldLeft/foldRight` methods without identity argument.
* Added `StreamEx/EntryStream.scanLeft/scanRight` methods without identity argument.
* Added `StreamEx.cartesianProduct/cartesianPower` methods with reduction operator.
* Added `IntStreamEx/LongStreamEx.range/rangeClosed` methods with additional step parameter.
* Added `IntStreamEx/LongStreamEx/DoubleStreamEx.foldLeft` methods.
* Methods `StreamEx/EntryStream.toMap/toSortedMap/toCustomMap` without merge function now produce better exception message in the case of duplicate keys.
* Methods `StreamEx/EntryStream.toMap/toSortedMap/toCustomMap` accepting merge function are not guaranteed to return ConcurrentMap for parallel streams now. They however guarantee now the correct merging order for non-commutative merger functions.
* Methods `StreamEx/EntryStream.grouping*` are not guaranteed to return the ConcurrentMap for parallel streams now. They however guarantee now the correct order of downstream collection.
* Methods `StreamEx.ofEntries` are declared as deprecated and may be removed in future releases!
* Deprecated methods `EntryStream.mapEntryKeys`/`mapEntryValues` are removed!
* Updated documentation

### 0.3.8

* Added `toBooleanArray` collectors (object and primitive).
* Added `MoreCollectors.distinctBy` collector.
* Added `StreamEx/EntryStream.distinct(keyExtractor)` intermediate operation.
* Added `EntryStream.distinctKeys/distinctValues` intermediate operations.
* Added `StreamEx.cartesianPower/cartesianProduct` static methods.
* Optimized: `MoreCollectors.least/greatest` collectors are now much faster (up to 10x depending on input).
* Updated documentation

### 0.3.7

* Added `MoreCollectors.groupingByEnum` collector.
* Added `IntCollector/LongCollector/DoubleCollector.averaging` primitive collectors.
* Added `IntCollector/LongCollector/DoubleCollector.andThen` default methods to replace `collectingAndThen`.
* Added `StreamEx.toFlatCollection` and `StreamEx.toFlatList` terminal operations.
* Added `StreamEx.ofSubLists(list, length, shift)` static method.
* Methods `IntCollector/LongCollector/DoubleCollector.collectingAndThen` are declared as deprecated and may be removed in future releases!
* Updated documentation

### 0.3.6

* Added `StreamEx.collapse(Predicate, Collector)` operation.
* Added `takeWhile` and `dropWhile` methods for all stream types.
* Added `StreamEx.ofPairs` and `EntryStream.ofPairs` methods.
* Optimized: `minBy*/maxBy*` methods for primitive streams now call keyExtractor function at most once per element.
* Updated documentation

### 0.3.5

* Generic arguments relaxed for `StreamEx` methods: `forPairs`, `collapse`, `groupRuns`, `intervalMap`, `sortedBy`.
* Added `MoreCollectors.minIndex/maxIndex` collectors.

### 0.3.4

* Fixed: `EntryStream.of(List<T>)`, `EntryStream.of(T[])` and `StreamEx.runLengths` returned stream
of `Map.Entry` objects which violate the documented contract for `equals` and `hashCode`.
* Fixed: `pairMap` method for all streams worked incorrectly when previous steps included `parallel().flatMap()` due to JDK bug. New version may also work faster in parallel for certain sources.
* Fixed: `collapse`-based methods (`collapse`, `groupRuns`, `runLengths`, `intervalMap`) worked incorrectly in various cases in parallel mode. New version may also work faster in parallel for certain sources.
* Fixed: `minBy*/maxBy*` for primitive streams now return strictly the first matched element (not the arbitrary one). 
* Optimized: `minBy/maxBy` methods for primitive streams now call keyExtractor function at most once per element
* Optimized: many stream creation methods (`zip` for all streams, `EntryStream.of(List)`, `StreamEx.ofSubLists`, etc.) now use custom spliterator.
* Optimized: `IntStreamEx.ofChars` reimplemented for JDK 8 as original `CharSequence.chars` implementation is poor.
* Added construction of all stream types from the `Spliterator`.
* Updated documentation

### 0.3.3

* Added `StreamEx.intervalMap` method
* Added `StreamEx.runLengths` method
* Added `StreamEx.ofSubLists` method
* Added `MoreCollectors.countingInt` collector
* `StreamEx/EntryStream.maxBy*/minBy*` methods optimized: now keyExtractor function is called at most once per element
* `StreamEx.groupRuns` method optimized (up to 5x performance boost depending on data)
* `StreamEx.collapse` methods changed: now the elements passed to the predicate are guaranteed to be two adjacent elements from the source stream.
* Updated documentation: now documentation is automatically copied from JDK for the inherited methods.

### 0.3.2

* Added `MoreCollectors` class: several useful collectors absent in JDK
* Added `skipOrdered(n)` method to every Stream implementation
* Updated documentation

### 0.3.1

* Added `mapToEntry` method for primitive streams
* Added `joining` methods family for primitive streams
* Added `StreamEx.collapse`/`groupRuns` methods
* Added `StreamEx.distinct(atLeast)` method
* Released jar works now as an OSGi bundle
* Updated documentation

### 0.3.0

* Added primitive collectors: `IntCollector`, `LongCollector`, `DoubleCollector`
* Added `flatMapToInt`/`flatMapToLong`/`flatMapToDouble`/`flatMapToObj` to primitive streams
* Added `EntryStream.flatMapKeyValue`/`filterKeyValue`/`mapToKey`/`mapToValue` methods
* Added `IntStreamEx.toCharArray`/`toShortArray`/`toByteArray` methods
* Added `DoubleStreamEx.toFloatArray` method
* Generic arguments for many methods are relaxed allowing more flexible usage
* Methods `EntryStream.mapEntryKeys`/`mapEntryValues` are declared as deprecated and may be removed in future releases!
* Updated documentation

### 0.2.3

* Added `toListAndThen()`, `toSetAndThen()` methods to `StreamEx` and `EntryStream`
* Added `StreamEx.cross()` methods family
* Added `EntryStream.peekKeys()`, `EntryStream.peekValues()`, `EntryStream.peekKeyValue()` methods
* Added construction of `EntryStream` from `List` or array (indices are used as keys)
* Added construction of `EntryStream` from two and three key-value pairs
* Added `EntryStream.append`/`prepend` for two and three key-value pairs
* Added `greater`/`less`/`atLeast`/`atMost` filter methods for primitive streams
* Updated documentation

### 0.2.2

* Fixed: `StreamEx.flatMapToEntry`, `EntryStream.flatMapKeys` and `EntryStream.flatMapValues` now correctly handles null value returned by mapper
* Added `StreamEx.scanRight()` and `EntryStream.scanRight()` methods
* Added `StreamEx.foldRight()` and `EntryStream.foldRight()` methods
* Added `StreamEx.forPairs()` method
* Added `StreamEx.partitioningBy()` methods
* Added `StreamEx.partitioningTo()` method
* Added `StreamEx.groupingTo()` methods
* Added `StreamEx.ofPermutations(int)` constructor
* Added `StreamEx.ofTree` constructors
* Added `StreamEx.without()`, `IntStreamEx.without()`, `LongStreamEx.without()` methods
* Added `EntryStream.join()` methods
* Updated documentation

### 0.2.1

* Fixed: `flatCollection` method now correctly handles null value returned by mapper
* Added `IntStreamEx.charsToString`, `IntStreamEx.codePointsToString` methods
* Added `StreamEx.scanLeft()` and `EntryStream.scanLeft()` methods
* Added construction of `EntryStream` by zipping keys and values from two arrays/lists
* Added construction of `StreamEx`/`IntStreamEx`/`LongStreamEx`/`DoubleStreamEx` by zip-mapping two arrays/lists
* Added `pairMap` method for handling adjacent pairs
* The `append`/`prepend` methods of `StreamEx`/`EntryStream` can accept a `Collection` now 
* Updated documentation

### 0.2.0

* Added `parallel(fjc)` method for all stream types
* Added `StreamEx.reverseSorted()` method
* Added `StreamEx.foldLeft()` and `EntryStream.foldLeft()` methods
* Added `IntStreramEx.toBitSet()` method
* Added construction of `IntStreamEx` from `char[]`, `short[]`, `byte[]` arrays
* Added construction of `DoubleStreamEx` from `float[]` array
* Updated documentation

### 0.1.2

* Added `IntStreamEx.elements` methods family
* Added construction of the constant stream
* Added `minBy`/`maxBy` methods family for primitive streams
* Updated documentation

### 0.1.1

* Fixed: `empty()` method of all the streams worked incorrectly when used several times
* Added `IntStreamEx.ofIndices` methods family
* Added `IntStreamEx.range(int)` and `LongStreamEx.range(long)`
* Added `StreamEx.ofNullable`
* Added construction of the streams from optionals
* Added construction of the streams from array subrange
* Updated documentation

### 0.1.0

Warning: this release introduces some changes which may break backwards compatibility

* `EntryStream.toMap(Supplier)` and `EntryStream.toMap(BinaryOperator, Supplier)` renamed to `toCustomMap`
* Added `StreamEx.toSortedMap` and `EntryStream.toSortedMap` methods family
* Methods producing `Map` use concurrent collector for parallel streams if possible
* Updated documentation
