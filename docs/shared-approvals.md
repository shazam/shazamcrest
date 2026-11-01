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

> **A change to the bucket depth moves every canonical.** The depth is part of the path that a pointer resolves to. After a change, approvalcrest cannot find the old canonicals, and writes duplicate content again under the new layout without a warning. To migrate, first run `mvn approvalcrest:reinstate` with the old depth. Then run `mvn approvalcrest:dedup` with the new depth.

## Deduplication workflow

### One-time setup (no test re-runs required)

After your test suite generates all approved files, run the dedup tool. It does not run tests. It only reads, writes and deletes files:

```bash
# as a Maven plugin (reads fmSharedDir from system property or POM)
mvn approvalcrest:dedup

# standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar
```

**What the dedup tool does:**

1. Finds all `*-approved.{json|content}` files that are not pointer files yet, outside the shared directory
2. Groups files by content key (SHA-256 prefix + byte length, per file extension)
3. For each group of 2 or more identical files: writes one canonical to the shared directory, then replaces every group member with a pointer file
4. For files whose content already matches a canonical: writes a pointer immediately. This handles new tests from after an earlier dedup run.
5. Deletes each canonical in the shared directory that no pointer file refers to (garbage collection)
6. Prints a summary: canonicals created, pointers written, orphans removed

Each run is **idempotent**. A second run on a tree that is already deduplicated changes nothing.

**Scan directory and reference lookup.** `--dir` controls which files the tool *converts* into pointers. It does not control which canonicals are *in use*. The tool looks for references in the full project.

A pointer outside the scanned subtree needs its canonical as much as a pointer inside it. This is important when you make `--dir` narrower, or run the goal for each module against a shared directory for several modules. The tool does not touch the canonicals of the modules that you did not scan.

If the shared directory is outside the project directory, the tool cannot find the pointers that refer to it. Thus the tool skips garbage collection completely, and the summary tells you.

**Restrict to one file type.** Both goals accept a type selection. Thus a project can consolidate its JSON approved files and not touch its `.content` approved files, or the reverse:

```bash
mvn approvalcrest:dedup -DfileMatcherSharedTypes=json
java -jar approvalcrest-dedup-VERSION-standalone.jar --types content
```

The selection limits only the conversion, and the deletion of canonicals. To decide whether a canonical is still in use, the tool always examines the pointers of all types. Both types use the same bucket directories. Thus a run for one type can never leave the pointers of the other type without their canonicals.

**The goals refuse directories that overlap.** If the shared directory contains the scan directory, every approved file in the scan tree looks like a canonical to the tool. In that case, both goals do not start, and thus do not delete those files.

### Subsequent test runs

Usual test runs work without configuration. After a code change that affects the output, approvalcrest finds a mismatch as usual. The in-place update behaviour depends on the value of `fmSharedEnabled`:

| `fmSharedEnabled` | Behaviour on content change |
|---|---|
| `false` (default) | Detaches the pointer and writes standalone content. The canonical does not change. |
| `true` | If a canonical that matches exists: writes a new pointer. If not: detaches the pointer, writes standalone content and prints a notice |

After a detachment, run `mvn approvalcrest:dedup` again. This consolidates the tests that now have the same new content.

### Reinstate (undo deduplication)

To replace all pointer files with standalone approved files and clear the shared directory:

```bash
mvn approvalcrest:reinstate

# or with the standalone JAR
java -jar approvalcrest-dedup-VERSION-standalone.jar --reinstate
```

Reinstate replaces the pointers under `--dir` with their content. Then it deletes the canonicals that nothing refers to any more. As with dedup, the reference lookup covers the full project. Thus, when you reinstate one module, the tool does not delete the canonicals that another module still points to.

Pass `--dry-run` (or `-DdryRun=true` for the Maven goal) to print what would happen. The tool then writes and deletes nothing:

```bash
mvn approvalcrest:reinstate -DdryRun=true
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
| `fileMatcherSharedTypes` | — | `all` | The approved file types that `approvalcrest:dedup` and `approvalcrest:reinstate` process: `json`, `content`, `json,content` or `all`. Only the tool uses it. It has no effect on the matchers at test time. |

> **Warning:** if you change `fileMatcherSharedDirBucketDepth` after you commit pointer files, all the pointers become invalid, because the canonical paths are different. Run `mvn approvalcrest:reinstate` with the old depth. Then run `mvn approvalcrest:dedup` with the new depth.

### Enabling write-side integration

To let the tests create pointers, set `-DfmSharedEnabled=true` when you run them:

```bash
# New test: creates a pointer not-approved file if a matching canonical exists
mvn test -DfmSharedEnabled=true

# In-place update: writes a pointer if a matching canonical exists
mvn test -DfileMatcherUpdateInPlace=true -DfmSharedEnabled=true
```

With `fmSharedEnabled=false` (the default), approvalcrest still **reads** pointer files. It does not create new pointer files automatically during test runs. The deduplication itself always runs in the dedup tool.

**Restrict to one file type.** The property accepts a type as well as a boolean. Thus you can limit the write side to the type that you ran the tool for:

```bash
# Only JSON approvals point at canonicals; .content files keep their own copy
mvn test -DfmSharedEnabled=json

# Both, which is what true means
mvn test -DfmSharedEnabled=true
```

The property accepts `true` and `all` (both types), `json`, `content`, a comma-separated list, and `false` or `none`. It pairs with the `--types` / `-DfileMatcherSharedTypes` option of the tool. For example, dedup with `--types json` to create only JSON canonicals. Then run tests with `-DfmSharedEnabled=json`. Thus new `.content` files do not get pointers to canonicals that the tool never created for them.

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

  --dir <path>          Scan directory (default: src/test/java)
  --shared-dir <path>   Shared approvals directory (default: src/test/java/shared-approvals)
  --bucket-depth <n>    Bucket prefix depth 1-6 (default: 2)
  --types <list>        Approved file types to process: json, content, json,content or all
                        (default: all)
  --dry-run             Print what would happen without writing files
  --reinstate           Replace all pointers with standalone content and clear shared dir
```

## CI integration example

Run dedup as a step after the tests in CI. It automatically consolidates approved files that became identical:

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
