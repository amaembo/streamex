# StreamEx 0.8.3
Enhancing the Java Stream API.

[![Maven Central](https://img.shields.io/maven-central/v/one.util/streamex.svg)](https://maven-badges.herokuapp.com/maven-central/one.util/streamex/)
[![Javadocs](https://www.javadoc.io/badge/one.util/streamex.svg)](https://www.javadoc.io/doc/one.util/streamex)
[![Build Status](https://github.com/amaembo/streamex/actions/workflows/test.yml/badge.svg)](https://github.com/amaembo/streamex/actions/workflows/test.yml)
[![Coverage Status](https://coveralls.io/repos/amaembo/streamex/badge.svg?branch=master&service=github)](https://coveralls.io/github/amaembo/streamex?branch=master)

This library defines four classes: `StreamEx`, `IntStreamEx`, `LongStreamEx`, `DoubleStreamEx`
that are fully compatible with the Java 8 stream classes and provide many useful additional methods.
Also, the `EntryStream` class is provided which represents a stream of map entries and provides
additional functionality for this case. Finally, there are some useful new collectors defined in `MoreCollectors`
class as well as the primitive collectors concept.

Full API documentation is available [here](http://amaembo.github.io/streamex/javadoc/).

Take a look at the [Cheatsheet](wiki/CHEATSHEET.md) for brief introduction to StreamEx!

Before updating StreamEx check the [migration notes](wiki/MIGRATION.md) and the full list of [changes](wiki/CHANGES.md).

StreamEx main points are the following:

* Shorter and convenient ways to do common tasks.
* Better interoperability with older code.
* 100% compatibility with the original JDK streams.
* Friendliness for parallel processing: any new feature takes advantage of parallel streams as much as possible.
* Performance and minimal overhead. Whenever StreamEx allows solving a task using less code compared to the standard JDK
Stream API, it should not be significantly slower than the standard way (and sometimes it's even faster).

### Examples

Collector shortcut methods (toList, toSet, groupingBy, joining, etc.)
```java
List<String> userNames = StreamEx.of(users).map(User::getName).toList();
Map<Role, List<User>> role2users = StreamEx.of(users).groupingBy(User::getRole);
StreamEx.of(1,2,3).joining("; "); // "1; 2; 3"
```

Selecting stream elements of a specific type
```java
public List<Element> elementsOf(NodeList nodeList) {
    return IntStreamEx.range(nodeList.getLength())
      .mapToObj(nodeList::item).select(Element.class).toList();
}
```

Adding elements to a stream
```java
public List<String> getDropDownOptions() {
    return StreamEx.of(users).map(User::getName).prepend("(none)").toList();
}

public int[] addValue(int[] arr, int value) {
    return IntStreamEx.of(arr).append(value).toArray();
}
```

Removing unwanted elements and using a stream as an Iterable:
```java
public void copyNonEmptyLines(Reader reader, Writer writer) throws IOException {
    for(String line : StreamEx.ofLines(reader).remove(String::isEmpty)) {
        writer.write(line);
        writer.write(System.lineSeparator());
    }
}
```

Selecting map keys by value predicate:
```java
Map<String, Role> nameToRole;

public Set<String> getEnabledRoleNames() {
    return StreamEx.ofKeys(nameToRole, Role::isEnabled).toSet();
}
```

Operating on key-value pairs:
```java
public Map<String, List<String>> invert(Map<String, List<String>> map) {
    return EntryStream.of(map).flatMapValues(List::stream).invert().grouping();
}

public Map<String, String> stringMap(Map<Object, Object> map) {
    return EntryStream.of(map).mapKeys(String::valueOf)
        .mapValues(String::valueOf).toMap();
}

Map<String, Group> nameToGroup;

public Map<String, List<User>> getGroupMembers(Collection<String> groupNames) {
    return StreamEx.of(groupNames).mapToEntry(nameToGroup::get)
        .nonNullValues().mapValues(Group::getMembers).toMap();
}
```

Pairwise differences:
```java
DoubleStreamEx.of(input).pairMap((a, b) -> b-a).toArray();
```

Support for byte/char/short/float types:
```java
short[] multiply(short[] src, short multiplier) {
    return IntStreamEx.of(src).map(x -> x*multiplier).toShortArray(); 
}
```

Define a custom lazy intermediate operation recursively:
```java
static <T> StreamEx<T> scanLeft(StreamEx<T> input, BinaryOperator<T> operator) {
        return input.headTail((head, tail) -> scanLeft(tail.mapFirst(cur -> operator.apply(head, cur)), operator)
                .prepend(head));
}
```

And more!

### License

This project is licensed under [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

### Installation

Releases are available in [Maven Central](https://repo1.maven.org/maven2/one/util/streamex/)

Before updating StreamEx check the [migration notes](wiki/MIGRATION.md) and the full list of [changes](wiki/CHANGES.md).

#### Maven

Add this snippet to your project's pom.xml `dependencies` section:

```xml
<dependency>
  <groupId>one.util</groupId>
  <artifactId>streamex</artifactId>
  <version>0.8.3</version>
</dependency>
```

#### Gradle 

Add this snippet to your project's build.gradle `dependencies` section:

```groovy
implementation 'one.util:streamex:0.8.3'
```

Pull requests are welcome.
