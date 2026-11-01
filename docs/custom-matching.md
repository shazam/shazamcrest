<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=4e9babdf-6a30-4e7b-a09d-f12bf35f3a1f" />

# custom-matching

Assert a field with a custom Hamcrest matcher, instead of the comparison of that field or in addition to it.

## Basic Usage

Use `.with(String path, Matcher<T> matcher)` to assert a structural property of a field, not an exact value. `sameBeanAs` and `sameJsonAsApproved` still compare all other fields as usual: `sameBeanAs` with the expected object, and `sameJsonAsApproved` with the approved file.

You cannot predict the exact value of some fields, for example a generated ID. Use this to assert the shape of such a field:

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;
import static org.hamcrest.Matchers.notNullValue;

// The id field must be present but its exact value is not pinned in the approved file
assertThat(actual, sameJsonAsApproved()
    .with("id", notNullValue()));
```

With `sameBeanAs`, the custom matcher replaces the comparison of the field at that path. `sameBeanAs` compares all other fields with `expected`:

```java
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;
import static org.hamcrest.Matchers.startsWith;

assertThat(actual, sameBeanAs(expected)
    .with("address.streetName", startsWith("Via")));
```

## Asserting *in addition to* the comparison

`.with(...)` removes the field. This is correct for a value that you cannot predict. Sometimes you *can* predict the value, but want a stronger guarantee about it. Then use `.alsoCheck(String path, Matcher<T> matcher)`. The field stays in the comparison, and the custom matcher also runs:

```java
import static org.hamcrest.Matchers.greaterThan;

// score must still equal the value in the approved file, AND be positive
assertThat(actual, sameJsonAsApproved()
    .alsoCheck("score", greaterThan(0L)));
```

(The `0L` is not a typo. An ordering matcher compares only values of the same boxed type, and a whole number often gets to the matcher as a `Long`. See [Numbers and boxing](#numbers-and-boxing).)

The difference is what gets into the approved file. With `.with("score", ...)`, approvalcrest never writes the field. Thus, if the score later changes from 5 to 5000, this change cannot make the test fail. With `.alsoCheck("score", ...)`, approvalcrest writes the field and compares it as usual.

It works in the same way for `sameBeanAs`, where the expected object takes the place of the approved file. `.with` removes the field from both sides before the comparison. `.alsoCheck` keeps it.

```java
// address.streetName must equal expected's value AND start with "Via"
assertThat(actual, sameBeanAs(expected)
    .alsoCheck("address.streetName", startsWith("Via")));
```

| | in the approved file | comparison | custom matcher |
|---|---|---|---|
| `.with(path, m)` | removed | skipped for that field | runs |
| `.alsoCheck(path, m)` | kept | runs | runs |

`.alsoCheckMatching(namePattern, matcher)` is the same choice for the name-pattern form of [`.withMatcher(...)`](#match-all-fields-whose-name-matches-a-pattern).

All the other content of this page applies to both modes in the same way. This includes the path syntax, fan-out through collections, the `*` wildcard, container matchers, and the behaviour of a path at a null. Both modes register in the same place, and the same code evaluates them. Only the removal is different.

You can register the same path in both ways, and **the last call wins**. An explicit `.ignoring(path)` still removes the field, whatever the order of the two calls is.

**Two limitations.**

- **You cannot undo the pattern form.** Patterns accumulate, and two matcher instances are never equal. Thus `.withMatcher(is("tags"), m1).alsoCheckMatching(is("tags"), m2)` still removes `tags`. Only the path form has last-call-wins.
- **A field that fails both checks gets one report.** Approvalcrest evaluates custom matchers before the content comparison, and stops at the first failure. Thus, if a field fails its matcher and also differs from the approved file, the report shows only the matcher mismatch.

**If you change a `.with(...)` call to `.alsoCheck(...)`, regenerate the approved file.** Approvalcrest wrote that file when `.with(...)` removed the field. Thus the file never contained the field. Re-run with `-DfileMatcherUpdateInPlace=true`. This works if the new matcher passes, because approvalcrest applies the matcher before it regenerates the approved file. If the matcher fails, approvalcrest does not regenerate the file.

## Chaining with `.ignoring()`

```java
import static org.hamcrest.Matchers.notNullValue;

