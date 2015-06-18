# StreamEx changes

### 0.3.3

* Added `StreamEx.intervalMap` method
* Added `StreamEx.runLengths` method
* Added `StreamEx.ofSubLists` method
* Added `MoreCollectors.countingInt` collector
* `StreamEx/EntryStream.maxBy*/minBy*` methods optimized: now keyExtractor function is called at most once per element
* Updated documentation

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
