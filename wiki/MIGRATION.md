# Migration between StreamEx releases

This document describes StreamEx changes which may break the backwards compatibility. For full list of changes see [CHANGES.md](CHANGES.md).

### 0.8.0
Issue#244: To align with Java 16 `Stream.toList` specification, we no more guarantee that the results of 
`AbstractStreamEx.toList` and `AbstractStreamEx.toSet` are mutable. They are still mutable by default 
but this will change in future release. You can switch their implementation to immutable, using temporary 
system property `-Dstreamex.default.immutable=true` and test how your code behaves. If you actually need mutability, 
use new methods `AbstractStreamEx.toMutableList` and `AbstractStreamEx.toMutableSet`.

### 0.7.3
Issue#219: Now many collectors defined in `MoreCollectors` may throw NullPointerException more eagerly, before the
 collector is used. Also, `MoreCollectors.last` now throws NullPointerException if the last element is null (this
  makes it conformant to `MoreCollectors.first`).

### 0.7.0
Issue#194: Method `skipOrdered` is removed, which may break source and binary compatibility if in was used. Use simply `skip` instead.
PR#200: Overloads added to `EntryStream.allMatch/anyMatch/noneMatch` which accept a `BiPredicate`. Such overloads may cause
    source incompatibility in rare case when a method reference was used which is either bound to var-arg method or
    has several overloads. E.g. assuming `boolean allNull(Object... objects)` now `EntryStream.of(...).allMatch(Utils::allNull)`
    won't compile anymore. To work-around this issue use a lambda instead of method reference.

### 0.6.0

Issue#67: Now `StreamEx.withFirst()` as well as `StreamEx.withFirst(BinaryOperator)` include `(first, first)` pair. If you used these operations in StreamEx 0.5.3-0.5.5, you should update the existing code: replace `.withFirst()` with `.withFirst().skip(1)`.

### 0.5.5

Issue#41: As `StreamEx.without(T...)` was added, the existing code may become ambiguous now. If you've used `.without(null)` before, replace it with dedicated `.nonNull()` operation.

Issue#63: `DoubleStreamEx.reverseSorted()` may change the order of non-canonical `NaN` values (actually sorting them).

### 0.5.3

Issue#52: `StreamEx.append(T...)` and `prepend(T...)` are final now, so if you extend `StreamEx` class, you cannot override them anymore. 

### 0.5.0

Issue#8: The package `javax.util.streamex` is renamed to `one.util.streamex`. The OSGi bundle name and Maven groupId are changed correspondingly. To migrate to StreamEx 0.5.0 you should:

* Replace every occurrence of `javax.util.streamex` to `one.util.streamex` in your Java files and OSGi manifests.
* Replace `io.github.amaembo` groupID with `one.util` in your build files (pom.xml, ivy.xml, build.gradle, etc.)
