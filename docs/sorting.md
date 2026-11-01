<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=c4c692b7-64c6-4a83-be80-f1fbb22b9e65" />

# sorting

Control the order of collections, to get stable comparisons whatever the iteration order is.

## Why Sorting Matters

Some collections have no guaranteed iteration order, for example `HashSet`, `HashMap` values, and the results of a database query without an `ORDER BY`. Their order can be different on each test run. Without a sort, a test that passes today can fail tomorrow, because the JVM hashed the elements in a different way. A sort makes the approved file stable and the comparison deterministic.

## Automatic Set Ordering

Approvalcrest **sorts** `HashSet` and other unordered sets **automatically** before the comparison. You do not have to configure this. Approvalcrest treats elements with the same serialised form as equal.

## Explicit Sort by Field Path

For `List` or `Map` values that have no guaranteed insertion order, sort by field name:

```java
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;

// Sort the "tags" collection
assertThat(actual, sameJsonAsApproved()
    .sortField("tags"));
```

You can use nested paths, and sort at several levels independently:

```java
// Sort the orders list, then sort the items within each order.
// 'orders' is a List; sortField("orders.items") fans out into every order element
// and sorts its items sub-collection.
assertThat(actual, sameJsonAsApproved()
    .sortField("orders")
    .sortField("orders.items"));
```

**Collection fan-out:** when a path segment resolves to a collection, approvalcrest sorts the sub-field that matches in **every element** of that collection. You write the path in the same way, whether the middle segments are single objects or collections.

To sort the root-level list, use an empty string for the root:

```java
assertThat(actual, sameJsonAsApproved()
    .sortField(""));
```

## Sort by Hamcrest Matcher on Field Name

Pass `Matcher<String>` arguments to match several field names at the same time:

```java
import static org.hamcrest.Matchers.is;

assertThat(actual, sameJsonAsApproved()
    .sortField(is("orders"), is("items")));
```

## Excluding Fields from the Sort Key (`SortField`) — Advanced

When approvalcrest sorts a collection of objects, the sort key of each element is its **full serialised JSON**. Elements can contain fields that change from run to run or are not relevant, for example IDs and timestamps. Such fields make the sort key unstable. With `SortField`, you remove specified fields from the sort key, so that only the stable fields set the order:

```
sort key = (full element JSON) − (excluded fields)
```

### Exclude one field from the sort key

```java
import com.github.karsaig.approvalcrest.matcher.sorting.SortField;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;

// Elements in "items" are sorted by all their fields except "age"
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("items", "age")));
```

### Exclude multiple fields

Pass all of them as varargs, or chain `.ignoring()` calls. The result is the same:

```java
// Exclude both "id" and "createdAt" from the sort key
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("items", "id", "createdAt")));

// Equivalent with chained calls
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("items").ignoring("id").ignoring("createdAt")));
```

### Exclude fields matching a pattern

Use `.ignoring(Matcher<String>)` to remove each field whose **name** matches a Hamcrest matcher. This is useful when the fields that change have the same naming convention:

```java
import static org.hamcrest.Matchers.containsString;

// Strip all fields whose name contains "Date" (e.g. "createdDate", "updatedDate")
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("items").ignoring(containsString("Date"))));
```

### Exclude nested fields from the sort key

Dot-paths work. You can remove a sub-field of a nested object from the sort key, and keep the rest of the nested object in it:

```java
// Sort "items" by all fields except the nested "address.city"
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("items").ignoring("address.city")));
```

### Matcher selector — apply to multiple collections at once

The collection selector can be a `Matcher<String>`, not a literal path. Then approvalcrest sorts **every collection whose name matches**, at any position in the object graph. Use `.sortFieldMatcher()` for this:

```java
import static org.hamcrest.Matchers.is;

// Sort every collection literally named "tags" throughout the whole object graph,
// excluding "weight" from each element's sort key
assertThat(actual, sameJsonAsApproved()
    .sortFieldMatcher(SortField.of(is("tags"), "weight")));
```

This also fans out through the collections on the path. If the object has `groups[].tags[]`, approvalcrest sorts each `tags` in each group element automatically.

### Compound sort key

When several fields stay in the sort key after the exclusion, they all have an effect on the order of the element. Approvalcrest serialises the fields in **alphabetical order by field name**. Thus, if you remove `id` from `{id, age, name}`, the sort key becomes `{age, name}`. The sort uses `age` first, because it is first in the alphabet, and uses `name` when two `age` values are equal. There is no API to set the priority of a field. The alphabetical order of the field names sets the priority.

## Works With

`sameBeanAs` and `sameJsonAsApproved`.

Both matchers have `.sortFieldPath(SortField)` (literal-path selector) and `.sortFieldMatcher(SortField)` (Hamcrest-matcher selector).

## Sort by Element Type

Use `sortType(Class<?>...)` to sort automatically each `Collection` or array whose element type matches one of the specified classes. This is the same as the automatic sort of `Set` fields. You do not have to give a field path:

```java
// Sort all List<Person> and array-of-Person fields automatically
assertThat(actual, sameJsonAsApproved()
    .sortType(Person.class));

// Multiple types at once — covers every collection of Person or Address anywhere in the graph
assertThat(actual, sameBeanAs(expected)
    .sortType(Person.class, Address.class));
```

This is useful when the same element type is in several collections in a deep object graph. Then approvalcrest sorts all of them, and you do not have to list each field path.

