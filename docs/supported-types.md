<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=83b36737-54e7-41bf-a2f6-44170b3048f1" />

# supported-types

How approvalcrest serialises common Java types to JSON.

## Primitives and Strings

Approvalcrest serialises them as their natural JSON values (`42`, `true`, `"hello"`).

## Date and Time — Legacy

| Type | Example serialised form |
|---|---|
| `java.util.Date` | `"1970-01-01T00:00:13.000Z"` |
| `java.sql.Date` | `"1970-01-01T00:00:13.000Z"` |
| `java.sql.Timestamp` | `"1970-01-01T00:00:13.000Z"` |

Approvalcrest serialises all of them as ISO-8601 UTC strings.

## Date and Time — `java.time.*`

| Type | Example serialised form |
|---|---|
| `Instant` | `"1970-01-01T00:00:00.042Z"` |
| `LocalDate` | `"2019-04-01"` |
| `LocalDateTime` | `"2020-04-21T18:38:15.000000013"` |
| `LocalTime` | `"18:45:13.000000125"` |
| `OffsetDateTime` | `"2020-04-21T20:54:15.000000013Z"` |
| `OffsetTime` | `"21:58:17.000000163Z"` |
| `ZonedDateTime` | `"2020-04-01T22:01:02.000000003Z[UTC]"` |

For the comparison, approvalcrest normalises `ZonedDateTime` and `OffsetDateTime` to UTC. The comparison treats two values that show the same instant in different time zones as equal. Two values at different instants are not equal, and the assertion fails.

## Set

A `Set` has no stable iteration order. Thus approvalcrest sorts its elements by their JSON representation before it writes them. The sort changes only the order, and keeps all the elements. This includes elements that are not `equals()` to each other, but serialise identically.

Such elements occur with classes that do not override `equals()`. There, two instances with the same values are different members of the set. It also occurs when approvalcrest does not serialise the field that makes two instances different, for example a `transient` field.

Before 1.5.0, approvalcrest merged those elements into one entry. A set that lost or got such an element then serialised the same as the original set, so the difference could not fail a test. If you must re-approve many approved files, set `-DapprovalcrestLegacySetCollapse=true` to get the old behaviour for a transition period. See [system-properties](system-properties.md).

## Optional

| Value | Serialised form |
|---|---|
| `java.util.Optional.empty()` | `{}` |
| `java.util.Optional.of(13L)` | `{"value": 13}` |
| `java.util.OptionalInt.empty()` | `{}` |
| `java.util.OptionalInt.of(42)` | `{"value": 42}` |
| `java.util.OptionalLong.empty()` | `{}` |
| `java.util.OptionalLong.of(99L)` | `{"value": 99}` |
| `java.util.OptionalDouble.empty()` | `{}` |
| `java.util.OptionalDouble.of(3.14)` | `{"value": 3.14}` |
| `com.google.common.base.Optional.absent()` | `{}` |
| `com.google.common.base.Optional.of(13L)` | `{"reference": 13}` |

### Interface, abstract and polymorphic types inside Optional

Sometimes the declared type parameter of an `Optional` is an interface, an abstract class or a concrete base class. Then the serialiser uses the **runtime type** of the value in the `Optional`. Thus the output includes all the fields of the actual implementation:

```java
Optional<Drawable> opt = Optional.of(new Circle("red", 5.0));
// Serialises all Circle fields (color, radius), not just Drawable methods
```

In detail:
- `Optional<SomeInterface>` — the serialiser uses the runtime type of the value
- `Optional<AbstractBase>` — the serialiser uses the runtime type of the value
- `Optional<ConcreteBase>` that holds a subclass — the serialiser uses the runtime type (the subclass), and keeps the fields of the subclass
- `Optional<ConcreteBase>` that holds an instance of `ConcreteBase` — the serialiser uses the declared type. Here the declared type and the runtime type are the same, so the output does not change

### Configuring additional types to skip in fallback factories

You can register a custom `TypeAdapter` or `TypeAdapterFactory` for a library type, for example the Vavr types `Option`, `Either` or `List`. Then it can be necessary to tell the internal fallback factories of approvalcrest not to handle that type. Use `addTypeToSkipInFallbackFactories`:

```java
GsonConfiguration config = new GsonConfiguration();
config.addTypeToSkipInFallbackFactories(io.vavr.control.Option.class);
config.addTypeAdapterFactory(myVavrOptionFactory);
```

Then the `UnsafeFieldTypeAdapterFactory` and the `GetterBasedTypeAdapterFactory` do not handle the specified type, and your custom adapter has priority.

## `java.nio.file.Path`

Approvalcrest serialises a `Path` as a string:

```json
"/something/anything"
```

## `Class<?>`

Approvalcrest serialises a `Class<?>` as the fully qualified class name:

```json
"com.example.MyClass"
```

## `Throwable`

Approvalcrest serialises a `Throwable` with the fields below. When the JDK denies the `sun.misc.Unsafe` memory-access methods, it uses getter-based property names, unless you attach the [agent](field-access-modes.md#the-agent). See [field-access-modes](field-access-modes.md).

```json
{
  "detailMessage": "something went wrong",
  "cause": { ... },
  "suppressedExceptions": [],
  "class": "java.lang.RuntimeException"
}
```

## Circular References

Approvalcrest handles circular object graphs automatically. It serialises each object instance one time. It replaces each later reference to the same instance with a `0xN` pointer:

```json
{
  "parent": {
    "0x1": {
      "children": [
        { "parent": "0x1", "childAttribute": "child1" }
      ],
      "parentAttribute": "parent"
    }
  }
}
```

You do not have to configure anything. Approvalcrest finds cycles automatically.

## Custom Types

For a type that approvalcrest does not handle natively, register a custom Gson adapter with `withGsonConfiguration`. You can also use this to change how approvalcrest serialises any type. For example, you can serialise a class as one string, not as a JSON object:

```java
import com.github.karsaig.approvalcrest.matcher.GsonConfiguration;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import java.util.UUID;

GsonConfiguration gsonConfig = new GsonConfiguration();
gsonConfig.addTypeAdapter(UUID.class,
    (JsonSerializer<UUID>) (src, type, ctx) -> new JsonPrimitive(src.toString()));

assertThat(actual, sameJsonAsApproved()
    .withGsonConfiguration(gsonConfig));
```

`GsonConfiguration` has three methods to register an adapter:

| Method | When to use |
|---|---|
| `addTypeAdapter(Type, Object)` | Exact type — `UUID.class`, `MyValueType.class` |
| `addTypeHierarchyAdapter(Class<?>, Object)` | The type and all its subclasses — for example, for interfaces or abstract base types |
| `addTypeAdapterFactory(TypeAdapterFactory)` | When you must examine the type at resolution time |

### Custom types and aliasing

Aliases apply only to JSON strings and JSON numbers. When a custom adapter serialises a type as a JSON **string**, you can alias the value of that field:

```java
// UUID serialised as a string → value can be aliased
GsonConfiguration gsonConfig = new GsonConfiguration();
gsonConfig.addTypeAdapter(UUID.class,
    (JsonSerializer<UUID>) (src, type, ctx) -> new JsonPrimitive(src.toString()));

assertThat(actual, sameJsonAsApproved()
    .withGsonConfiguration(gsonConfig)
    .withAlias("orderId", "550e8400-e29b-41d4-a716-446655440000", "<orderId>"));
```

If the type stays a Gson JSON object, aliases do not apply to it. A JSON object is the default for class instances.

## Related

- [same-bean-as](same-bean-as.md)
- [same-json-as-approved](same-json-as-approved.md)
