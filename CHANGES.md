# StreamEx changes

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
