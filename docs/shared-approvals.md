<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=eb4371f0-15ff-46c6-b867-c365bff5a636" />

# Shared Approvals

Sometimes many tests verify identical content, for example all tests that assert the same email template rendered for the same user. Then the repository collects thousands of duplicate approved files. Shared approvals replace the duplicate files with small _pointer files_. Each pointer file refers to one _canonical_ approved file.

## How it works

### Pointer files (always active)

An approved file can contain a _pointer reference_ instead of JSON content:

```
/*com.example.EmailServiceTest.testSendWelcomeEmailToUser1*/
/*pointer:src/test/java/shared-approvals/a1/a1b2c3d4e5f6-4827-approved.json*/
```

The first line is the usual test comment header. The second line is a pointer comment, not JSON. It contains the path to the canonical file, relative to the working directory of the module (the project root).

When a test loads an approved file that contains a pointer, approvalcrest follows the pointer automatically and compares with the content of the canonical. **You do not have to configure anything** to follow pointers. In a checkout that contains committed pointer files and canonicals, the tests pass without any system properties.

### Canonical files

A canonical file is in the configured shared directory (`src/test/java/shared-approvals` by default), and contains the actual approved content. Approvalcrest makes its name from the SHA-256 hash prefix and the byte length of the content:

```
{sharedDir}/{bucket}/{sha256prefix12}-{byteLen}-approved.{ext}
```

For example:
```
src/test/java/shared-approvals/a1/a1b2c3d4e5f6-4827-approved.json
```

The `{bucket}` directory is the first N characters of the hash (default N=2, which gives 256 buckets). The hash prefix and the byte length together prevent hash collisions in practice, for practical file sizes. If the bucket depth is not in the supported range of 1–6, approvalcrest rejects it with a configuration error that names the property.