assertThat(actual, sameBeanAs(expected)
    .ignoring("transientId")
    .with("createdAt", notNullValue()));
```

## Common Patterns

```java
// Field must not be null
assertThat(actual, sameJsonAsApproved()
    .with("id", notNullValue()));

// Field matches a regex pattern
assertThat(actual, sameJsonAsApproved()
    .with("email", matchesPattern(".*@example\\.com")));

// Field is within a numeric range
assertThat(actual, sameJsonAsApproved()
    .with("score", both(greaterThan(0)).and(lessThan(100))));

// Field has a minimum length
assertThat(actual, sameJsonAsApproved()
    .with("description", hasLength(greaterThan(0))));

// Nested field must not be null
assertThat(actual, sameJsonAsApproved()
    .with("order.trackingCode", notNullValue()));

// Path through a collection (fan-out): asserts trackingCode is non-null in EVERY order
assertThat(actual, sameJsonAsApproved()
    .with("orders.trackingCode", notNullValue()));

// Assert the collection itself (not each element) — orders must not be empty
assertThat(actual, sameJsonAsApproved()
    .with("orders", not(empty())));
```

## Paths Through Collections (Fan-out)

When a path segment resolves to a collection or an array, `.with()` **fans out**. It applies the matcher to the resolved value **in every element** of that collection.

```java
// 'orders' is a List<Order>; each Order has a 'trackingCode' field.
// The matcher must pass on trackingCode in EVERY order.
assertThat(actual, sameJsonAsApproved()
    .with("orders.trackingCode", notNullValue()));
```

The two rules that follow also apply, in the same way, to the `*` segment below.

**The match is strict:** the matcher must pass on **every** value that the path resolved to. If one value fails, the full assertion fails, and the message identifies the first element that failed.

**Resolution is lenient:** path resolution skips elements that do not have the field. This is not a failure. Thus, in a list where only some orders have `trackingCode`, the matcher applies to the orders that have it, and the assertion can pass.

Only a path that resolves against **no** element is an error. Because of the skip, a path can get to the array-of-entries form of a map. But in a heterogeneous collection, the skip also makes your assertion narrower, and nothing tells you. When this is important, also assert the size of the container.

**An empty collection fails:** if the collection that the path fans out over is empty, the assertion fails. This prevents a pass that checks nothing, when a list is unexpectedly empty.

Fan-out is recursive. If several path segments are collections, it applies at each level:

```java
// orders → List<Order>, each Order has items → List<Item>
// Every item in every order must have a non-null sku
assertThat(actual, sameJsonAsApproved()
    .with("orders.items.sku", notNullValue()));
```

Fan-out applies to `Collection` fields and to Java arrays. A `Map` is different: the path does not fan out over it, but **traverses it by key**. The key is a path segment. Name the entry that you want:

```java
// The trackingCode of the order stored under key "A-1"
assertThat(actual, sameJsonAsApproved()
    .with("ordersByRef.A-1.trackingCode", notNullValue()));
```

A path without the key does not get to the values. There is no entry with the name `trackingCode`, so approvalcrest rejects the path:

```java
// throws IllegalArgumentException: "ordersByRef.trackingCode does not exist"
assertThat(actual, sameJsonAsApproved()
    .with("ordersByRef.trackingCode", notNullValue()));
```

To assert something about the full map, and not one entry, point the path to the map. Then use a map matcher. See below.

### Every value: the `*` segment

You can name a key when you know it. When you want the same assertion for every entry, use `*`:

```java
// The trackingCode of EVERY order in the map must be non-null
assertThat(actual, sameJsonAsApproved()
    .with("ordersByRef.*.trackingCode", notNullValue()));
```

`*` means **every named child at that position**: a map key, an object property or a bean field. It works in the same way in `.ignoring(path)`, so the two agree:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring("ordersByRef.*.generatedId"));
```

Things to know:

