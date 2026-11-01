<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=3f8eb663-fce0-4df5-9f6f-1b6508693937" />

# best-practices

Guidelines for the use of approvalcrest in real projects.

## Choosing Between Matchers

- **`sameBeanAs(expected)`** — you keep the expected object in code. The full assertion is in the test source file. Use it when the expected value is easy to build and you want it next to the assertion.
- **`sameJsonAsApproved()`** — you review the serialised output one time, rename the file to approve it, and commit it. Use it for large or complex object graphs, where an expected object in code is difficult to maintain. The approved file becomes the exact, versioned specification of the expected output.
- **`sameContentAsApproved()`** — the same approval workflow as `sameJsonAsApproved`, but for any text content. Examples are rendered templates, API response bodies, generated reports, log output and HTML fragments. The file extension is `.content`. It does not parse JSON, and compares the content byte-for-byte.

## What an Approved File Represents

After you rename it from `*-not-approved.json` to `*-approved.json`, an approved file has the same function as an expected value in code, but it is stricter. A usual assertion checks only the fields that you remembered to check. An approved file asserts every field in the full object graph. An unexpected change at any position fails the test immediately.

Treat approved files like application code:
- **Commit them to version control** — they are part of the test suite
- **Review changes in pull requests** the same way you review application code
- **Never commit `*-not-approved.*` files** — if one is in CI, nobody approved the test. Treat it as a build failure.

## Testing at Every Level

approvalcrest works at each test level. The API and the approval workflow are the same at all levels.

### Unit Tests — Best Fit

`sameBeanAs`, `sameJsonAsApproved` and `sameContentAsApproved` all work equally well at the unit level. State control is easy, because everything runs in one JVM process and is fast to reset. Unit tests run quickly, so you can handle a change that affects many tests locally. Run with `-DfileMatcherUpdateInPlace=true`. Review the diff. Then commit the updated approved files. Small changes do not need the CI patch job.

### Integration Tests — Effective but Fiddlier

Integration tests are close to the implementation code. This causes two problems:
- Implementation details can get into the serialised output. Then internal changes that do not affect external behaviour can make the tests fail.
- State (databases, caches, queues) is more difficult to reset fully than in unit tests.

With good state management (see below), integration tests work well with approvalcrest. Decide carefully what you include in the approved file. Sometimes, when you ignore or alias internal-only fields, the approved file becomes a more stable specification.

### API Tests — Natural Fit

The output is already in a serialised format (JSON, XML, plain text). Give the response body directly to `sameJsonAsApproved()` or `sameContentAsApproved()`. API calls set up the state, which is usual at this level. For a change that affects many endpoints, use the CI patch workflow below.

### E2E and UI Tests

Two patterns work well:

**Page Object Model with `getModel()`:**

Add a `getModel()` method to each Page Object. The method copies the important state of the page into a DTO. Give the DTO to `sameJsonAsApproved()`:

```java
ProductPage page = navigator.to(ProductPage.class);
assertThat(page.getModel(), sameJsonAsApproved());
```

The DTO contains only the important data, for example prices, labels and availability. The fragile element selectors stay in the Page Object, not in the test. Approved files record the expected page state exactly. When a UI change affects many pages, the patch-generation job regenerates all approved files in one run.

**DOM fragment with `sameContentAsApproved()`:**

Get a section of the rendered DOM. Then assert it directly:

```java
String component = driver.findElement(By.id("product-card")).getAttribute("outerHTML");
assertThat(component, sameContentAsApproved());
```

This is less stable than the DTO approach, because whitespace, generated class names and attribute order can cause unwanted differences. But it is simpler to set up, and it is useful to find unexpected changes to the DOM structure.

Page Object `getModel()` is one good pattern for E2E tests. Any pattern that copies the important state into a form that you can assert works in the same way.

## State Management

Tests must give deterministic output. The best approach is to reset each part of the stateful infrastructure to a known baseline before each test:

- **Database**: truncate tables, roll back transactions, or restore a fixed fixture snapshot
- **Caches**: clear them, or load them to a defined state
- **Message queues**: empty them, or fill them with known messages
- **Clocks and ID generators**: inject and control them, so that the output is fully predictable

When each test has its own state, you get three benefits that add to each other:
1. Each test is **independent**. Side effects from an earlier test cannot break a test.
2. Tests are **reproducible** when they run alone. They give the same result each time.
3. The full suite can run **in parallel** safely. Without shared mutable state, the tests have no race conditions between them.

See [dynamic-values](dynamic-values.md) for detailed strategies, for example clock injection, state reset patterns and fallback approaches for legacy systems.

## CI Workflow

### Two-Job Setup

Run two separate CI jobs:

**Job 1 — Verify (runs on every push, must pass before merge):**

```bash
mvn test        # Maven
./gradlew test  # Gradle
```

This job fails immediately if an approved file is absent or differs from the actual output.

**Job 2 — Patch Generation (you start it manually or on request):**

