<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=eb4371f0-15ff-46c6-b867-c365bff5a636" />

# file-control

Customise approved file names, paths, and update behaviour.

All options on this page apply to both `sameJsonAsApproved` and `sameContentAsApproved`.

## `.withUniqueId(String id)`

Adds an ID to the name of the approved file. You need this in two frequent cases:

**Several assertions in the same test method** — by default, each call of a file-based matcher in a test gets the same file name (class hash + method hash). Without a unique ID, the second assertion reads and writes the same file as the first, and the two collide:

```java
@Test
void myTest() {
    assertThat(result1, sameJsonAsApproved().withUniqueId("first"));
    assertThat(result2, sameJsonAsApproved().withUniqueId("second"));
    // → first-approved.json and second-approved.json — no collision
}
```

**Parameterized tests** — all iterations have the same method name, so each iteration needs a different ID:

```java
@ParameterizedTest
@MethodSource("cases")
void paramTest(String name, MyDto value) {
    assertThat(value, sameJsonAsApproved().withUniqueId(name));
    // → <methodHash>-<name>-approved.json per iteration
}
```

If `id` starts with `-`, approvalcrest does not add a second separator:

```java
assertThat(value, sameJsonAsApproved().withUniqueId("-scenario1"));
// → <methodHash>-scenario1-approved.json  (single dash)
```

## `.withFileName(String name)`

Replace the generated file name completely:

```java
assertThat(actual, sameJsonAsApproved().withFileName("my-custom-name"));
// → creates: <classHash>/my-custom-name-approved.json
```

## `.withPath(Path path)`

Override the directory of the approved files. Use this in specific cases where the default location (next to the test source) is not suitable. An example is a resources directory that the build does not compile:

```java
assertThat(actual, sameJsonAsApproved()
    .withPath(Paths.get("src/test/resources/approved")));
```

Usually the default behaviour is better: the approved files are next to the test class. Then the approved file and the test that owns it are together, and they are easy to find and review.

## `.withPathName(String relativeStr)`

Override the approved file directory with a path **relative to the source directory of the test class**. Approvalcrest accepts an absolute path as it is:

```java
assertThat(actual, sameJsonAsApproved()
    .withPathName("mydir"));
// → writes to: {testClassPath}/mydir/<methodHash>-approved.json
```

**Absolute path example:**

```java
assertThat(actual, sameJsonAsApproved()
    .withPathName("/abs/path"));
// → writes to: /abs/path/<methodHash>-approved.json
```

**Migration from pre-1.0.1:** before, `withPathName("mydir")` wrote to `mydir/` relative to the working directory. Move the approved files into the `{testClassPath}/mydir/` subtree.

## `.withRelativePathName(String relStr)`

Override the approved file directory with a path **relative to the working directory**. Approvalcrest adds the hash of the class name as a subdirectory:

```java
assertThat(actual, sameJsonAsApproved()
    .withRelativePathName("snapshots"));
// → writes to: {workingDir}/snapshots/<classHash>/<methodHash>-approved.json
```

**Migration from pre-1.0.1:** before, `withRelativePathName("snapshots")` wrote to `{testClassPath}/snapshots/`. Move the approved files to `{workingDir}/snapshots/<classHash>/`.

## Configuring the test source root

By default, approvalcrest stores approved files next to the test class under `src/test/java`. Your tests can be in a different source directory, for example integration tests under `src/it/java`, or Kotlin or Groovy tests under `src/test/kotlin`. Then set the `fileMatcherSourceRoot` property (alias `fmSourceRoot`) to that directory:

```bash
# Integration tests in their own source directory
mvn verify -DfileMatcherSourceRoot=src/it/java                              # Maven
./gradlew integrationTest -DfileMatcherSourceRoot=src/integrationTest/java  # Gradle

# Short alias
mvn verify -DfmSourceRoot=src/it/java                              # Maven
./gradlew integrationTest -DfmSourceRoot=src/integrationTest/java  # Gradle
```

The value is a path relative to the project root. It sets the base directory. To compute the path of an approved file, approvalcrest adds the package path of the test class to this directory. Thus `com.example.MyTest` with `-DfileMatcherSourceRoot=src/it/java` resolves to `src/it/java/com/example/`. When you set a per-matcher override (`.withPath`, `.withPathName`, `.withRelativePathName`), it still has priority.

The property applies to both ways to identify a test: stack-trace detection, and the injected `TestInfo` / `Description` / `Method`. Thus it cannot move the approved files of some tests and not of others.

