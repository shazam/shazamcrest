<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=f4abc3d0-c0e5-40bc-bcd0-8fdfaaa1e2f0" />

# same-json-as-approved

Assert that a Java object or a JSON string matches an approved file.

## The Approval Workflow

1. **First run** — there is no approved file. The matcher writes `<classHash>/<methodHash>-not-approved.json` next to the test source.
2. **Review** the generated file to make sure that the output is correct.
3. **Rename** it from `*-not-approved.json` to `*-approved.json`.
4. **Later runs** compare the output with the approved file. A run passes if the two match. If they do not match, the run fails and shows a diff.

Commit the approved files to version control. Review changes to them in pull requests, as you do for application code.

## Basic Usage

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;

@Test
void runWithDefaultConfigShouldPassWithMatchingApprovedFile() {
    MyBean actual = buildMyBean();
    assertThat(actual, sameJsonAsApproved());
}
```

## JSON String as Input

The matcher also accepts a JSON string that is already serialised:

```java
@Test
void rawJsonStringMatchesApprovedFile() {
    String json = fetchJsonFromApi();
    assertThat(json, sameJsonAsApproved());
}
```

## Parameterized Tests (JUnit 5 & 6)

Add `TestInfo` as a test parameter. JUnit 5 & 6 inject it automatically. Pass it to `sameJsonAsApproved(testInfo)`, so that the matcher can find the correct test method. Use `.withUniqueId(name)` to make a separate approved file for each case:

```java
@ParameterizedTest
@MethodSource("data")
void testParameterized(String name, BeanOne value, TestInfo testInfo) {
    assertThat(value, sameJsonAsApproved(testInfo).withUniqueId(name));
}
```

Without `TestInfo`, the matcher cannot find a **private** test method. It throws a `NullPointerException` with a message that gives the fix. Public test methods work without `TestInfo`, because the matcher finds the method metadata from the stack trace.

## Parameterized Tests (JUnit 4)

JUnit 4 needs a `Junit4DesciptionWatcher` rule. See [junit4-vintage](junit4-vintage.md) for a full example.

## Related

- [same-content-as-approved](same-content-as-approved.md) — the same approval workflow for any text
- [file-control](file-control.md) — custom file names, paths, and in-place update
- [ignoring-fields](ignoring-fields.md) — exclude fields from comparison
- [aliasing](aliasing.md) — stable placeholders for values that change from run to run
- [sorting](sorting.md) — make the order of collections stable
- [supported-types](supported-types.md) — how approvalcrest serialises common Java types
- [junit4-vintage](junit4-vintage.md) — JUnit 4 / Vintage usage
- [junit5-jupiter](junit5-jupiter.md) — JUnit 5 & 6 Jupiter usage