```bash
mvn test -DfileMatcherUpdateInPlace=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true  # Gradle
git diff > approved-files.patch
```

If a test uses a custom matcher (`.with`, `.withMatcher`, `.alsoCheck`, `.alsoCheckMatching`), add `-DfileMatcherSkipCustomMatchersOnUpdate=true`. When a custom matcher fails, approvalcrest does not rewrite the approved file of that assertion. Without the property, that file stays old, however often the job runs. Treat the result of this job as a step that makes files, not as a test result. With the property, custom matchers do not run, so Job 1 tells you whether the regenerated files are correct. See [file-control](file-control.md#custom-matchers-during-a-regeneration-run).

Publish `approved-files.patch` as a CI artifact. Review the patch. Then download and apply it:

```bash
git apply approved-files.patch
```

Then commit the changes to the approved files (in a separate commit, see below). Push the commit. Job 1 then passes.

### Choosing Your Path

**Small change (few affected tests):** run locally with `-DfileMatcherUpdateInPlace=true`. Review the diff. Then commit the updated approved files.

**Large change (many affected tests):** push the code change. Start the patch-generation job. Review the patch artifact. If you find problems, fix them locally or repeat these steps. When the patch is correct, apply it. Then commit the approved files.

## Reviewing Approved File Changes

### Keep Approved File Changes in Separate Commits

When a code change updates many approved files, commit those files separately from the application code:

```
commit 1: Rename UserDto fields (application code)
commit 2: Update approved files for UserDto rename
```

Then a pull request is easier to review. Reviewers can examine the code change and the approved file change independently, and understand the relation between them.

### De-duplicate Large Diffs

Sometimes one change affects hundreds of approved files, for example the rename of a field that is in every approved file. Then the diff repeats the same change many times. A small script can combine identical change patterns into a summary. Then the review is easy, also for very large change sets.

### Review Gets Faster Over Time

The first few times that a reviewer sees approved files, they examine the structure: field names, nesting and types. After some reviews, the reviewer knows the schema and trusts it. Later reviews can go directly to the important parts: the values that changed, new fields and business logic output.

Problems become visible automatically in both places:
- **Locally**: the IDE diff view shows which field changed, and both values. You do not have to search the logs.
- **In the PR**: the approved file shows as a usual file diff. You see the change in context, as for any other code change. Reviewers do not have to run anything.

## File Organisation

Keep approved files next to the test source files, in the same package directory. This is the default behaviour and the correct approach. Each approved file is next to the test that owns it. Thus it is easy to find and review, and you can see what it covers. When approved files are in a different place from their tests, navigation is more difficult. Also, the relation between the test and its expected result is less clear.

If a specific case needs it, see [file-control](file-control.md) for options to change file names and paths.

## IDE Run Templates

Add `fileMatcherPassOnCreate` and `fileMatcherUpdateInPlace` with the value `false` to the JUnit run template of your IDE. Then the properties are **visible and easy to change**. When you must create or update approved files, change `false` to `true` in the run configuration. You do not have to remember the property names.

**IntelliJ IDEA:**

1. Open **Run → Edit Configurations → Edit Configuration Templates → JUnit**
2. In the **VM options** field, add:

```
-DfMPOnCreate=false -DfMUInPlace=false
```

Or use the full property names:

```
-DfileMatcherPassOnCreate=false -DfileMatcherUpdateInPlace=false
```

When you must create or update approved files, change the values to `true` in the run configuration:
- **`fileMatcherPassOnCreate` / `fMPOnCreate`** — set it to `true` to pass the test when there is no approved file yet. With `false`, the test fails with "Not approved file created". Approvalcrest still writes the `-not-approved` file, so that you can review it.
- **`fileMatcherUpdateInPlace` / `fMUInPlace`** — set it to `true` to overwrite the existing approved files with the current actual output.

This gives a fast cycle:

1. Change the property to `true`.
2. Run the test.
3. Examine the generated approved file.
4. Change the property back to `false`.
5. Commit.

You do not have to remember the property names or type them.

**Tip:** before you commit, review `git diff` to make sure that the overwritten approved files contain the expected changes. After the update, always change the property back to `false`. If it stays `true`, each run overwrites the approved files, and real failures never show.

## Multiple Assertions in One Test

Each call of a file-based matcher gets its file name from the test class and the method name. If a test has more than one assertion with `sameJsonAsApproved()` or `sameContentAsApproved()`, all the calls use the same file name and collide. Use `.withUniqueId(String id)` on each call to give it a different file name:

```java
@Test
void checkBothStates() {
    assertThat(before, sameJsonAsApproved().withUniqueId("before"));
    assertThat(after,  sameJsonAsApproved().withUniqueId("after"));
}
```

The same rule applies to parameterized tests. Each iteration must pass a different ID, usually the parameter name or the index.

## Related

- [dynamic-values](dynamic-values.md)
- [file-control](file-control.md)