This works with `sameBeanAs` and `sameJsonAsApproved`.

## Paths Through Maps

A sort path gets to a map entry by its key, and `*` means every entry:

```java
// Sorts the tags collection under ONE entry
assertThat(actual, sameJsonAsApproved()
    .sortField("ordersByRef.A-1.tags"));

// Sorts the tags collection under EVERY entry
assertThat(actual, sameJsonAsApproved()
    .sortField("ordersByRef.*.tags"));
```

`*` has the same meaning here as in [custom-matching](custom-matching.md) and [ignoring-fields](ignoring-fields.md): every named child at that position. Thus one syntax works for all three. A wildcard and a literal key at the same level both apply, and one does not override the other. Thus `sortField("ordersByRef.*.tags", "ordersByRef.A-1.notes")` sorts `tags` in all entries and `notes` under `A-1`.

The ignored sub-paths of a `SortField` also accept `*`. Thus you can keep a field out of the sort key under every child, not only under one named child:

```java
// Sort orders by everything except each entry's generated id
assertThat(actual, sameJsonAsApproved()
    .sortFieldPath(SortField.of("orders", "*.generatedId")));
```

As in other places, `*` is a wildcard only in a segment that is not the last. `sortField("ordersByRef.*")` points to a key with the literal name `*`. To sort the map, name the map field.

## How a complex-key map is written

Approvalcrest writes a map whose keys are not strings, primitives or enums as an array of `[key, value]` pairs, with the key first:

```json
"ordersByRef": [
  [ { "ref": "A-1" }, { "total": 12 } ],
  [ { "ref": "A-2" }, { "total": 30 } ]
]
```

Approvalcrest never swaps the two positions in a pair, so the file records which half was the key. It still sorts all other things. It orders the entries by their content, and sorts a collection-valued half like any other collection. Before 1.5.1, it also ordered the pair by content, so `{a: z}` and `{z: a}` wrote the same bytes.

This is true wherever the declared type of the field that holds the map tells how deep the maps nest. Examples are `Map<K, Map<K2, V>>` at any depth, a map in a `Set`, `List` or array, and any mix of the two, such as `Set<Map<K, Set<Map<K2, V>>>>`. Subclasses also count: `class Registry extends HashMap<Ref, Map<Ref, Order>>`, `EnumMap`, `Hashtable`, the `ImmutableMap` of Guava, and any subclass of your own. The reason is that approvalcrest resolves the value type through the supertypes. It does not read the value type from the second type argument of the declaration.

This is also true for a map at the root. There, approvalcrest examines the *object* and not a declared type. Thus a root `Map<K, Object>` whose values are all maps keeps its pair order, where the same field does not.

It is not true where nothing describes the level:

- a value that the declared type leaves open: `Map<K, Object>`, an unbounded type variable or a wildcard. This also applies to each subclass that sets the value to `Object`, for example `Properties` and the frequent `class Payload extends HashMap<String, Object>`. A raw `Map<K, Map>` is different. The level is a map, so it *is* described. Only the levels below it are not described;
- a map that is a **key**, not a value, because nothing describes the key half of the pair;
- a map below a field that has no marker, such as a plain `List<Map<K, V>>` or a field declared as `Object`.

In each of those cases, the pairs behave as before 1.5.1. Nothing sorts them, unless a `sortField` gets to that level. Thus they usually still come out with the key first. But if you add a `sortField` that gets to them, the key and the value can swap. The same applies to the levels below the eighth container level, because approvalcrest does not describe them.

The declared type decides, and two more things follow from this. First, approvalcrest writes a `Map` whose keys are all strings, primitives or enums as one object for each entry, not as pairs. Thus nothing there needs protection, and a level that approvalcrest claims for such a map has no cost.

Second, a custom type adapter for a `Map` type can write the map in any form. If it writes an array of two-element arrays, approvalcrest cannot tell this from an entry array, and treats it as one. Thus those inner arrays keep their order, and approvalcrest does not sort them. A `Map`-typed *field* has this behaviour in all versions. The change is that the levels below it now have it too.

For a **file matcher**, the key-first order also requires strict mode (`fileMatcherStrictFileMatching`), which is on by default. With `-DfileMatcherStrictFileMatching=false`, approvalcrest orders the pairs by content on both sides. Then the comparison passes for a map and the same map with its keys and values swapped. The written form depends on that setting. Thus, if you change the setting in either direction, you must regenerate each approved file that holds such a map. Approvalcrest reads an unrecognised value of this property as `false`, so a typo such as `-DfMStrictMatching=ture` turns strict mode off.

`sameBeanAs` serialises both sides itself, so there the key stays first, whatever the setting is.

A raw JSON string input keeps its pairs exactly as the string has them. The exception is when a `sortField` names the field that holds them. Then approvalcrest orders them by content.

## A null map key

Approvalcrest records a `null` key as the member name `"null"`:

```json
"m": [ { "null": "someValue" } ]
```

A path points to the entry by that name, as for any other key. If the map holds a key that is not a primitive, a `String` or an enum, approvalcrest writes the full map as `[key, value]` pairs. There, a null key is a bare JSON null.

A `null` key and a `String` key of `"null"` write the same member name. Approvalcrest records both entries, but a comparison cannot tell them apart. Thus the comparison passes for a map with the key `null` and a map with the key `"null"`. If that difference is important, check the key with a separate assertion. Do not rely on the file for it.

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
