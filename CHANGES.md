# StreamEx changes

### 0.2.1

* Fixed: flatCollection method now correctly handles null value returned by mapper
* Added IntStreamEx.charsToString, IntStreamEx.codePointsToString methods
* Added StreamEx.scanLeft() and EntryStream.scanLeft() methods
* Added construction of EntryStream by zipping keys and values from two arrays/lists
* Added construction of StreamEx/IntStreamEx/LongStreamEx/DoubleStreamEx by zip-mapping two arrays/lists
* Added pairMap method for handling adjacent pairs
* The append/prepend methods of StreamEx/EntryStream can accept a Collection now 
* Updated documentation

### 0.2.0

* Added parallel(fjc) method for all stream types
* Added StreamEx.reverseSorted() method
* Added StreamEx.foldLeft() and EntryStream.foldLeft() methods
* Added IntStreramEx.toBitSet() method
* Added construction of IntStreamEx from char[], short[], byte[] arrays
* Added construction of DoubleStreamEx from float[] array
* Updated documentation

### 0.1.2

* Added IntStreamEx#elements methods family
* Added construction of the constant stream
* Added minBy/maxBy methods family for primitive streams
* Updated documentation

### 0.1.1

* Fixed: empty() method of all the streams worked incorrectly when used several times
* Added IntStreamEx#ofIndices methods family
* Added IntStreamEx#range(int) and LongStreamEx#range(long)
* Added StreamEx#ofNullable
* Added construction of the streams from optionals
* Added construction of the streams from array subrange
* Updated documentation

### 0.1.0

Warning: this release introduces some changes which may break backwards compatibility

* EntryStream#toMap(Supplier) and EntryStream#toMap(BinaryOperator, Supplier) renamed to toCustomMap
* Added StreamEx#toSortedMap and EntryStream#toSortedMap methods family
* Methods producing Map use concurrent collector for parallel streams if possible
* Updated documentation