On Gradle, the approvalcrest plugin sets the property for a test suite other than `test`, such as `integrationTest`. It does this only when the task has a source set of the same name, and only one of `src/<name>/java` and `src/<name>/kotlin` exists. A value that you set wins. See [getting-started](getting-started.md#gradle).

### Defaults per framework

All frameworks use the same property. Only the value **when you do not set it** is different. Thus a project that does not set it keeps the layout that its framework expects:

| Matchers | Default when the property is unset |
|---|---|
| JUnit 4, JUnit 5 Jupiter, TestNG | `src/test/java` |
| `approvalcrest-junit-jupiter-kotlin` | `src/test/kotlin` |

> **One property, one root.** There is only one property. If you set the property in a project with **both** Java and Kotlin tests, it moves both.
>
> For a mixed project that needs two different roots, do not set the property. Instead, use the per-matcher overrides for the set of tests that is not standard. If you set the wrong root, the test fails, because approvalcrest cannot find the approved file. It never compares with the wrong file.

### Keeping approved files under test resources

The approved files do not have to be next to the test sources. To keep them in a separate tree, set the root to a directory under `src/test/resources`. Approvalcrest still adds the package path and a directory for each test class, so the layout under the root stays the same.

<details><summary>Maven</summary>

Define the root as a property of your POM, and give it to the test JVM through Surefire. The `approvalcrest:dedup` and `approvalcrest:reinstate` goals read the same POM property, so they scan the same tree:

```xml
<properties>
    <fileMatcherSourceRoot>src/test/resources/approvalcrest</fileMatcherSourceRoot>
</properties>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <fileMatcherSourceRoot>${fileMatcherSourceRoot}</fileMatcherSourceRoot>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

Set the root on the `test` task. The `approvalcrestDedup` and `approvalcrestReinstate` tasks of the approvalcrest plugin read it from the `test` task, so they scan the same tree:

<!-- snippet: resources-root.kts -->
```kotlin
tasks.test {
    systemProperty("fileMatcherSourceRoot", "src/test/resources/approvalcrest")
}
```

Gradle copies `src/test/resources` to the test classpath. Thus a change to an approved file under the root runs the tests again.

</details>

<details><summary>Gradle (Groovy DSL)</summary>

Set the root on the `test` task. The `approvalcrestDedup` and `approvalcrestReinstate` tasks of the approvalcrest plugin read it from the `test` task, so they scan the same tree:

<!-- snippet: resources-root.gradle -->
```groovy
tasks.named('test') {
    systemProperty 'fileMatcherSourceRoot', 'src/test/resources/approvalcrest'
}
```

Gradle copies `src/test/resources` to the test classpath. Thus a change to an approved file under the root runs the tests again.

</details>

Thus the approved files of `com.example.MyTest` go to `src/test/resources/approvalcrest/com/example/<classHash>/`. Set the root in the build file, not only on the command line, because each run must find the same files. When you change the root, move the existing approved files yourself. Approvalcrest does not move them.

## Migration from pre-1.0.0 Without Moving Files

Before 1.0.0, `withPathName(str)` resolved relative to the **working directory**. From 1.0.0, it resolves relative to **testClassPath**. If your tests used `withPathName` with a path from the project root, approvalcrest now looks for the approved files in the wrong directory.

Split the old `withPathName` call into `withRelativePathName` (base from the working directory) + `withPathName` (subdirectory). Then the approved files stay **where they are**:

**Before (pre-1.0.0):**

```java
// Resolved from working directory → src/test/jsons/<file>
assertThat(actual, sameJsonAsApproved()
    .withPathName("src/test/jsons")
    .withFileName("my-snapshot"));
```

**After (1.0.0+):**

```java
// withRelativePathName resolves from workingDir, withPathName adds subdirectory
assertThat(actual, sameJsonAsApproved()
    .withRelativePathName("src/test")
    .withPathName("jsons")
    .withFileName("my-snapshot"));
```

Both give the same absolute path: `{projectRoot}/src/test/jsons/my-snapshot-approved.json`. The approved file stays where it is. You do not have to rename or move it.

**Resolution logic when both are set:**

```
fileNameWithPath = workingDirectory() / relativePathName / pathName / fileName
```

`workingDirectory()` is the project root. Thus this is the same as the old `withPathName` behaviour, which resolved directly from the working directory.

## In-Place Update

When you run the tests again with `-DfileMatcherUpdateInPlace=true`, approvalcrest overwrites the approved files with the current actual output:

```bash
mvn test -DfileMatcherUpdateInPlace=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true  # Gradle
```

Use this **locally** after an intentional change, for example a new field or a change to the serialisation. Review the diffs. Then commit the updated approved files.

**Never set this in the build that must pass before a merge.** A verification run must fail when the output differs from the approved file, and not accept the change. Also, each approved file must exist before the run starts. A deliberate regeneration job is different. You start it manually, and you review its output before you commit it. See [best-practices](best-practices.md#ci-workflow).

### Custom matchers during a regeneration run

This section is for JSON only. `sameContentAsApproved` compares text and has no custom matchers.

Approvalcrest evaluates custom matchers before the content comparison, and stops the assertion at the first failure. This also stops the rewrite. Then that file never regenerates, however many times the job runs again. Any later assertion in the same test method does not run either. Each later module also does not run, unless the build uses `--fail-at-end` on Maven or `--continue` on Gradle. `-DfileMatcherSkipCustomMatchersOnUpdate=true` defers the evaluation for that run:

```bash
mvn test -DfileMatcherUpdateInPlace=true -DfileMatcherSkipCustomMatchersOnUpdate=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true -DfileMatcherSkipCustomMatchersOnUpdate=true  # Gradle
```

It does nothing unless you also set `fileMatcherUpdateInPlace`. Thus it has no effect on a verification run, whatever the value of the property is.

**It applies to the full run.** Approvalcrest also skips the custom matchers of a test whose approved file is already current. Thus that test passes where it fails without the property. You lose nothing permanently. The verdict of a custom matcher comes from the actual object, never from the approved file. Thus no regeneration can fix a custom matcher failure, and the failure comes back on the next verification run.

But a regeneration run verifies nothing. Do not read its green result as a verification. For this reason, the property is off by default. Do not set it on any job other than the regeneration job.

What it changes, and what it does not:

- `.with()` and `.withMatcher()` still remove their field. Thus the regenerated file is byte-for-byte the same as the file from a run where all matchers passed. This is intentional: the file must be what the next verification run compares with.
- `.alsoCheck()` and `.alsoCheckMatching()` keep their field. Thus the regenerated file contains the value that the custom matcher rejects. Expect that test to fail on the next verification run.
- In this run, a field path that names nothing is not an error **for a file that needs a rewrite**. The reason is that the check for it is part of the evaluation. On a file that is already current, the deferred evaluation still runs, and still throws the error.
- A path that goes through a non-object, for example `beanChar.subpath`, still fails, as the same path under `.ignoring()` does. The failure comes from the removal of the field, not from the match.
- With `-DfmSharedEnabled=true`, a test that stopped at a failed matcher before can now attach to a shared canonical or detach from it.

## Machine-Readable / AI-Friendly Output

Enable structured output to let AI agents and CI tools parse the details of a test failure.

**Fluent API:**

```java
assertThat(actual, sameJsonAsApproved().withMachineReadableOutput());
```

**System property (applies to all tests in the run):**

```bash
mvn test -DfileMatcherMachineReadable=true        # Maven
./gradlew test -DfileMatcherMachineReadable=true  # Gradle

# or with the short aliases:
mvn test -DfmAI=true               # Maven
./gradlew test -DfmAI=true         # Gradle
mvn test -DfMMReadable=true        # Maven
./gradlew test -DfMMReadable=true  # Gradle
```

### Message format

All machine-readable messages are valid JSON objects in compact form (no whitespace). This uses fewer tokens for AI consumers. The exact fields depend on the type of failure.

**File matcher — content mismatch:**
```
{"failureType":"MISMATCH","test":"MyTest#myTestMethod","approvedFile":"/abs/path/to/4ac405/11b2ef-approved.json","action":"Set system property fMUInPlace=true and re-run to update the approved file","expected":"{...approved content...}","actual":"{...current serialized value...}","ignoredFields":[{"path":"createdAt","reason":"IGNORE_PATH"},{"path":"metadata","reason":"REMOVED_EMPTY","causes":["metadata.id","metadata.secret"]}],"aliasedFields":[{"path":"userId","originalValue":"a1b2c3d4-...","alias":"%%IGNORED-UUID%%"}],"sortedFields":[{"path":"items","reason":"SORT_PATH"},{"path":"tags","reason":"SORT_PATTERN","pattern":"a string containing \"tag\""}]}
```

**File matcher — no approved file yet:**
```
{"failureType":"NEW_FILE","test":"MyTest#myTestMethod","notApprovedFile":"/abs/path/to/4ac405/11b2ef-not-approved.json","approvedFile":"/abs/path/to/4ac405/11b2ef-approved.json","action":"Set system property fMUInPlace=true and re-run, or copy the not-approved file to approvedFile path"}
```

**Bean matcher — value mismatch:**
```
{"failureType":"MISMATCH","expected":"{...expected...}","actual":"{...actual...}","ignoredFields":[],"aliasedFields":[],"sortedFields":[]}
```

**Bean matcher — incompatible types:**
```
{"failureType":"TYPE_MISMATCH","expectedType":"com.example.Foo","actualType":"com.example.Bar","action":"Add .skipClassComparison() to the matcher, or set system property bMSCComparison=true"}
```

### ignoredFields tracking

The `ignoredFields` array records **which fields the filter actually removed**, and **why**. Each entry contains:

| Field | Description |
|-------|-------------|
| `path` | The dotted path of the removed field (e.g. `address.zipCode`) |
| `reason` | One of: `IGNORE_PATH`, `CUSTOM_MATCHER`, `CUSTOM_MATCHER_PATTERN`, `IGNORE_PATTERN`, `REMOVED_EMPTY` |
| `pattern` | *(only for pattern-based)* The matcher description that matched |
| `causes` | *(only for `REMOVED_EMPTY`)* Child fields whose removal left this parent empty |

**Reason values:**
- `IGNORE_PATH` — removed by `.ignoring("fieldPath")`
- `CUSTOM_MATCHER` — removed by `.with("fieldPath", matcher)` (validated separately, unless a regeneration run deferred it)
- `CUSTOM_MATCHER_PATTERN` — removed by `.withMatcher(patternMatcher, valueMatcher)`
- `IGNORE_PATTERN` — removed by `.ignoring(Matcher<String>)` applied to the JSON tree
- `REMOVED_EMPTY` — the parent became empty after the removal of its children

`.alsoCheck(...)` and `.alsoCheckMatching(...)` make no entry. They add an assertion and remove nothing, so there is nothing to report.

Approvalcrest applies type-based ignore rules (`.ignoring(Class)`) and pattern-based ignore rules (`.ignoring(Matcher)`) during the Gson serialisation, with an `ExclusionStrategy`. Thus it cannot record each field that these rules remove. When you configure these rules, a `"note"` field tells you this.

### aliasedFields tracking

The `aliasedFields` array records the value replacements that aliases made. Each entry contains:

| Field | Description |
|-------|-------------|
| `path` | The dotted path of the aliased field |
| `originalValue` | The original value, before the alias |
| `alias` | The replacement value |

### sortedFields tracking

The `sortedFields` array records **only the array fields that a sort you configured actually sorted**. Each entry contains:

| Field | Description |
|-------|-------------|
| `path` | The dotted path of the field whose array approvalcrest sorted (e.g. `items`, `order.lineItems`) |
| `reason` | One of: `SORT_PATH`, `SORT_PATTERN` |
| `pattern` | *(only for `SORT_PATTERN`)* The matcher description that matched |

**Reason values:**
- `SORT_PATH` — sorted by `.sortField("fieldPath")` or `.sortFieldPath(SortField.of("path"))`
- `SORT_PATTERN` — sorted by `.sortField(Matcher<String>)` or `.sortFieldMatcher(SortField.of(matcher))`

Approvalcrest applies a type-based sort (`.sortType(Class)`) during the Gson serialisation, with a field-name marker. Thus it cannot record the fields that a type-based sort changes. When you configure a type-based sort, the `"note"` field tells you about this limitation.

### Passive AI discovery tip

In the default (non-machine-readable) mode, every failure message ends with:

```
[AI tip] Re-run with system property fmAI=true for structured, machine-readable output.
```

Thus an AI agent that gets a usual test failure can find the machine-readable mode, without explicit configuration.

## Pointer Files and Shared Approvals

An approved file can contain a _pointer reference_ instead of content. Approvalcrest follows the pointer automatically, so you do not have to change the tests:

```
/*com.example.MyTest.myTestMethod*/
/*pointer:src/test/java/shared-approvals/a1/a1b2c3d4e5f6-4827-approved.json*/
```

**In-place update on a pointer file** (`-DfileMatcherUpdateInPlace=true`):

- With `-DfmSharedEnabled=false` (default): approvalcrest detaches the pointer and replaces it with a standalone approved file. The canonical does not change.
- With `-DfmSharedEnabled=true`: approvalcrest writes a new pointer if a canonical that matches exists. If not, it detaches the pointer as above.

After the tests, run `mvn approvalcrest:dedup` or `./gradlew approvalcrestDedup` to replace the detached files with pointers to shared canonicals again. Each needs the setup in [Adding the dedup tool](shared-approvals.md#adding-the-dedup-tool).

See [shared-approvals](shared-approvals.md) for the full deduplication workflow.

## Related

- [best-practices](best-practices.md)
- [same-json-as-approved](same-json-as-approved.md)
- [shared-approvals](shared-approvals.md)
