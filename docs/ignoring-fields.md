<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=d3ba28e7-0e56-403b-934a-aa06f4b8c47c" />

# ignoring-fields

Remove specific fields from the comparison by path, by Hamcrest matcher or by type. You can also remove full array elements by value with `ignoringElementsWhere`.

This works with `sameBeanAs` and `sameJsonAsApproved`. You can chain several `.ignoring()` calls.

## When to Use Ignoring

Use `.ignoring()` when a field is not relevant to the test. Do not use it as a general way to remove unwanted differences. Too many ignored fields make the approved file less precise, and the tests can then miss defects.

A field can change from run to run and still be important. For such a field, use [aliasing](aliasing.md) to show the field with a placeholder. Or use [custom matching](custom-matching.md) to assert a structural property, for example not null, or a match with a pattern. If you can control the source, for example with an injected clock or a seeded ID generator, do that. It is the preferred approach. See [dynamic-values](dynamic-values.md).

## By Field Path

Use a dot-separated path to remove a specific nested field:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring("address.streetName"));
```

For a top-level field, use the field name:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring("createdAt")
    .ignoring("updatedAt")
    .ignoring("id"));
```

A deep path works at any nesting level:

```java
// Ignore a field nested several levels down
assertThat(actual, sameJsonAsApproved()
    .ignoring("order.customer.address.postCode"));
```

## Paths Through Collections (Fan-out)

When a path segment resolves to a collection or an array, the traversal **fans out**. Approvalcrest applies the rest of the path to each element of that collection. You write the same path, whether the middle segments are single objects or collections.

```java
// 'orders' is a List<Order>; each Order has a 'trackingCode' field.
// This removes trackingCode from EVERY element of orders.
assertThat(actual, sameJsonAsApproved()
    .ignoring("orders.trackingCode"));
```

Fan-out applies at each level. If a sub-field is also a collection, the path fans out again:

```java
// orders → List<Order>, each Order has items → List<Item>, each Item has a price
// Removes price from every item in every order
assertThat(actual, sameJsonAsApproved()
    .ignoring("orders.items.price"));
```

This works the same for `List`, `Set`, any `Collection` subtype and a JSON array. A path through an empty collection does nothing, and gives no warning.

## Paths Through Maps

A path does **not** fan out over a `Map`. Approvalcrest goes through a map by key, and the key is a path segment:

```java
// Removes generatedId from the order stored under key "A-1" only
assertThat(actual, sameJsonAsApproved()
    .ignoring("ordersByRef.A-1.generatedId"));
```

To apply the same path to every entry, use `*`:

```java
// Removes generatedId from EVERY order in the map
assertThat(actual, sameJsonAsApproved()
    .ignoring("ordersByRef.*.generatedId"));
```

`*` means "every named child at that position": a map key, an object property or a bean field. It has the same meaning in `.with(path, matcher)`. Thus an ignore rule and a custom matcher for the same structure agree.

Two limits:

- **`*` is a wildcard only in a segment that is not the last.** As the last segment, it keeps its usual meaning: a key with the literal name `*`. A JSON document or a `Map<String,?>` can have such a key. Thus `.ignoring("headers.*")` removes that key. `.ignoring("ordersByRef.*")` also looks for such a key, and does not mean "every value". If there is no such key, it does nothing and gives no warning.
- **If all values become empty, the field disappears.** Sometimes the ignored field is the only field of each value. Then each value becomes empty, and approvalcrest removes each entry. Then the array becomes empty, and the map field disappears from the output. A named key does the same for one entry. `*` applies to all entries at the same time, so usually the full field disappears.

### Removing elements under every map entry

`ignoringElementsWhere` accepts the same `*` segment. This is the only way to get to the array under each entry of a `Map<String, List<T>>`:

```java
// Removes tags with that system from the list under EVERY entry
assertThat(actual, sameJsonAsApproved()
    .ignoringElementsWhere("ordersByRef.*.tag.system", FLOW_ID_TAG_SYSTEM));
```

Compare it with the form **without the wildcard**, not with a named key. `ordersByRef.system` filters the outer array of map entries. It looks for a `system` field in each entry object, finds none, and thus does nothing and gives no warning. `ordersByRef.*.system` filters the list *inside* each entry, which is what you want for a map of lists. A named key, `ordersByRef.A-1.system`, gets to the same inner list for that one entry. Thus the wildcard is the named form, applied to each entry.

A `*` at the end of the full path is the leaf field name, not a wildcard. This is the same as in all other places.

A map whose keys are not strings, primitives or enums serialises in a different way: as pairs, not as single-entry objects. Thus a path through such a map gets to the fields of the **key** objects, and also of the values. A named path already does this, and `*` does the same.

## Removing Array Elements by Value

`.ignoring()` removes *fields*. It cannot remove one array element because of the content of that element. `.ignoringElementsWhere()` does this. It removes the array elements whose nested field has a given value.

The path points to a field **in each element**. `ignoringElementsWhere` filters the **innermost array on the path**. It removes each element whose leaf field value satisfies the matcher:

```java
import static org.hamcrest.Matchers.equalTo;

// meta.tag is an array of { system, code, ... }; remove the elements
// whose system is the tracking system, keeping the rest of the array.
assertThat(actual, sameJsonAsApproved()
    .ignoringElementsWhere("meta.tag.system", equalTo(FLOW_ID_TAG_SYSTEM)));
```

Any `Matcher` works (`startsWith`, `greaterThan`, `not(...)`, …). An overload also accepts a plain `String`. It matches the value of the field, converted to a String:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoringElementsWhere("meta.tag.system", FLOW_ID_TAG_SYSTEM));
```

As with the other ignore styles, the path fans out through the collections before the innermost array. Thus it works for nested structures such as a FHIR `Bundle`. This example removes the `tag` elements that match from the resource of each entry, not the entries:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoringElementsWhere("entry.resource.meta.tag.system", equalTo(FLOW_ID_TAG_SYSTEM)));
```