- **`*` is a wildcard only in a segment that is not the last.** As the last segment, it keeps its usual meaning: a key with the literal name `*`. A JSON document or a `Map<String,?>` can have such a key. Thus `.ignoring("headers.*")` removes that key. `.ignoring("ordersByRef.*")` also looks for such a key, and does not mean "every value". To assert something about the full map, point the path to the map field.
- **`*` is not necessary for collections or arrays.** The path already goes through them, so `orders.trackingCode` already means "in every order". `orders.*.trackingCode` is a *different* query, not a synonym. It means "any named field of each order, then trackingCode".
- **A `*` that matches nothing is an error** for `.with(...)`. Thus a path with a typo cannot pass because it matches nothing. For `.ignoring(...)`, it does nothing, as does any ignore path that matches nothing.
- **Strict match, lenient resolution.** These are the same two rules as for the collection fan-out above. Every child that the path resolves against must pass. Path resolution skips children that do not have the rest of the path.
- **A scalar child is not an error.** Path resolution skips it. A bean has scalar fields and object fields. Thus `*.trackingCode` skips the scalars and applies to the objects.

When there is an object, a `*` resolves against the object, so the values keep their real types. With a named map key, the object walk cannot get to the value. Approvalcrest then uses the serialised JSON, where a whole number is a `Long`. Thus `.with("ordersByRef.*.quantity", equalTo(1))` and `equalTo(1L)` both match. But `.with("ordersByRef.A-1.quantity", equalTo(1))` needs the `1L` form.

The same applies to ordering matchers such as `greaterThan`, `lessThan`, `greaterThanOrEqualTo` and their counterparts. They have one more risk. Approvalcrest resolves a field path two times: first against the object, then against the serialised JSON if the first attempt did not decide the result.

Thus an `int` field is an `Integer` on the first attempt and a `Long` on the second. So the retry answers `greaterThan(0L)`, and it works. A `long` field is a `Long` on both attempts, so `greaterThan(0)` matches nothing, however large the value is. Approvalcrest reads a field that `.withMatcher(...)` / `.alsoCheckMatching(...)` gets to only from the serialised JSON, so a whole number there is always a `Long`.

### Numbers and boxing

**Write the number of the matcher in the same form as the value.** Use `greaterThan(0L)` for a `long` field, and for any name-pattern matcher. A `double` field is a `Double` on the object walk, so `greaterThan(1.0)` is correct there. But approvalcrest writes a *whole-valued* double such as `2.0` as a whole number. It comes back from the serialised JSON as a `Long`, so the name-pattern forms need `greaterThan(1L)` for it.

A wrong form gives a mismatch. The failure message names the value, the type that it came as, and the cast that failed. The same applies to a matcher in `allOf`, `both().and()`, `hasItem` or `everyItem`.

**One combination cannot fail.** Sometimes the number of the matcher has a different form from the value. Then `not(greaterThan(...))` passes, whatever the data is. The same is true for `lessThan`, `greaterThanOrEqualTo` and `lessThanOrEqualTo`. Hamcrest answers false for the pair, and does not throw an exception. The negation changes that false into a pass, so nothing can make the assertion fail:

```java
// long id = 42. Passes, and would pass for any value at all.
assertThat(actual, sameBeanAs(expected)
    .with("id", not(greaterThan(0))));

// Correct: the matcher's number written the same way as the field's.
assertThat(actual, sameBeanAs(expected)
    .with("id", not(greaterThan(0L))));
```

## Matching the Container Itself

To assert a property of the container, and not of each element, point the path **at the container field**. Then use a container matcher. The path stops there, so there is no fan-out. The matcher receives the collection, map or array.

### Size

```java
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;

// The orders list must contain exactly one element
assertThat(actual, sameBeanAs(expected)
    .with("orders", hasSize(1)));

// At least one element, without pinning the exact count
assertThat(actual, sameBeanAs(expected)
    .with("orders", hasSize(greaterThan(0))));
```

### Content and order

```java
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;

// Exactly these elements, in exactly this order
assertThat(actual, sameBeanAs(expected)
    .with("orders", contains(orderWithRef("A-1"), orderWithRef("A-2"))));

// Exactly these elements, order irrelevant — the right choice for a Set
assertThat(actual, sameBeanAs(expected)
    .with("orders", containsInAnyOrder(orderWithRef("A-2"), orderWithRef("A-1"))));

// At least one matching element; others may be present
assertThat(actual, sameBeanAs(expected)
    .with("orders", hasItem(orderWithRef("A-1"))));
```

### Maps

