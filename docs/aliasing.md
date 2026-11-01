<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=4629c939-407c-461a-bbee-8b42aefcef3a" />

# aliasing

Replace raw values that change from run to run with stable aliases in approved files.

With an alias, the approved file contains a placeholder that a person can read, for example `<userId>`. It does not contain the raw value, which changes on each run. The approved file then describes the field, and does not depend on one raw value.

Aliases work with `sameJsonAsApproved` and `sameBeanAs`.

**Applies to strings and numbers only.** Aliases match `String` values and numeric values. For the match, the matcher uses the string form of a number, so `13` matches the rule for `"13"`. The matcher never replaces a Boolean value (`true`/`false`) or a `null` value with an alias.

By default, you cannot alias a custom type that serialises as a JSON object. To alias such a type, register a custom Gson adapter that serialises the type as a JSON string (see [supported-types](supported-types.md#custom-types)).

## By Value (Global)

The matcher replaces each occurrence of the raw value in the full serialised output with the alias:

```java
assertThat(actual, sameJsonAsApproved()
    .withAlias("runtime-uuid-1234", "<userId>"));
```

In the approved file:

```json
{
  "userId": "<userId>"
}
```

On the first run, the matcher applies the alias before it writes the `-not-approved` file. Thus the file that you review and approve already contains `<userId>`. The raw value does not go into source control.

## By Field and Value (Scoped)

Replace the value only when it is in a specific named field. This is useful when the same raw value is in several fields, and you want to alias only one of them:

```java
// Replaces "volatile-uuid-123" only in the field named "userId";
// the same value in any other field is left untouched
assertThat(actual, sameJsonAsApproved()
    .withAlias("userId", "volatile-uuid-123", "<userId>"));
```

## Multiple Aliases

`withAlias` calls accumulate. Each call adds a new rule. When two rules conflict, the later rule wins:

```java
assertThat(actual, sameJsonAsApproved()
    .withAlias("uuid-111", "<orderId>")
    .withAlias("uuid-222", "<customerId>"));
```

## Batch Rules with `AliasMap`

`AliasMap` holds several rules. Pass it to the matcher with `withAliasMap`. Several `withAliasMap` calls accumulate, and they do not replace each other.

### Simple value replacement

```java
import com.github.karsaig.approvalcrest.matcher.alias.AliasMap;

AliasMap aliases = AliasMap.builder()
    .add("runtime-uuid-1234", "<userId>")
    .add("secret-token-abc",  "<token>")
    .build();

assertThat(actual, sameJsonAsApproved()
    .withAliasMap(aliases));
```

### Field-scoped rule in `AliasMap`

```java
AliasMap aliases = AliasMap.builder()
    .add("userId",  "volatile-uuid-1", "<userId>")
    .add("orderId", "volatile-uuid-2", "<orderId>")
    .build();
```

### Dynamic alias via resolver function

Instead of a static alias string, supply a `Function<String, String>` that receives the raw value and returns the alias. This is useful when the alias needs information from the value:

```java
AliasMap aliases = AliasMap.builder()
    // Captures the numeric suffix: "user-42" → "<user:42>"
    .add("user-42", v -> "<user:" + v.split("-")[1] + ">")
    .build();
```

### Regex-based field and value matching

Use `addByPattern` to match field names and values with regular expressions:

```java
import java.util.regex.Pattern;

AliasMap aliases = AliasMap.builder()
    // Replace any value matching the date-time pattern, in any field containing "Date"
    .addByPattern(
        Pattern.compile(".*Date"),
        Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.*"),
        "<timestamp>")
    .build();
```

To alias all values in a field whose name matches a pattern, use `addByPattern` with a resolver function. The value can be anything:

```java
AliasMap aliases = AliasMap.builder()
    // All fields ending in "Id" or "id": alias each value, preserving a short prefix
    .addByPattern(
        Pattern.compile(".*[Ii]d$"),
        v -> "<id:" + v.substring(0, Math.min(4, v.length())) + "...>")
    .build();
```

To match only a value pattern, in any field, use the fluent `entry()` builder below.

### Full predicate entry builder

For the most complex rules, use the fluent `entry()` builder. All constraints are optional. Leave out the constraints that you do not need:

```java
AliasMap aliases = AliasMap.builder()
    .entry()
        .path(p -> p.startsWith("response"))     // JSON path to the field (optional)
        .field("id")                              // exact field name (optional)
        .valuePattern(Pattern.compile("\\d{8}"))  // value regex (optional)
        .alias("<responseId>")                    // static alias or Function<String,String>
    .register()
    .build();
```

Available predicates on `EntryBuilder`:
- `.path(Predicate<String>)` — matches the full dot-path to the value (e.g. `"order.lineItems.productId"`)
- `.field(String)` — exact field name
- `.field(Predicate<String>)` — any predicate on the field name
- `.fieldPattern(Pattern)` — field name regex
- `.value(String)` — exact value
- `.value(Predicate<String>)` — any predicate on the value
- `.valuePattern(Pattern)` — value regex
- `.alias(String)` — static alias
- `.alias(Function<String, String>)` — dynamic resolver

## Behaviour with `sameBeanAs`

With file-based matchers (`sameJsonAsApproved`), the matcher applies the aliases before it writes the approved file or compares with it. Only the alias goes into source control.

With `sameBeanAs`, the matcher applies the aliases to the **actual** object and to the **expected** object before the comparison. Thus two objects with different raw values in the same field can match, if both values map to the same alias:

```java
// actual.userId = "uuid-111", expected.userId = "uuid-999"
// Both are aliased to "<userId>" before comparison → test passes
assertThat(actual, sameBeanAs(expected)
    .withAlias("uuid-111", "<userId>")
    .withAlias("uuid-999", "<userId>"));
```

## Interaction with Sorting

The matcher applies aliases **before** it sorts. Aliases can change the sort order of two elements. Then the approved file must show the order of the aliased values. Remember this when you use `withAlias` together with `sortField`.

## Last-registered Wins

When two rules match the same value, the **last registered** rule wins. This applies to all `withAlias` and `withAliasMap` calls together:

```java
// "<override>" wins because it was registered last
assertThat(actual, sameJsonAsApproved()
    .withAliasMap(baseAliases)      // contains: "shared-value" → "<base>"
    .withAliasMap(overrideAliases)  // contains: "shared-value" → "<override>"
);
```

## Related

- [dynamic-values](dynamic-values.md) — how to select ignore, alias or custom matching
- [supported-types](supported-types.md) — how approvalcrest serialises types; how to register custom Gson adapters