Behaviour:

- The path matches the field names of the **serialised JSON**. Write the path exactly as it is in the JSON. Do this when the input is a JSON string, or a value that serialises with those names. Some objects serialise with different internal names, for example raw library model objects with a custom Gson `TypeAdapter`. Such objects must use the JSON-level names in their serialised form, or the path does not match.
- If **all** elements match, approvalcrest keeps the array as an empty array (`[]`), and does not remove it. After regeneration, the approved file contains the empty array.
- Approvalcrest keeps an element that does not have the leaf field, or that is not a JSON object. A path that is absent, empty or not an array does nothing, and gives no warning.
- In strict mode (the default), approvalcrest removes elements only from the **actual** side. Thus you must regenerate the approved file to remove them from it. `.ignoring()` works in the same way.

## By Hamcrest Matcher on Field Name

Pass a `Matcher<String>` to remove all fields whose name matches, at any depth in the object graph:

```java
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

// Ignore all fields whose name starts with "street"
assertThat(actual, sameJsonAsApproved()
    .ignoring(startsWith("street")));

// Ignore all timestamp-style fields
assertThat(actual, sameJsonAsApproved()
    .ignoring(endsWith("At")));

// Ignore a specific field by exact name
assertThat(actual, sameJsonAsApproved()
    .ignoring(is("transientId")));
```

Use this when many fields have the same naming convention. Then you can remove all of them, and you do not have to list each field.

## By Type

Ignore all fields of a given Java type in the full object graph:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring(Instant.class));
```

Use this to remove all timestamp fields, when they have the same type. You can chain several types:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring(Instant.class)
    .ignoring(UUID.class));
```

## Null Field Serialization

By default, the serialised JSON includes fields that have a null value. `.ignoring()` needs this when a field has a null value in a collection element. Without null serialisation, approvalcrest leaves the field out of the JSON before the ignore logic runs. Then approvalcrest never removes the ignored element from the collection.

**Turn off null serialisation for all tests.** This gives the old behaviour, where the JSON leaves out null fields:

```bash
mvn test -DapprovalcrestSerializeNulls=false        # Maven
./gradlew test -DapprovalcrestSerializeNulls=false  # Gradle
```

**Turn off null serialisation for one matcher:**

```java
assertThat(actual, sameJsonAsApproved()
    .withoutSerializingNulls()
    .ignoring("id"));
```

When null serialisation is off and `field` is null, `.ignoring("field")` has no effect. The field is not in the JSON, so there is nothing to remove.

**Migration from pre-1.0.2 approved files:**

Re-run your tests with `-DfileMatcherUpdateInPlace=true` to regenerate approved files that now include null fields:

```bash
mvn test -DfileMatcherUpdateInPlace=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true  # Gradle
```

## Strict Mode (default on)

By default, `ignoring()` operates in **strict mode**: approvalcrest removes ignored fields only from the **actual** side. Sometimes approvalcrest wrote an approved file while strict mode was off, for example in a version before 1.0.1. If that file still contains the value of an ignored field, the test fails.

**Migration from pre-1.0.1 approved files:**

Re-run your tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files without the ignored fields:

```bash
mvn test -DfileMatcherUpdateInPlace=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true  # Gradle
```

**Turn off strict mode for all tests.** This gives the old two-sided behaviour. Approvalcrest then removes ignored fields from the actual side and from the approved side before the comparison:

```bash
mvn test -DfileMatcherStrictFileMatching=false        # Maven
./gradlew test -DfileMatcherStrictFileMatching=false  # Gradle
```

**Turn off strict mode for one test** (with the last parameter of the `FileMatcherConfig` constructor):

```java
// new FileMatcherConfig(overwriteInPlace, passOnCreate, sortInputFile, strictMatching)
FileMatcherConfig config = new FileMatcherConfig(false, false, false, false);
```

## Chaining

You can chain the three styles in any combination:

```java
assertThat(actual, sameBeanAs(expected)
    .ignoring("id")
    .ignoring(startsWith("internal"))
    .ignoring(Instant.class));
```

## A map key that looks like a circular-reference marker

Approvalcrest writes an object with a circular reference in a wrapper, under a generated key: `{"0x1": {…}}`. Path navigation skips that key, so a path is the same whether or not the type has cycles. Approvalcrest treats a key as a wrapper key if it is `0x` followed by lowercase hex digits. A map key of your own can have this form.

A path that leaves out such a key still goes through it. For any other key, such a path matches nothing:

```java
// map has one entry, keyed "0x1"
.ignoring("map.0x1.leaf")   // removes leaf, as it should
.ignoring("map.leaf")       // ALSO removes leaf -- the key is skipped as if it were a wrapper
.ignoring("map.k1.leaf")    // for an ordinary key "k1", omitting it matches nothing
```

Approvalcrest filters both sides of a comparison in the same way, so this does not cause a false failure. But it removes more from the comparison than you asked for, and gives no warning. Thus the assertion becomes weaker. The same applies to `sortField(...)` and `ignoringElementsWhere(...)`, because they navigate paths in the same way.

Exactly the keys that are `0x` plus lowercase hex have this effect: `0x1`, `0xff`, `0xdeadbeef`. A key with an uppercase digit (`0xFF`), a prefix or a suffix does not. If you have such keys and need exact paths under them, rename the key for the comparison. Alternatively, use a path to the entry that does not go through the key.

## Related

- [dynamic-values](dynamic-values.md) — how to select ignore, alias or custom matching
- [same-bean-as](same-bean-as.md)
- [same-json-as-approved](same-json-as-approved.md)