```java
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;

assertThat(actual, sameBeanAs(expected)
    .with("ordersByRef", aMapWithSize(1)));

assertThat(actual, sameBeanAs(expected)
    .with("ordersByRef", hasEntry(equalTo("A-1"), orderWithRef("A-1"))));

assertThat(actual, sameBeanAs(expected)
    .with("ordersByRef", hasKey("A-1")));
```

### Arrays

A Java array is not a `Collection`, so `hasSize` and `contains` do not apply to an array field. Use the matchers for arrays:

```java
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.arrayContaining;

assertThat(actual, sameBeanAs(expected)
    .with("orderArray", arrayWithSize(2)));

assertThat(actual, sameBeanAs(expected)
    .with("orderArray", arrayContaining(orderWithRef("A-1"), orderWithRef("A-2"))));
```

Fan-out through an array works as usual. `.with("orderArray.trackingCode", notNullValue())` works as it does for a `List`.

## Container Matchers and JSON String Input

`sameJsonAsApproved` accepts an object or a raw JSON string. Approvalcrest resolves a container matcher against the Java object. Only if the path cannot resolve there, approvalcrest tries again against the serialised JSON, where a collection is a Gson `JsonArray`. When a container matcher resolves against the object, that result is final, and there is no retry. Thus a negated container matcher works when the path resolves against the object. On JSON-string input, the path never resolves against the object, and one combination there cannot fail (see the note about negation below).

A `JsonArray` is an `Iterable` but not a `Collection`. Thus the matcher gets a read-only `List` view of the live array. Approvalcrest does not copy the array. The view converts a scalar element when it reads it. A JSON number comes as a `Long` or `Double`, and a JSON string as a `String`.

Thus size, emptiness and scalar contents work for both input forms. A JSON string cannot supply element *classes*:

| Matcher | Object input | JSON string input |
|---|---|---|
| `hasSize`, `empty`, `iterableWithSize`, `emptyIterable` | works | works |
| `hasItem`, `contains`, `containsInAnyOrder` over **scalars** | works | works |
| `hasItem`, `contains`, `containsInAnyOrder` over **objects** | works | the matcher gets the container, but an element matcher for the class of the field cannot match a `JsonObject`. See the note about negation below: there, this becomes a pass with no warning |
| `hasEntry`, `hasKey`, `aMapWithSize` | works | fails, because a map serialises to a `JsonArray` of single-entry objects, not to a `java.util.Map` |

```java
// Both hold whether the input is an object or a JSON string
assertThat(actual, sameJsonAsApproved()
    .with("orders", hasSize(2)));

assertThat(actual, sameJsonAsApproved()
    .with("tags", contains("urgent", "review")));
```

A size mismatch reports the size, for either input form:

```
orders collection size was <2>
```

Negation works for a container matcher on both input forms, because the value at the path decides the result of the container matcher:

```java
// Fails when orders is empty, for object and JSON-string input alike
assertThat(actual, sameJsonAsApproved()
    .with("orders", not(empty())));

// Fails when an order with this reference is present
assertThat(actual, sameBeanAs(expected)
    .with("orders", not(hasItem(orderWithRef("A-9")))));
```

**One combination cannot fail.** `not(hasItem(...))`, `not(contains(...))` and `not(containsInAnyOrder(...))` pass, whatever the data is, when *all three* of these conditions are true. The input is a raw JSON string. The elements are objects, not scalars. The element matcher is for the class of the element.

```java
// Object input: fails when an order with this reference is present. Correct.
assertThat(actual, sameBeanAs(expected)
    .with("orders", not(hasItem(orderWithRef("A-9")))));

// JSON string input: passes even when A-9 IS present. The matcher is handed a
// JsonObject, which no Order matcher can match, so hasItem is false and not(...) is true.
assertThat(jsonString, sameJsonAsApproved()
    .with("orders", not(hasItem(orderWithRef("A-9")))));
```

Without the negation, approvalcrest reports the same mismatch: `mismatches were: [was JsonObject <{...}>]`. This is the limitation in the table above. The negation changes that non-match into a pass.

There are two solutions. Compare the object and not a JSON string, or write the matcher for the JSON shape. Over **scalar** elements, all three matchers work on both input forms, because the list view converts a scalar when it reads it. Thus `not(hasItem("urgent"))` over a `tags` array fails correctly, whatever the form of the input is.

