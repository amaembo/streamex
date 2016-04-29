# StreamEx Cheatsheet

## Contents

* [Glossary](#glossary)
* [Stream sources](#stream-sources)
* [New intermediate operations](#new-intermediate-operations)
 * [filtering](#filtering)
 * [mapping](#mapping)
 * [flat-mapping](#flat-mapping)
 * [distinct](#distinct)
 * [sorting](#sorting)
 * [partial reduction](#partial-reduction)
 * [concatenate](#concatenate)
 * [peek](#peek)
 * [misc](#misc-intermediate-operations)
* [New terminal operations](#new-terminal-operations)
 * [Collector shortcuts](#collector-shortcuts)
 * [Search](#search)
 * [Folding](#folding)
 * [Primitive operations](#primitive-operations)
 * [forEach-like operations](#foreach-like-operations)
 * [misc](#misc-terminal-operations)
* [Collectors](#collectors)
 * [Basic collectors](#basic-collectors)
 * [Adaptor collectors](#adaptor-collectors)

## Glossary

* *any*: either of `StreamEx`, `IntStreamEx`, `LongStreamEx`, `DoubleStreamEx`, `EntryStream`, sometimes without `EntryStream`.
* *element*: any object or primitive value which appears in the stream pipeline.
* *entry*: a pair of objects usually (but not always) having the `Map.Entry` semantics. The first object in the pair is *key*, the second one is *value*.

## Stream sources

What I want | How to get it
--- | ---
Empty Stream | `any.empty()`
Stream of array, varargs, `Collection`, `Spliterator`, `Iterator`, `Stream` | `any.of()`
Stream of `Enumeration` | `StreamEx.of()`
Stream of boxed `Collection<Integer>`, etc. with unboxing | `IntStreamEx/LongStreamEx/DoubleStreamEx.of()` 
Stream of boxed array `Integer[]`, etc. with unboxing | `IntStreamEx/LongStreamEx/DoubleStreamEx.of()` 
Stream of `byte[]`, `char[]`, `short[]` array | `IntStreamEx.of()`
Stream of `float[]` array | `DoubleStreamEx.of()`
Infinite Stream from `Supplier` | `any.generate()`
Infinite Stream using iterative function | `any.iterate()`
Convert three-argument for loop to Stream | `any.iterate()`
Fixed length Stream of constant elements | `any.constant()`
Stream from mutable object which is left in the known state after full Stream consumption | `any.produce()`
Custom stream source which maintains mutable state | `Emitter/IntEmitter/LongEmitter/DoubleEmitter`
Stream of array or `List` with indices | `EntryStream.of()`
Stream of single value or empty if null supplied | `StreamEx.ofNullable()`
Stream of `Map` keys (with optional values filter) | `StreamEx.ofKeys()`
Stream of `Map` values (with optional keys filter) | `StreamEx.ofValues()`
Stream of `Map` entries or explicit key-value pairs | `EntryStream.of()`
Zip two arrays or lists | `any.zip()`
Split `CharSequence` with regexp | `StreamEx.split()`
Stream of `List` subLists of fixed length | `StreamEx.ofSubLists()`
Stream of all elements of tree-like structure | `StreamEx.ofTree()`
Stream of all elements of tree-like structure tracking the elements depth | `EntryStream.ofTree()`
Stream of all possible pairs of array or `List` elements | `StreamEx/EntryStream.ofPairs()`
Stream of all possible tuples of given length of `Collection` elements | `StreamEx.cartesianPower()`
Stream of all possible tuples of given `Collection` of collections | `StreamEx.cartesianProduct()`
Stream of permutations | `StreamEx.ofPermutations()`
Stream of array or `List` indices (with optional element filter) | `IntStreamEx.ofIndices()`
Stream of range of integral values (with optional step parameter) | `IntStreamEx/LongStreamEx.range()/rangeClosed()`
Stream of increasing `int` or `long` values | `IntStreamEx.ints()`/`LongStreamEx.longs()`
Stream of random numbers | `IntStreamEx/LongStreamEx/DoubleStreamEx.of(Random, ...)`
Stream of `CharSequence` symbols | `IntStreamEx.ofChars()/ofCodePoints()`
Stream of `BitSet` true bits | `IntStreamEx.of(BitSet)`
Stream of lines from file or `Reader` | `StreamEx.ofLines()`
Stream of bytes from the `InputStream` | `IntStreamEx.of(InputStream)`

## New intermediate operations

### filtering

What I want | How to get it
--- | ---
Remove nulls | `StreamEx/EntryStream.nonNull()`
Remove entries which keys or values are null | `EntryStream.nonNullKeys()/nonNullValues()`
Remove elements by predicate | `any.remove()`
Remove given elements | `StreamEx/IntStreamEx/LongStreamEx.without()`
Leave only elements greater/less/at least/at most given value | `IntStreamEx/LongStreamEx/DoubleStreamEx.greater()/atLeast()/less()/atMost()`
Filter entries which keys or values satisfy the predicate | `EntryStream.filterKeys()/filterValues()`
Filter entries applying the `BiPredicate` to key and value | `EntryStream.filterKeyValue()`
Remove entries which keys or values satisfy the predicate | `EntryStream.removeKeys()/removeValues()`
Remove entries applying the `BiPredicate` to key and value | `EntryStream.removeKeyValue()`
Select elements which are instances of given class | `StreamEx.select()`
Select entries which keys or values are instances of given class | `EntryStream.selectKeys()/selectValues()`
Take stream elements while the condition is true | `any.takeWhile()`
Take stream elements while the condition is true including first violating element | `any.takeWhileInclusive()`
Skip stream elements while the condition is true | `any.dropWhile()`

### mapping

What I want | How to get it
--- | ---
Map array or `List` indices to the corresponding elements | `IntStreamEx.elements()`
Map element to the entry | `any.mapToEntry()`
Map entry keys leaving values unchanged | `EntryStream.mapKeys()/mapToKey()`
Map entry values leaving keys unchanged | `EntryStream.mapValues()/mapToValue()`
Map entry key and value using `BiFunction` | `EntryStream.mapKeyValue()`
Swap entry key and value (so entry value becomes key and vice versa) | `EntryStream.invert()`
Drop entry values leaving only keys | `EntryStream.keys()`
Drop entry keys leaving only values | `EntryStream.values()`
Convert every entry to `String` | `EntryStream.join()`
Map pair of adjacent elements to the single element | `any.pairMap()`
Map only first or last element, leaving others as is | `any.mapFirst()/mapLast()`
Map stream element providing special mapper function for the first or last element | `any.mapFirstOrElse()/mapLastOrElse()`
Attach the first stream element to every stream element | `StreamEx.withFirst()`

### flat-mapping

What I want | How to get it
--- | ---
Flat-map primitive stream to the stream of other type | `IntStreamEx/LongStreamEx/DoubleStreamEx. flatMapToInt()/flatMapToLong()/flatMapToDouble()/flatMapToObj()`
Flatten multiple collections to the stream of their elements | `StreamEx/EntryStream.flatCollection()`
Flatten multiple maps to the stream of their entries | `StreamEx.flatMapToEntry()`
Perform cross product of current stream with given array, `Collection` or `Stream` source creating entries | `StreamEx.cross()`
Flat-map entry keys leaving values unchanged | `EntryStream.flatMapKeys()/flatMapToKey()`
Flat-map entry values leaving keys unchanged | `EntryStream.flatMapValues()/flatMapToValue()`
Flat-map entry key and value using `BiFunction` | `EntryStream.flatMapKeyValue()`

### distinct

What I want | How to get it
--- | ---
Leave only distinct elements which appear at least given number of times | `StreamEx.distinct(atLeast)`
Leave distinct elements using custom key extractor | `StreamEx/EntryStream.distinct(keyExtractor)`
Leave only entries having distinct keys | `EntryStream.distinctKeys()`
Leave only entries having distinct values | `EntryStream.distinctValues()`

### sorting

What I want | How to get it
--- | ---
Sort in reverse order | `any.reverseSorted()`
Sort using given key | `any.sortedBy()/sortedByInt()/sortedByLong()/sortedByDouble()`

### partial reduction

Partial reduction is a procedure of combining several adjacent stream elements. Usually `BiPredicate` is used to determine which elements should be combined.
`BiPredicate` is applied to the pair of adjacent elements and returns `true` if they should be combined.

What I want | How to get it
--- | ---
Group some adjacent stream elements into `List` | `StreamEx.groupRuns()`
Reduce some adjacent stream elements using `Collector` or `BinaryOperator` | `StreamEx.collapse()`
Collapse some adjacent stream elements into interval | `StreamEx.intervalMap()`
Remove adjacent duplicate elements counting them | `StreamEx.runLengths()`
Group adjacent entries with equal keys | `EntryStream.collapseKeys()`

### concatenate

What I want | How to get it
--- | ---
Add new elements/`Collection`/`Stream` to the end of existing stream | `any.append()`
Add new elements/`Collection`/`Stream` to the beginning of existing stream | `any.prepend()`

### peek

What I want | How to get it
--- | ---
Peek only entry keys | `EntryStream.peekKeys()`
Peek only entry values | `EntryStream.peekValues()`
Peek entry keys and values using `BiConsumer` | `EntryStream.peekKeyValue()`
Peek only first or last stream element | `any.peekFirst()/peekLast()`

### misc intermediate operations

What I want | How to get it
--- | ---
Extract first stream element and use it to alternate the rest of the stream | `StreamEx.headTail()`
Define almost any custom intermediate operation recursively | `StreamEx.headTail()`
Execute custom-defined operation in fluent manner | `any.chain()`
Work-around parallel stream skip bug prior to Java 8u60 | `any.skipOrdered()`
Perform parallel stream computation using the custom `ForkJoinPool` | `any.parallel(pool)`
Zip two streams together | `StreamEx.zipWith()`

## New terminal operations

### Collector shortcuts

What I want | How to get it
--- | ---
Collect elements to `List`, `Set` or custom `Collection` | `StreamEx/EntryStream.toList()/toSet()/toCollection()`
Collect elements to `List` or `Set` adding custom final step | `StreamEx/EntryStream.toListAndThen()/toSetAndThen()`
Collect elements or entries to `Map` | `StreamEx/EntryStream.toMap()/toSortedMap()`
Collect entries to `Map` adding custom final step | `EntryStream.toMapAndThen()`
Collect entries to custom `Map` | `EntryStream.toCustomMap()`
Partition elements using the `Predicate` | `StreamEx.partitioningBy()/partitioningTo()`
Grouping elements | `StreamEx.groupingBy()/groupingTo()`
Grouping entries | `EntryStream.grouping()/groupingTo()`
Joining elements to `String` | `any.joining()`
Flatten collections and collect them to single final collection | `StreamEx.toFlatList()/toFlatCollection()`
Getting maximal element using custom key extractor | `any.maxBy()/maxByInt()/maxByLong()/maxByDouble()`
Getting minimal element using custom key extractor | `any.minBy()/minByInt()/minByLong()/minByDouble()`

### Search

What I want | How to get it
--- | ---
Check whether stream has given element | `StreamEx/EntryStream/IntStreamEx/LongStreamEx.has()`
Find element satisfying the predicate | `any.findFirst(Predicate)/findAny(Predicate)`
Find the index of element satisfying the predicate or equal to given value | `any.indexOf()`

### Folding

What I want | How to get it
--- | ---
Fold elements left-to-right | `any.foldLeft()`
Fold elements right-to-left | `StreamEx/EntryStream.foldRight()`
Get `List` of cumulative prefixes or suffixes | `StreamEx/EntryStream.scanLeft()/scanRight()`
Get primitive array of cumulative prefixes | `IntStreamEx/LongStreamEx/DoubleStreamEx.scanLeft()`

### Primitive operations

What I want | How to get it
--- | ---
Collect `IntStreamEx` to `byte[]`, `char[]` or `short[]` | `IntStreamEx.toByteArray()/toCharArray()/toShortArray()`
Collect `IntStreamEx` to `BitSet` | `IntStreamEx.toBitSet()`
Collect `DoubleStreamEx` to `float[]` | `DoubleStreamEx.toFloatArray()`
Collect stream of chars or codepoints to `String` | `IntStreamEx.charsToString()/codePointsToString()`

### forEach-like operations

What I want | How to get it
--- | ---
Perform operation on every adjacent pair of elements | `StreamEx.forPairs()`
Perform operation on entry key and value using `BiConsumer` | `EntryStream.forKeyValue()`

### misc terminal operations

What I want | How to get it
--- | ---
Convert `IntStreamEx` to `InputStream` | `IntStreamEx.asByteInputStream()`

## Collectors

### Basic collectors

What I want | How to get it
--- | ---
Collect to array | `MoreCollectors.toArray()`
Collect to boolean array using the `Predicate` applied to each element | `MoreCollectors.toBooleanArray()`
Collect to `EnumSet` | `MoreCollectors.toEnumSet()`
Count number of distinct elements using custom key extractor | `MoreCollectors.distinctCount()`
Get the `List` of distinct elements using custom key extractor | `MoreCollectors.distinctBy()`
Simply counting, but get the result as `Integer` | `MoreCollectors.countingInt()`
Get the first or last element only | `MoreCollectors.first()/last()`
Get the element only if there's exactly one element | `MoreCollectors.onlyOne()`
Get the given number of first or last elements in the `List` | `MoreCollectors.head()/tail()`
Get the given number of greatest/least elements according to the given `Comparator` or natural order | `MoreCollectors.greatest()/least()`
Get all the maximal or minimal elements according to the given `Comparator` or natural order | `MoreCollectors.maxAll()/minAll()`
Get the index of maximal or minimal element according to the given `Comparator` or natural order | `MoreCollectors.minIndex()/maxIndex()`
Get the intersection of input collections | `MoreCollectors.intersecting()`
Get the result bitwise-and operation | `MoreCollectors.andingInt()/andingLong()`
Join the elements into string with possible limit to the string length (adding ellipsis if necessary) | `Joining.with()`
Perform a group-by with the specified keys domain, so every key is initialized even if absent in the input | `MoreCollectors.groupingBy()/groupingByEnum()`
Partition input according to the `Predicate` | `MoreCollectors.partitioningBy()`
Get the common prefix or common suffix `String` of input elements | `MoreCollectors.commonPrefix()/commonSuffix()`
Get the list of input elements removing the elements which follow their dominator element | `MoreCollectors.dominators()`

### Adaptor collectors

What I want | How to get it
--- | ---
Collect using two independent collectors | `MoreCollectors.pairing()`
Filter the input before passing to the collector | `MoreCollectors.filtering()`
Map the input before passing to the collector | `MoreCollectors.mapping()`
Flat-map the input before passing to the collector | `MoreCollectors.flatMapping()`
Perform a custom final operation after the collection finishes | `MoreCollectors.collectingAndThen()`
