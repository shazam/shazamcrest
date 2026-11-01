<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=7ef7a133-8641-4190-a106-d60f524f3722" />

# same-bean-as

Compare two Java objects field by field, through the full object graph.

## Why Not `equals()`?

`sameBeanAs` does not use the `equals()` method of the objects, and this is intentional. An `equals()` method is often not sufficient for a test:

- **It can leave out fields.** A hand-written `equals()` checks only the fields that the developer thought of at that time. Fields that someone adds later are not in the comparison, and nothing tells you.
- **It can be wrong.** An `equals()` that an IDE generates often covers only some of the fields. Business objects often override `equals()` to compare only the ID. Then the assertion passes, also when all the other fields are wrong.
- **It does not tell you where the difference is.** When `equals()` returns `false`, you know that the objects differ, but not where. `sameBeanAs` shows a JSON diff with the exact field path and both values.
- **It can be `true` for objects that differ in an important way.** Two objects with the same ID and a different state pass an `equals()` that compares only the ID. Then your test passes when it must fail.

`sameBeanAs` serialises both objects to JSON and compares the full graph. Thus it finds each field difference, however the class implements `equals()`, and also when the class does not implement `equals()`.

## Basic Usage

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;

@Test
void orderMatchesExpected() {
    Order actual   = buildOrder();
    Order expected = expectedOrder();

    assertThat(actual, sameBeanAs(expected));
}
```

## Why Use It

With usual Hamcrest matchers, you need one matcher for each field that you want to check:

```java
// Before — tedious and easy to forget a field
assertThat(actual.getId(),    equalTo(expected.getId()));
assertThat(actual.getName(),  equalTo(expected.getName()));
assertThat(actual.getPrice(), equalTo(expected.getPrice()));
// ... and so on for every field
```

With `sameBeanAs`, one call compares the full object graph:

```java
// After — complete comparison with no boilerplate
assertThat(actual, sameBeanAs(expected));
```

If any field differs, the error message shows the JSON diff with the exact path and both values.

## Error Messages

When a field differs, you get a structured diff:

```
Expected:
{
  "name": "Widget",
  "price": 9.99
}
     but:
{
  "name": "Widget",
  "price": 12.99     ← actual value
}
```

Use `MatcherAssert.assertThat` from the approvalcrest package, not the `assertThat` of Hamcrest. Then you get a `ComparisonFailure`, and the IDE can show a side-by-side diff.

## Skipping Class Comparison

By default, `sameBeanAs` requires that both objects have the same type. To compare objects of different types that have the same serialised structure, use `.skipClassComparison()`:

```java
assertThat(actualDto, sameBeanAs(expectedEntity).skipClassComparison());
```

To turn off the type check for all tests, set `-DbeanMatcherSkipClassComparison=true`.

## Circular References

The matcher handles circular object graphs automatically. It serialises each object one time. It replaces each later reference to the same instance with a `0xN` pointer:

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

You do not have to configure anything. The matcher finds circular references automatically.

## Related

- [ignoring-fields](ignoring-fields.md) — remove fields from the comparison; a path applies to each element of a collection
- [custom-matching](custom-matching.md) — assert conditions on single fields; a path applies to each element of a collection
- [sorting](sorting.md) — make the order of collections stable; `sortType` sorts each collection whose element type you name
- [supported-types](supported-types.md) — how approvalcrest serialises common Java types