## A null half-way along a path

A path resolves against the Java object first. Where the object walk cannot get to a value, approvalcrest tries again against the serialised JSON. This applies to an array segment, a map entry that its key identifies, and a member whose serialised name is different from the field name. When the input is a raw JSON string, it applies to all paths.

When a reference on the path is `null`, that retry answers `null` for **everything** below it, because a null in parsed JSON has no type. Thus a path whose segments after the null name nothing still resolves, and `nullValue()` accepts it:

```java
// customer is null. This passes whether or not Customer has an "nmae" member.
assertThat(actual, sameBeanAs(expected)
    .with("customer.nmae", nullValue()));
```

Thus a path that still uses a name from before a rename asserts nothing, if a reference on it is null. When a `nullValue()` assertion on a nested path passes, it tells you about the reference, not about the leaf.

The solution is to assert the reference, so that the assertion says what you mean:

```java
assertThat(actual, sameBeanAs(expected)
    .with("customer", nullValue()));
```

## Match All Fields Whose Name Matches a Pattern

Use `.withMatcher(Matcher<String> fieldNamePattern, Matcher<V> matcher)` to apply a custom matcher to **every field at any depth** whose name matches the pattern. This is useful when many fields have the same naming convention:

```java
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.matchesPattern;

// Every field whose name ends with "Id" must be non-null
assertThat(actual, sameJsonAsApproved()
    .withMatcher(endsWith("Id"), notNullValue()));

// Every field whose name starts with "url" must be a valid HTTPS URL
assertThat(actual, sameJsonAsApproved()
    .withMatcher(startsWith("url"), matchesPattern("https://.*")));
```

You can chain several patterns:

```java
assertThat(actual, sameJsonAsApproved()
    .withMatcher(endsWith("Id"),    notNullValue())
    .withMatcher(startsWith("url"), matchesPattern("https://.*")));
```

`.with(path, matcher)` points to one named field. `.withMatcher` is different: it scans the full object graph, and applies to every field name that matches, at any position. Approvalcrest applies pattern-based matchers before the sort.

The matcher gets the values in the same form as for `.with(path, matcher)`. Thus a collection-valued field also accepts a container matcher here, and the table above applies without change:

```java
// Every field named "tags" must hold two elements
assertThat(actual, sameJsonAsApproved()
    .withMatcher(is("tags"), hasSize(2)));
```

Approvalcrest matches a pattern against the name of the field, whatever its type is. This includes `Set`-typed and `Map`-typed fields. "Every field that matches" has one result: a pattern that matches **no** field passes, because there is nothing to check. Thus a pattern with a wrong field name gives an assertion that passes, not an error.

Here, a whole number is **always** a `Long`, because this form reads the serialised JSON and never the object. Thus an ordering matcher in a pattern needs `greaterThan(0L)`, never `greaterThan(0)`. See [Numbers and boxing](#numbers-and-boxing).

`.withMatcher(...)` removes every field that it matches, as `.with(...)` does. To keep those fields in the comparison and also assert them, use `.alsoCheckMatching(fieldNamePattern, matcher)`. See [Asserting *in addition to* the comparison](#asserting-in-addition-to-the-comparison).

**The pattern form has no last-call-wins.** Patterns accumulate in a list, and two matcher instances are never equal. Thus `.withMatcher(is("tags"), m1).alsoCheckMatching(is("tags"), m2)` still removes `tags`. You can register only the path form again in the other mode.

## Works With

- `sameBeanAs` — assert a specific field with a matcher, instead of the comparison with `expected` or in addition to it
- `sameJsonAsApproved` — the same, against the approved file

Both modes work with both matchers. `sameContentAsApproved` has no fields, so none of them apply to it.

## Kotlin

The type inference of Kotlin does not work correctly with F-bounded generics ([KT-5464](https://youtrack.jetbrains.com/issue/KT-5464)). The Kotlin module has extension functions that let chains compile. See [kotlin](kotlin.md).

## Related

- [dynamic-values](dynamic-values.md) — how to select ignore, alias or custom matching
- [ignoring-fields](ignoring-fields.md)
- [kotlin](kotlin.md)
