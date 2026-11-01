<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=b01fdd26-9e71-41c4-9255-0f11b90de210" />

# same-content-as-approved

Assert that any text content matches an approved file.

`sameContentAsApproved` uses the same approval workflow as [`sameJsonAsApproved`](same-json-as-approved.md), with these differences:
- The file extension is `.content`, not `.json`.
- It accepts any `String` value, and does not parse JSON.
- It compares the content byte-for-byte. Before the comparison, it changes Windows `\r\n` line endings to `\n`.

Use it for rendered templates, API responses, log output, generated reports, or any text where the exact content is important.

## Basic Usage

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameContentAsApproved;

@Test
void assertContent() {
    String actual = renderTemplate();
    assertThat(actual, sameContentAsApproved());
}
```

## Approval Workflow

1. **First run** — the matcher writes `<classHash>/<methodHash>-not-approved.content` next to the test source.
2. **Review** the generated file.
3. **Rename** it to `*-approved.content`.
4. **Later runs** compare the content with the approved file.

## Parameterized Tests (JUnit 5 & 6)

Pass `TestInfo` to the matcher. Use `.withUniqueId()` to make a separate approved file for each case:

```java
@ParameterizedTest
@MethodSource("cases")
void parameterized(String input, TestInfo testInfo) {
    assertThat(input, sameContentAsApproved(testInfo).withUniqueId(input));
}
```

## File Control

All the file name and path options of `sameJsonAsApproved` work the same way here:

```java
// Distinct approved file per assertion in the same test
assertThat(header, sameContentAsApproved().withUniqueId("header"));
assertThat(body,   sameContentAsApproved().withUniqueId("body"));

// Custom file name
assertThat(actual, sameContentAsApproved().withFileName("my-template"));

// Machine-readable output for CI and AI agents
assertThat(actual, sameContentAsApproved().withMachineReadableOutput());
```

See [file-control](file-control.md) for the full list of options.

## What Is Not Available

These features are only for the JSON and bean matchers. They do **not** apply to `sameContentAsApproved`:

- `.ignoring()` — removes fields, and raw text has no fields
- `.with()` / `.withMatcher()` / `.alsoCheck()` / `.alsoCheckMatching()` — custom matchers for single fields
- `.sortField()` / `.sortType()` — sorts collections
- `.withAlias()` / `.withAliasMap()` — replaces values with aliases

Content can contain sub-strings that change from run to run, such as generated IDs and timestamps. We recommend two approaches. Make the system deterministic at the source (see [dynamic-values](dynamic-values.md)). Alternatively, change the string before the assertion:

```java
// Replace the volatile token before asserting
String stable = actual.replaceAll("\\d{4}-\\d{2}-\\d{2}", "<date>");
assertThat(stable, sameContentAsApproved());
```

## Unicode Support

The matcher supports all of Unicode, also non-Latin scripts. The matcher stores the content byte-for-byte in the approved file. It also compares the content byte-for-byte.

## Related

- [file-control](file-control.md) — custom file names, paths, and in-place update
- [same-json-as-approved](same-json-as-approved.md) — the approval matcher for JSON
- [dynamic-values](dynamic-values.md) — how to handle values that change from run to run
