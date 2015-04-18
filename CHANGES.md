# StreamEx changes

### 0.1.0

Warning: this release introduces some changes which may break backwards compatibility

* EntryStream#toMap(Supplier) and EntryStream#toMap(BinaryOperator, Supplier) renamed to toCustomMap
* Added StreamEx#toSortedMap and EntryStream#toSortedMap methods family
* Methods producing Map use concurrent collector for parallel streams if possible
* Updated documentation
