# StreamEx
Enhancing Java 8 Streams.

### Examples

Collector shortcut methods (toList, toSet, groupingBy, joining, etc.)
```java
List<String> userNames = StreamEx.of(users).map(User::getName).toList();
Map<Role, List<User>> role2users = StreamEx.of(users).groupingBy(User::getRole);
StreamEx.of(1,2,3).joining("; "); // "1; 2; 3"
```

Selecting stream elements of specific type
```java
public List<Element> elementsOf(NodeList nodeList) {
    return IntStreamEx.range(0, nodeList.getLength())
      .mapToObj(nodeList::item).select(Element.class).toList();
}
```

Adding elements to stream
```java
public List<String> getDropDownOptions() {
    return StreamEx.of(users).map(User::getName).prepend("(none)").toList();
}

public int[] addValue(int[] arr, int value) {
    return IntStreamEx.of(arr).append(value).toArray();
}
```

Removing unwanted elements and using the stream as Iterable:
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

And more!

### License

This project is licensed under [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

### Installation

Releases are available in [Maven Central](https://repo1.maven.org/maven2/io/github/amaembo/streamex/)

To use from maven add this snippet to the pom.xml `dependencies` section:

```xml
<dependency>
  <groupId>io.github.amaembo</groupId>
  <artifactId>streamex</artifactId>
  <version>0.1.2</version>
</dependency>
```

Pull requests are welcome.