> **A change to the bucket depth moves every canonical.** The depth is part of the path that a pointer resolves to. After a change, approvalcrest cannot find the old canonicals, and writes duplicate content again under the new layout without a warning. To migrate, first run `mvn approvalcrest:reinstate` or `./gradlew approvalcrestReinstate`. It needs no depth, because each pointer holds the full path of its canonical. Then change the depth, and run `mvn approvalcrest:dedup` or `./gradlew approvalcrestDedup`. Each needs the setup in [Adding the dedup tool](#adding-the-dedup-tool).

## Deduplication workflow

### Adding the dedup tool

The dedup tool is in the `com.github.karsaig:approvalcrest-dedup` artifact.

<details><summary>Maven</summary>

To run its goals with Maven, declare the plugin in your POM:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.karsaig</groupId>
            <artifactId>approvalcrest-dedup</artifactId>
            <version>1.6.1</version>
        </plugin>
    </plugins>
</build>
```

Maven resolves the `approvalcrest:` prefix from the plugins that your POM declares. The goals need Maven 3.9.6 or later.

</details>

<details><summary>Gradle</summary>

The approvalcrest Gradle plugin adds the tasks `approvalcrestDedup` and `approvalcrestReinstate` (see [getting-started](getting-started.md#gradle)). They run the dedup tool of the plugin version. You do not declare the artifact.

The tasks have two options of their own. `--preview` prints the changes, and writes and deletes nothing. `--types json` limits the tool to one file type. Gradle reads `--dry-run` as its own option, which skips every task, so the tasks use `--preview`.

The tasks read `fileMatcherSharedDir`, `fileMatcherSharedDirBucketDepth` and `fileMatcherSourceRoot` from the `test` task and from the `-D` properties of the command line. They scan the source root of the `test` task only. For the approved files of another test suite, run the standalone JAR with `--dir`. The tasks do not read `-DdryRun=true`: a run with it changes files. Use `--preview`.

</details>

<details><summary>Gradle without the plugin</summary>

Run the standalone JAR from the project directory. Download `approvalcrest-dedup-VERSION-standalone.jar` from the `com/github/karsaig/approvalcrest-dedup` directory of Maven Central, or with Maven as the next paragraph shows. The JAR does not read the build script. When your `test` task sets `fileMatcherSourceRoot`, `fileMatcherSharedDir` or `fileMatcherSharedDirBucketDepth`, give the same value with `-D` before `-jar`:

```bash
java -DfileMatcherSourceRoot=src/test/resources/approvalcrest -jar approvalcrest-dedup-VERSION-standalone.jar
```

</details>

The standalone JAR is the same artifact with the `standalone` classifier. It contains all its dependencies, so `java -jar` runs it without a build tool. To download it into the current directory:

```bash
mvn dependency:copy -Dartifact=com.github.karsaig:approvalcrest-dedup:1.6.1:jar:standalone -DoutputDirectory=.
```

### One-time setup (no test re-runs required)

After your test suite generates all approved files, run the dedup tool. It does not run tests. It only reads, writes and deletes files:

```bash
mvn approvalcrest:dedup       # Maven
./gradlew approvalcrestDedup  # Gradle

# standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar
```

The tool reads its settings from the properties in [Configuration](#configuration).

**What the dedup tool does:**

1. Finds all `*-approved.{json|content}` files that are not pointer files yet, outside the shared directory
2. Groups files by content key (SHA-256 prefix + byte length, per file extension)
3. For each group of 2 or more identical files: writes one canonical to the shared directory, then replaces every group member with a pointer file
4. For files whose content already matches a canonical: writes a pointer immediately. This handles new tests from after an earlier dedup run.
5. Deletes each canonical in the shared directory that no pointer file refers to (garbage collection)
6. Prints a summary: canonicals created, pointers written, orphans removed

Each run is **idempotent**. A second run on a tree that is already deduplicated changes nothing.

**Scan directory and reference lookup.** `--dir` controls which files the tool *converts* into pointers. It does not control which canonicals are *in use*. The tool looks for references in the full project.

Without `--dir` (`<dir>` in the Maven plugin configuration), the tool scans the directory that `fileMatcherSourceRoot` (alias `fmSourceRoot`) names. Without the property, it scans `src/test/java`. The Gradle tasks have no `--dir` option. Without the property, they scan `src/test/kotlin` when the project has only that directory.

A pointer outside the scanned subtree needs its canonical as much as a pointer inside it. This is important when you make `--dir` narrower, or run the tool for each module against a shared directory for several modules. The tool does not touch the canonicals of the modules that you did not scan.

If the shared directory is outside the project directory, the tool cannot find the pointers that refer to it. Thus the tool skips garbage collection completely, and the summary tells you.

**Restrict to one file type.** The goals, the Gradle tasks and the standalone JAR accept a type selection. Thus a project can consolidate its JSON approved files and not touch its `.content` approved files, or the reverse:

```bash
mvn approvalcrest:dedup -DfileMatcherSharedTypes=json  # Maven
./gradlew approvalcrestDedup --types json              # Gradle

# standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar --types content
```

The selection limits only the conversion, and the deletion of canonicals. To decide whether a canonical is still in use, the tool always examines the pointers of all types. Both types use the same bucket directories. Thus a run for one type can never leave the pointers of the other type without their canonicals.

**The tool refuses directories that overlap.** If the shared directory contains the scan directory, every approved file in the scan tree looks like a canonical to the tool. In that case, the tool does not start, and thus does not delete those files.

### Subsequent test runs

Usual test runs work without configuration. After a code change that affects the output, approvalcrest finds a mismatch as usual. The in-place update behaviour depends on the value of `fmSharedEnabled`:

| `fmSharedEnabled` | Behaviour on content change |
|---|---|
| `false` (default) | Detaches the pointer and writes standalone content. The canonical does not change. |
| `true` | If a canonical that matches exists: writes a new pointer. If not: detaches the pointer, writes standalone content and prints a notice |

After a detachment, run `mvn approvalcrest:dedup` or `./gradlew approvalcrestDedup` again. This consolidates the tests that now have the same new content.

### Reinstate (undo deduplication)

To replace all pointer files with standalone approved files and clear the shared directory:

```bash
mvn approvalcrest:reinstate       # Maven
./gradlew approvalcrestReinstate  # Gradle

# or with the standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar --reinstate
```

Reinstate replaces the pointers under `--dir` with their content. Then it deletes the canonicals that nothing refers to any more. As with dedup, the reference lookup covers the full project. Thus, when you reinstate one module, the tool does not delete the canonicals that another module still points to.

Pass `-DdryRun=true` to a Maven goal, `--preview` to a Gradle task, or `--dry-run` to the standalone JAR. Then the tool prints the changes, and writes and deletes nothing:

```bash
mvn approvalcrest:reinstate -DdryRun=true   # Maven
./gradlew approvalcrestReinstate --preview  # Gradle

# standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar --reinstate --dry-run
```

Use reinstate when you:
- change the location of the shared directory (reinstate with the old configuration → dedup with the new configuration)
- stop the use of shared approvals completely

## Configuration

| Property | Alias | Default | Description |
|---|---|---|---|
| `fileMatcherSharedDir` | `fmSharedDir` | `src/test/java/shared-approvals` | Path to the shared directory for canonical files, relative to the project root |
| `fileMatcherSharedEnabled` | `fmSharedEnabled` | `false` | Turns on write-side integration. New approved files and in-place updates look for a canonical that matches. `true`/`all` for both types, `json` or `content` for one type, `false`/`none` for neither |
| `fileMatcherSharedDirBucketDepth` | `fmSharedDirBucketDepth` | `2` | The number of characters from the start of the hash that make the name of the bucket subdirectory (1–6) |
| `fileMatcherSourceRoot` | `fmSourceRoot` | `src/test/java` | The directory that the tool scans when `--dir` (`<dir>` in the Maven plugin configuration) is not set. The tests read the same property to find the approved files. |
| `fileMatcherSharedTypes` | — | `all` | The approved file types that the `approvalcrest:dedup` and `approvalcrest:reinstate` goals process: `json`, `content`, `json,content` or `all`. Only the Maven goals read it. The Gradle tasks and the standalone JAR take `--types` instead. It has no effect on the matchers at test time. |

For `fileMatcherSharedDir`, `fileMatcherSharedDirBucketDepth` and `fileMatcherSourceRoot`, the goals, the Gradle tasks and the standalone JAR read the long name and the alias, as the tests do. A long name and its alias with different values stop the tool with an `Ambiguous configuration` error. The goals also read these three properties from the `<properties>` of your POM. The Gradle tasks also read them from the `test` task.

> **Warning:** if you change `fileMatcherSharedDirBucketDepth` after you commit pointer files, all the pointers become invalid, because the canonical paths are different. Run `mvn approvalcrest:reinstate` or `./gradlew approvalcrestReinstate`, which needs no depth. Then change the depth, and run `mvn approvalcrest:dedup` or `./gradlew approvalcrestDedup`.

### Enabling write-side integration

To let the tests create pointers, set `-DfmSharedEnabled=true` when you run them:

```bash
# New test: creates a pointer not-approved file if a matching canonical exists
mvn test -DfmSharedEnabled=true        # Maven
./gradlew test -DfmSharedEnabled=true  # Gradle

# In-place update: writes a pointer if a matching canonical exists
mvn test -DfileMatcherUpdateInPlace=true -DfmSharedEnabled=true        # Maven
./gradlew test -DfileMatcherUpdateInPlace=true -DfmSharedEnabled=true  # Gradle
```

With `fmSharedEnabled=false` (the default), approvalcrest still **reads** pointer files. It does not create new pointer files automatically during test runs. The deduplication itself always runs in the dedup tool.

**Restrict to one file type.** The property accepts a type as well as a boolean. Thus you can limit the write side to the type that you ran the tool for:

```bash
# Only JSON approvals point at canonicals; .content files keep their own copy
mvn test -DfmSharedEnabled=json        # Maven
./gradlew test -DfmSharedEnabled=json  # Gradle

# Both, which is what true means
mvn test -DfmSharedEnabled=true        # Maven
./gradlew test -DfmSharedEnabled=true  # Gradle
```

The property accepts `true` and `all` (both types), `json`, `content`, a comma-separated list, and `false` or `none`. It pairs with the type selection of the tool: `-DfileMatcherSharedTypes` for the Maven goals, and `--types` for the Gradle tasks and the standalone JAR. For example, dedup with `--types json` to create only JSON canonicals. Then run tests with `-DfmSharedEnabled=json`. Thus new `.content` files do not get pointers to canonicals that the tool never created for them.

## Stale pointers

A stale pointer is a pointer whose target path is outside the current `fmSharedDir`. For example, someone renamed the shared directory, and the pointers still refer to the old path. The `fMStrictMatching` flag controls the behaviour. This is the same flag that controls the ignore behaviour:

| `fMStrictMatching` | Behaviour |
|---|---|
| `true` (default) | The test fails with a "Stale pointer" error. The error gives the target path, and tells you to run reinstate + dedup |
| `false` | Approvalcrest follows the pointer, whether or not the target is in `fmSharedDir` (lenient mode) |

A pointer whose target file does not exist always fails.

## CLI options

```
java -jar approvalcrest-dedup-VERSION-standalone.jar [options]

  --dir <path>          Scan directory (default: fileMatcherSourceRoot, else src/test/java)
  --shared-dir <path>   Shared approvals directory (default: src/test/java/shared-approvals)
  --bucket-depth <n>    Bucket prefix depth 1-6 (default: 2)
  --types <list>        Approved file types to process: json, content, json,content or all
                        (default: all)
  --dry-run             Print what would happen without writing files
  --reinstate           Replace all pointers with standalone content and clear shared dir
```

## CI integration example

Run dedup as a step after the tests in CI. It automatically consolidates approved files that became identical:

<details><summary>Maven</summary>

```yaml
# GitHub Actions example
- name: Run tests
  run: mvn test

- name: Deduplicate approved files
  run: mvn approvalcrest:dedup

- name: Commit deduplicated approvals (if changed)
  run: |
    git diff --quiet || (git add -A && git commit -m "chore: deduplicate approved files")
```

</details>

<details><summary>Gradle</summary>

```yaml
# GitHub Actions example
- name: Run tests
  run: ./gradlew test

- name: Deduplicate approved files
  run: ./gradlew approvalcrestDedup

- name: Commit deduplicated approvals (if changed)
  run: |
    git diff --quiet || (git add -A && git commit -m "chore: deduplicate approved files")
```

</details>

## Pointer file format reference

A pointer file has two lines:

```
/*{testClass}.{testMethod}*/
/*pointer:{relativePathToCanonical}*/
```

- Line 1: the usual comment header (the same as in every approved file)
- Line 2: `/*pointer:` followed by the path relative to the **project root** (working directory), then `*/`
- The extension (`.json` or `.content`) matches the original approved file

## Related

- [file-control](file-control.md)
- [system-properties](system-properties.md)
