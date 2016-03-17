# Migration between StreamEx releases

This document describes StreamEx changes which may break the backwards compatibility. For full list of changes see [CHANGES.md](CHANGES.md).

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
