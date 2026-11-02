<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=308d73c7-f1a0-403c-8df9-56ae120707f0" />

Changelog
===========

Version 1.6.1 - Unreleased
-----
- Added the Gradle plugin `io.github.karsaig.approvalcrest`, for Gradle 8.14 and later. It passes each `-D` property of the command line to the test JVM, so `./gradlew test -DfileMatcherUpdateInPlace=true` works as `mvn test` does. It makes the approved files inputs of each `Test` task, so a changed approved file runs the tests again. A run with `fileMatcherUpdateInPlace` or `fileMatcherPassOnCreate` is never up to date and never comes from the build cache. For a test suite other than `test` with a source set of the same name, it sets `fileMatcherSourceRoot` to the source directory of that suite, when that directory is unambiguous. It adds the tasks `approvalcrestDedup` and `approvalcrestReinstate`, and `approvalcrest { attachAgent = true }` attaches the agent. See [getting-started](docs/getting-started.md#gradle).
- The docs show the Gradle setup in a block next to each Maven block, for the Kotlin DSL and the Groovy DSL. They also say that you must add Hamcrest to the test classpath, because each approvalcrest artifact declares it with the `provided` scope. See [getting-started](docs/getting-started.md).
- `approvalcrest-dedup` is now on Maven Central. No earlier release published it. To run `mvn approvalcrest:dedup` and `mvn approvalcrest:reinstate`, declare the plugin in your POM, as [shared-approvals](docs/shared-approvals.md#adding-the-dedup-tool) shows. The goals need Maven 3.9.6 or later. The standalone JAR is the same artifact with the `standalone` classifier.
- `mvn approvalcrest:dedup` and `mvn approvalcrest:reinstate` read `-DdryRun=true`. A dry run then changes no files. The summary of a dry run starts with `Dry run, no files changed.`, on the goals and on the standalone JAR with `--dry-run`. Before, the summary of a dry run read the same as the summary of a run that changed the files.
- A dry run of reinstate with a narrowed scan directory now counts only the canonicals that a real run deletes. Before, it also counted a canonical that a pointer outside the scan directory still used. The real run kept that canonical.
- **[breaking]** Removed the `useApprovedDirectory` property (alias `uADirectory`). Approvalcrest read the property, but no code used the value. Also removed `TestMetaInformation.getApprovedDirectory()`, `FileMatcherConfig.isApprovedDirectory()`, and the approved-directory parameter of the `FileMatcherConfig` and test meta constructors. **Migration:** delete `getApprovedDirectory()` from your own `TestMetaInformation`. In each `FileMatcherConfig` constructor call, delete the third `boolean`. In each meta constructor call, delete the approved-directory argument, for example the last argument of `Junit4TestMeta(Path, String, String, Path)`. The `AbstractTestMetaBase` constructor that takes a working directory now takes it as its first argument. The compiler finds each call that must change. To keep approved files under `src/test/resources`, set `fileMatcherSourceRoot`, as [file-control](docs/file-control.md#keeping-approved-files-under-test-resources) shows.
- `mvn approvalcrest:dedup` and `mvn approvalcrest:reinstate` read the same properties as the tests, aliases included: `fileMatcherSharedDir` / `fmSharedDir` and `fileMatcherSourceRoot` / `fmSourceRoot`. `mvn approvalcrest:dedup` also reads `fileMatcherSharedDirBucketDepth` / `fmSharedDirBucketDepth`. The goals also read these properties from the `<properties>` of your POM. A long name and its alias with different values stop the goal with an `Ambiguous configuration` error. See [shared-approvals](docs/shared-approvals.md#configuration).
- Without `--dir` (`<dir>` in the Maven plugin configuration), the dedup tool now scans the directory that `fileMatcherSourceRoot` (alias `fmSourceRoot`) names. Without the property, it scans `src/test/java`, as before. Thus a project that keeps its approved files elsewhere, for example under `src/test/resources`, does not have to give the directory two times.
- `approvalcrest-junit-jupiter-kotlin` now depends on `approvalcrest-junit-jupiter`, and its jar holds only the Kotlin classes. Before, the jar held a reduced copy of `approvalcrest-junit-jupiter` without `com.github.karsaig.approvalcrest.jupiter.MatcherAssert`. The POM did not list the dependency. Thus a project with only the Kotlin artifact could not compile the `assertThat` import that [kotlin](docs/kotlin.md) shows.
- The error for a missing `--add-opens` now gives the Gradle flag as two arguments: `jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")`. Before, it gave `jvmArgs '--add-opens java.base/java.time=ALL-UNNAMED'`, and the test JVM did not start with that line: `Unrecognized option`.

Version 1.6.0 - 2026/09/05
-----
- Added `-DfileMatcherSkipCustomMatchersOnUpdate=true` (alias `fMSCMOUpdate`). It defers custom matchers while `-DfileMatcherUpdateInPlace=true` regenerates approved files. When a custom matcher fails, approvalcrest does not regenerate the approved file of that assertion. Without the flag, that file stays out of date, however often the regeneration job runs again. Also, later assertions in the same test method do not run. The flag applies to the full run. Thus approvalcrest also skips the matchers of a test whose approved file is already current. That test passes where it would fail without the flag. The failure comes back on the next verification run, and that run decides whether the regenerated files are correct. The default does not change, and the flag does nothing unless an in-place update runs. `.with` and `.withMatcher` still remove their field. Thus the regenerated file is the file that a run would give if all matchers passed. `.alsoCheck` and `.alsoCheckMatching` keep their field. Thus approvalcrest writes the rejected value, and that test fails on the next run. JSON matchers only. See [file-control](docs/file-control.md).
- An ordering matcher with the wrong kind of number no longer stops the test with a `ClassCastException`. `.with("someLongField", greaterThan(0))` matches nothing, because a whole number gets to the matcher as a `Long`, and Hamcrest compares only values of the same boxed type. That does not change. The defect was in the code that builds the message for that mismatch. That code threw an exception, with a stack trace that named Hamcrest internals, not the field. The message now names the field, the value, the type that the value came as, and the cast that failed. The same ordering matcher in `allOf`, `both().and()`, `hasItem` or `everyItem` threw an exception before there was a verdict. Over an `int` field, that form now passes. This is the only case where a result changes, not only a message. Warning: one combination still cannot fail. `not(greaterThan(...))` and its counterparts pass, whatever the data is, when the boxed types are different. See [custom-matching](docs/custom-matching.md).
- Added `.alsoCheck(path, matcher)` and `.alsoCheckMatching(fieldNamePattern, matcher)`. They apply a custom matcher to a field *in addition to* the comparison. `.with(...)` and `.withMatcher(...)` apply a custom matcher *instead of* the comparison. The older pair removes the matched field, so the field never gets into the approved file, and a later change to the value cannot fail. This is correct for a generated id. It is not correct for a value that you can predict, but want a stronger guarantee about. The two older methods do not change, and no approved file changes. The approved file of a `.with(...)` call never contained the field. If you change the call to `.alsoCheck(...)`, regenerate that file. Re-run with `-DfileMatcherUpdateInPlace=true`. Approvalcrest regenerates the file if the new matcher passes. If you implement `CustomisableMatcher` yourself, you must add the two methods. One side effect: with `.with(...)` and machine-readable output together, the order of the `ignoredFields` array can be different, but its entries do not change. See [custom-matching](docs/custom-matching.md).
- A class that inherits fields from a locked JDK module now serialises on a JDK where approvalcrest cannot get to those fields. The most frequent example is your own exception class. It extends `RuntimeException`, and thus has the private fields of `Throwable`. Before, serialisation of such a class threw `JsonIOException: Failed making field 'java.lang.Throwable#detailMessage' accessible`. A locked type uses getter-based output, but that class did not. The reason was that the module of the class is not locked, so approvalcrest never sent the class to the getter-based path. This applies only where approvalcrest serialises the inherited fields. For example, a class that extends `java.util.EventObject` keeps its output, because the only field of `EventObject` is transient and approvalcrest never read it. This affects only runs where approvalcrest cannot open any module, which today means JDK 26 or `fallback` mode. All other runs do not change. **[Correction]** JDK 26 does not deny the Unsafe memory-access methods by default; it only warns. Such runs are `fallback` mode, or a JDK that denies the methods when the agent is not attached, for example with `--sun-misc-unsafe-memory-access=deny`.
- **[breaking]** On JDK 26 and later, types from locked JDK modules (`java.time.*`, `java.lang.*`, `java.math.*`, and others) no longer serialise as an empty `{}`. JDK 26 disables `sun.misc.Unsafe`, but does not remove it. Thus the default safe mode continued to use it, read nothing, and wrote an object with no fields. An approved file could match `{}` and report a pass, when the comparison compared nothing. Safe mode now checks that Unsafe can still read a field before it uses Unsafe. Where Unsafe cannot, safe mode serialises those types from their public getters. On JDK 8 to 25, the output does not change, and the approved files do not change. **Migration:** on JDK 26, attach the new agent below to keep the old field-based output. Alternatively, re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files for locked-module types. **[Correction]** JDK 26 does not deny the Unsafe memory-access methods by default; it only warns, so its output stays field-based. This entry applies when the JDK denies the methods: on JDK 24 or later with `--sun-misc-unsafe-memory-access=deny`. JEP 471 plans this as the default of a later JDK.
- Added `approvalcrest-agent`, an optional Java agent. It keeps the output identical on JDK 26, with no `--add-opens` and no approved files to regenerate. Add it as a test dependency. Then pass one JVM argument. On JDK 8 to 25, the output does not change, whether or not you attach the agent. Thus you can add it before JDK 26 arrives. Force mode still requires `--add-opens`. **[Correction]** On JDK 26, the output is also identical without the agent, because JDK 26 only warns. The agent is necessary when the JDK denies the Unsafe memory-access methods. See [field-access-modes](docs/field-access-modes.md#the-agent).
- A `*` segment no longer matches fields that are not in the serialised output: `static`, `transient` and compiler-generated fields. Before, it read the other constants of an enum. It also followed the hidden reference from a non-static inner class to its enclosing instance. Thus an assertion could pass on data outside the object under comparison. It also failed next to an empty collection or a sibling that it could not get to. Then it reported a path that the caller never wrote.
- A collection matcher now behaves the same when a path names the field and when a name pattern names it. `withMatcher(...)` gave the matcher the raw serialised form of a collection-valued field. Thus `.with("tags", hasSize(2))` passed, but `.withMatcher(is("tags"), hasSize(2))` failed on the same field. `iterableWithSize` worked in both.
- A `*` path segment now means "every named child at this position": a map key, an object property or a bean field. Thus `.with("ordersByRef.*.trackingCode", notNullValue())` and `.ignoring("ordersByRef.*.generatedId")` apply to every entry of a map. Before, you had to name each key. As the *last* segment, `*` keeps its old meaning: a key with the literal name `*`. Collections and arrays need no wildcard, because the path already goes through them: `orders.trackingCode` already means "in every order". See [custom-matching](docs/custom-matching.md) and [ignoring-fields](docs/ignoring-fields.md).
- When you ignore a wildcard path, approvalcrest can remove the container too. Sometimes the ignored field is the only field of each map value. Then each value becomes empty, so each entry disappears, then the array, then the map field, keys included. A named key limits this to one entry. `*` applies to all entries at the same time, so usually the full field disappears. Check what the comparison still covers. Do not read a pass as proof that approvalcrest compared the map.
- `sortField(...)` accepts the same `*` segment. Thus a sort path can get to the collection under every map entry, and you do not have to name each key. A wildcard and a literal key at the same level both apply, and one does not override the other. The ignored sub-paths of a `SortField` also accept `*`. Thus you can keep a field out of the sort key under every child. Before, approvalcrest read a `*` there as a literal key name. Nothing matched, and the field stayed in the key without a warning. The approved file then recorded an order that nobody asked for. See [sorting](docs/sorting.md).
- `ignoringElementsWhere(...)` also accepts it, so you can filter a `Map<String, List<T>>`. `ordersByRef.*.system` filters the list under each entry. `ordersByRef.system`, without the wildcard, filters the outer entry array instead, and matches nothing. A named key gets to the same list as the wildcard, for that one entry.
- A field path can now go through a `Set`-typed or `Map`-typed field. Thus `.with("ordersByRef.A-1.trackingCode", …)` and `.with("tagSet.name", …)` work. Before, both failed with `… does not exist`, even when they were well-formed. The reason was that approvalcrest writes such a field under a prefixed key, and the path resolver did not recognise that key. `.ignoring(...)` and `.sortField(...)` already worked. A path without the map key still does not get to the values. A path goes through a map by key, and does not fan out over it. See [custom-matching](docs/custom-matching.md).
- An ignored field in a circular-reference graph now applies to all objects in that graph, not only to the first object with the field. Before, approvalcrest filtered the two sides differently. Thus the assertion failed on unrelated data, and the message pointed to the wrong field. This affected only graphs with more than one object that has the ignored field.
- A negated collection matcher can now fail. Before, `.with("orders", not(empty()))` passed on an empty collection. The same was true for `not(hasSize(n))`, `not(hasItem(x))`, `not(contains(...))` and `not(containsInAnyOrder(...))`. These assertions could never fail, whatever the data was. They now report their mismatch. This change does not fix one combination, and no change can fix it. See the next entry.
- Warning: one combination still cannot fail: `not(hasItem(x))`, `not(contains(...))` and `not(containsInAnyOrder(...))`, when all three conditions apply. The input is a raw JSON string. The elements are objects, not scalars. The matcher is for the class of the element. Compare the object, not a JSON string. Alternatively, write the matcher for the JSON shape. Over scalar elements, all of them work on both input forms. See [custom-matching](docs/custom-matching.md).
- Documented what `.with(path, matcher)` does when the path names a collection, map or array, and which Hamcrest matchers work for each input form. `sameJsonAsApproved` accepts a raw JSON string as well as an object. The two forms differ in what they can assert about the elements of a collection. See [custom-matching](docs/custom-matching.md).
- **[breaking]** Sometimes a map holds two entries whose key and value both serialise identically. Approvalcrest now writes each such entry as its own key/value pair. Before, the two entries came out as two copies of one four-element array, which did not show the map correctly. This affects only maps whose keys are not primitives, `String`s or enums, and only where two entries collide. Approvalcrest writes every other map byte-for-byte as before. An approved file that records the old shape now fails. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the affected approved files.
- **[breaking]** When you ignore everything under a `Set`-typed or `Map`-typed field, approvalcrest now removes the field. Before, it left `"ordersByRef": []` in the approved file. A usual field that became empty in the same way already disappeared. This affects only fields that the ignore rules make completely empty. Strict mode is on by default. Thus an approved file that still holds the empty collection now fails. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the affected approved files.
- **[breaking]** When you ignore a field in an object in a collection, approvalcrest no longer removes the other values in that collection. Before, `.ignoring("list.a")` over `{"list":[{"a":1},"keep-me",42]}` made the collection empty and then removed the field. Thus it removed two values that the rule did not name. Approvalcrest filtered both sides in the same way, so the assertion still passed. Warning: one shape still becomes empty, because approvalcrest cannot tell it from a map entry. That shape is a collection of exactly two elements, in another collection, reduced to plain values. A collection that a field holds is safe, whatever its length. An approved file that approvalcrest generated while it removed those values now fails. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the affected approved files.
- **[breaking]** `sortType(...)` now gets to a collection field whose declared type is a subclass, such as `class OrderList extends ArrayList<Order>`. Before, `Order` in `sortType` had no effect on such a field. The sort skipped the field, so approvalcrest compared it in the order in which it came. Thus the recorded order of such a field changes. Approvalcrest still sorts only the types that you name. `sortType` does not sort a collection field unless it names the element type. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files that hold such a field. See [sorting](docs/sorting.md).
- `withMatcher(Matcher<String>, ...)` now gets to a `Set`-typed or `Map`-typed field. Approvalcrest holds such a field under an internal name. Before, it tested the pattern against that internal name and not the name of the field, so the pattern never matched. A pattern that matches no field passes. Thus `.withMatcher(is("tagSet"), hasSize(2))` was an assertion that could not fail, whatever the data was. It now behaves as the path-based `.with("tagSet", ...)` always did. Fields of all other types already worked. See [custom-matching](docs/custom-matching.md).
- `.ignoring(Matcher<String>)` no longer removes a `Set`-typed or `Map`-typed field for a pattern that matches only its internal name, such as `startsWith("!")`. Also, machine-readable output now reports a field that approvalcrest removed or aliased in such a field under its declared name. Before, the path contained the internal name. A pattern that matches the declared name of a field works as before.
- Machine-readable output now lists an ignore rule that took effect below the top of its path. It also lists it one time, not two times. Before, it did not record `.ignoring("a.b.c")` when `a.b` kept its other fields. Also, it counted two times a rule that matched both the actual value and the approved JSON. It still lists element indexes, and parents that approvalcrest removed because they became empty, for each occurrence. This affects only the report. What approvalcrest compares does not change.
- Documented a map key that looks like a circular-reference wrapper key (`0x` followed by lowercase hex). Path navigation skips such a key, as if it were a wrapper key. Thus a path that leaves out such a key still goes through it. A path that leaves out any other key matches nothing. See [ignoring-fields](docs/ignoring-fields.md).
- **[breaking]** A map whose keys are not primitives, `String`s or enums now records which half of an entry was the key. Approvalcrest writes such a map as `[key, value]` pairs. Before, it reordered the pair by its content. Thus `{a: z}` and `{z: a}` wrote the same bytes, and an approved file for one matched the other. The pair now keeps its order at every level that the declared type describes. This includes a map in another map, in a `Set`, or under a list of lists. It also includes subclasses such as `EnumMap` or your own subclasses, wherever the declaration names the value type. The pair also keeps its order at the root. There, approvalcrest examines the object itself, not a declared type. Approvalcrest still sorts the entries and a collection-valued half. As a result, the entry order can change. Regenerate the files. Do not edit them by hand. With `sameBeanAs`, this applies in all cases. No property turns it off, and there is no file to regenerate. An assertion that passed only because the comparison treated a map and its transpose as equal now fails. Rewrite that assertion. For a file matcher, it applies in strict mode, which is the default. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files that hold such a map. See [sorting](docs/sorting.md).
- Corrected a wrong entry in the released 1.0.1 notes. It said that a sort of an array of arrays keeps the order of the elements in each inner array. In fact, approvalcrest sorts the inner arrays together with the outer array, and no release behaved differently. The only exception is the `[key, value]` pair of a complex-key map. That pair is two positions, not a collection, and it keeps its order at any depth (see the entry above). The 1.0.1 entry keeps its wrong claim, with a correction note after it. Thus anyone who followed its migration advice can see that the advice was never necessary.
- A map with a `null` key no longer stops the comparison. Before, it threw a `NullPointerException` before approvalcrest wrote any file, and the other keys of the map decided whether it threw. In a map whose other keys are primitives, `String`s or enums, approvalcrest now records the key as the member name `"null"`. Where any other key causes the `[key, value]` form, approvalcrest writes each key as a value in the entry. Thus it writes a `null` key as a bare JSON null. Then a `null` key and a `String` key of `"null"` look the same. Approvalcrest records both entries, but a comparison of one map with the other reports a match. See [sorting](docs/sorting.md).

Version 1.5.0 - 2026/07/29
-----

- `SortField.ignoring(...)` now returns a new `SortField`, and does not change the receiver. Before, if you derived two configurations from one base, you got one object with both sets of ignores. Thus a `SortField` in a shared configuration collected the paths of all callers. The paths that a caller got depended on the order of the calls. It was also not safe in parallel execution. Chains (`SortField.of("x").ignoring("a").ignoring("b")`) do not change. Code that called `ignoring(...)` for its side effect, and did not use the result, must now use the returned instance.
- `assertThrows` no longer treats a skipped test or a JVM-level error as the exception under test. It now rethrows an assumption failure (`TestAbortedException`, `AssumptionViolatedException`, or the `SkipException` of TestNG). Thus the test framework reports the test as skipped, and approvalcrest does not compare it with the expected exception. It rethrows a `VirtualMachineError` as it is, and does not match or wrap it. You can still assert that a matcher fails: `assertThrows` still matches `AssertionError` intentionally.
- When approvalcrest cannot write an approved file during an in-place update, the error now names that file. Before, the message contained the object under comparison instead. Thus the message identified no file, and put the full serialised content into the exception, the log and the CI output.
- `ignoringElementsWhere` now rejects a null value or a null matcher where you declare the rule, and names the path. Before, it failed later in the comparison with a `NullPointerException` that did not name the rule or the path.
- Reflection failures now keep their original cause. Before, an `IllegalAccessException`, `InvocationTargetException` or `SecurityException` all gave the same "Cannot access field" message. Thus you could not check the `--add-opens` guidance in the message.
- The Kotlin matchers now use `fileMatcherSourceRoot` (alias `fmSourceRoot`). Thus a Kotlin project whose tests are outside `src/test/kotlin` can configure the directory of the approved files. Before, the Kotlin matchers ignored the property, and there was no way to change the directory. Only the default is different for each framework: `src/test/kotlin` for the Kotlin matchers, `src/test/java` for the others. Thus projects that do not set the property do not change. If you set the property in a project with Java and Kotlin tests, it now moves both. See [file-control](docs/file-control.md).
- Fixed a `StackOverflowError` in the comparison of a graph of `Throwable`s that refer to each other in a cycle. This affected only throwables. Approvalcrest already handled cyclic beans that are not throwables correctly.
- `fileMatcherSharedEnabled` (alias `fmSharedEnabled`) now accepts a file type as well as a boolean. Thus you can limit the write-side shared-approval integration to `json` or `content`. Before, it always covered both. `true` and `false` keep their meanings, and `true` is the same as `all`. Thus no current configuration changes its behaviour. The dedup tool has a `--types` option for the same selection. See [shared-approvals](docs/shared-approvals.md).
- You can now limit `approvalcrest:dedup` and `approvalcrest:reinstate` to one approved file type with `-DfileMatcherSharedTypes=json|content` (or `--types` on the standalone CLI). Thus a project can consolidate its JSON approvals and not touch its `.content` approvals. The default is both types, so current runs do not change. The selection limits only the conversion and the deletion of canonicals. To decide whether any pointer still refers to a canonical, the tool always examines the pointers of all types. Thus a run for one type cannot leave the pointers of the other type without their canonicals. See [shared-approvals](docs/shared-approvals.md).
- A test method that a class inherits from an **abstract** base class now fails with an error message. Before, all the subclasses used one approved file without a warning. Each subclass resolved to the same file and overwrote the others. The reason is that the stack trace, and the `Method` that TestNG injects, can report only the class that declares the method. To get one approved file for each subclass, pass `TestInfo` (JUnit 5). With JUnit 4, use `Junit4DesciptionWatcher`. Alternatively, supply a custom `TestMetaInformation`. Concrete test classes do not change.
- **Behaviour change.** Approvalcrest no longer merges `Set` elements that serialise to the same JSON into one entry. It sorts sets by their JSON representation to keep the output stable. Before, that sort also removed duplicates. Thus it removed elements that are not `equals()`, but serialise identically. This occurs with classes that do not override `equals()`, or when approvalcrest does not serialise the field that makes two instances different. A set that lost or got such an element then serialised the same as the original set, so the difference could not fail a test. Approved files that contain such a set now differ. You must re-approve them. The content that approvalcrest recorded before was incomplete. To get the old behaviour while you migrate, set `-DapprovalcrestLegacySetCollapse=true` (alias `aLSCollapse`). See [supported-types](docs/supported-types.md).
- Approvalcrest now recognises object-graph reference ids with hexadecimal letters in all places where it parses them. From the tenth object in a graph, an id contains the letters `a`-`f`. The throwable adapter did not accept these letters.
- `mvn approvalcrest:dedup` no longer deletes shared canonical files that pointer files outside the scan directory still refer to. The scan directory still controls which files the tool converts. But the tool now examines the full project to decide whether a canonical is still in use. Before, a narrower scan deleted those canonicals. This also occurred when you ran the goal for each module against a shared directory for several modules. The pointers then resolved to content that did not exist on disk.
- `mvn approvalcrest:reinstate` and `--reinstate` now support `--dry-run` / `-DdryRun=true`. Before, the command line accepted the flag, but the tool ignored it. Thus a run that you intended as a preview rewrote every pointer and deleted every canonical. **[Correction]** The Maven goal ignored `-DdryRun=true` until 1.6.1. No release before 1.6.1 published `approvalcrest-dedup`.
- `approvalcrest:reinstate` now deletes only the canonicals that nothing refers to any more. Before, it always cleared the shared directory.
- Both dedup goals now refuse to run when the shared directory contains the scan directory. Before, they treated every approved file as a canonical and deleted it. One typo in `--shared-dir` was sufficient for this, because the default shared directory is in the default scan directory.
- Approvalcrest now supports package-private test methods. JUnit Jupiter and TestNG both allow test methods that are not public. But approvalcrest looked for the current test only in the public methods. Thus such a test failed with "Cannot determine test method", and did not resolve its approved file. Approvalcrest now also identifies a test method reliably when an overload has the same name.
- Approvalcrest now reads approved files correctly when their comment header ends with a CRLF, not a LF. Approvalcrest writes headers with a LF. Thus, before this fix, on a Windows checkout with `core.autocrlf=true`, it did not remove the header, and compared the comment as part of the content. On such a checkout, it also did not recognise pointer files as pointers.
- Approvalcrest now validates `fileMatcherSharedDirBucketDepth` (alias `fmSharedDirBucketDepth`) against its documented range of 1–6, and reports a configuration error that names the property. Before, a value out of the range failed with an unrelated `StringIndexOutOfBoundsException` from inside the matcher. See [shared-approvals](docs/shared-approvals.md).

Version 1.4.2 - 2026/07/28
-----

- Added the `fileMatcherSourceRoot` property (alias `fmSourceRoot`) to configure the test source directory for approved files. The default is `src/test/java`. When tests are outside the standard directory, set it to a value such as `src/it/java` or `src/test/kotlin`.

Version 1.4.1 - 2026/07/12
-----

- Added `ignoringElementsWhere` to remove array elements from the comparison by the value of a nested field. The path points to a field in each element. `ignoringElementsWhere` filters the innermost array on the path. Before the comparison, it removes each element whose leaf field value satisfies the given matcher, or equals the given String. The path goes through the collections before the innermost array automatically. Thus a path such as `entry.resource.meta.tag.system` filters the `tag` array of every `entry`. This works with `sameBeanAs` and `sameJsonAsApproved`. See [ignoring-fields](docs/ignoring-fields.md).

Version 1.4.0 - 2026/06/29
-----

- Added pointer-file support for approved files, and a new `approvalcrest-dedup` module. When the content of an approved file after the header is `/*pointer:relative/path*/`, approvalcrest follows the pointer automatically. It does this when it reads the file, with no configuration. The test comment header stays. An in-place update (`-DfMUInPlace=true`) on a pointer file detaches the test from the shared canonical, and does not change the canonical. `-DfmSharedEnabled=true` turns on write-side integration. When it is on, a new file whose content matches a canonical gets a pointer `-not-approved` file for the developer to approve. An in-place update then writes a pointer to a canonical that matches, when one exists. The configurable shared directory (`-DfmSharedDir=`, default `src/test/java/shared-approvals`) is the authoritative location for canonical files. The `approvalcrest-dedup` module has `mvn approvalcrest:dedup` (group duplicates → create canonicals → write pointers, GC orphans). It also has `mvn approvalcrest:reinstate` (replace all pointers with standalone approved files and clear the shared directory). See [shared-approvals](docs/shared-approvals.md).
- Removed the `buildIndex` / `bFIndex` flags from `FileMatcherConfig`. `FileMatcherConfig` declared them, but nothing used them.
- Fixed `JsonMatcher`, which passed without a warning during an in-place update when a custom matcher (`.with(fieldPath, matcher)`) failed. Approvalcrest regenerated the approved file, and the assertion passed. Thus the same custom matcher failed again on the next usual run, with no visible cause. Approvalcrest now does not change the file, and the assertion fails.

Version 1.3.4 - 2026/06/24
-----

- Fixed path-based custom matchers (`.with("fieldPath", matcher)`), which failed with `IllegalArgumentException: <field> does not exist` on a `Map<String, Object>`, a `List<Map<String, Object>>` or a JSON string input. A path through a collection now collects values from the elements that have the field. It fails only when no element has the field. Before, one element without the field made the full path fail. For a map, an element without the field is the usual case, because each key is in its own element. Heterogeneous collections behave in the same way.

Version 1.3.3 - 2026/06/17
-----

- Fixed `withAlias` / `withAliasMap`, which approvalcrest applied to the approved JSON in strict mode (`fileMatcherStrictFileMatching=true`, the default). Approvalcrest already skipped all other transformations on the approved side in strict mode: `.ignoring()`, `.with()` and pattern-based sorts. Aliases were the only exception. Now, in strict mode, approvalcrest applies aliases only to the actual side. This agrees with the strict-mode contract, which uses the approved file as it is. Approvalcrest now detects an approved file that still contains the raw (non-aliased) value as stale. The assertion fails. Regenerate the file. Approved files that already contain the aliased form (the usual case) do not change, and need no regeneration.
- Added the AI tip to the "not approved file created" failure message. The message that is not machine-readable now ends with `[AI tip] Re-run with fmAI=true for the absolute path of the file that needs to be renamed.` Before, only content-mismatch failures had the tip. Also fixed the machine-readable `action` field for `NEW_FILE` failures. Before, it suggested `fMUInPlace=true`. That is incorrect: an in-place update throws when there is no approved file yet. The correct action is to rename or copy the generated file.
- Added the AI tip to the type-mismatch failure message (`sameBeanAs` with incompatible types). The message that is not machine-readable now ends with the standard `[AI tip] Re-run with fmAI=true for structured, machine-readable output.` tip, as content-mismatch failures do.
- Fixed `JsonMatcher.describeTo()`, which applied transformations (aliases, removal of ignored fields, custom sorts) to the approved JSON in strict mode. This did not agree with `doMatches()`. In strict mode, approvalcrest uses the approved file as it is on the expected side of the comparison. But `describeTo()` applied aliases to the same content and removed fields from it, without a warning. Thus the "Expected:" section of the Hamcrest failure message showed different values from the diff. For usual (up-to-date) approved files, this difference had no effect. For stale approved files, the failure message contradicted itself.

Version 1.3.2 - 2026/06/03
-----

- Approvalcrest now writes machine-readable JSON output in compact form (no pretty-printing), so that AI consumers use fewer tokens. For all failure types (`MISMATCH`, `NEW_FILE`, `TYPE_MISMATCH`), approvalcrest serialises the `expected` and `actual` field values and the outer JSON wrapper without whitespace.
- Fixed path-based operations (`.ignoring("path")`, `.with("path", matcher)`, `.sortField("path")`, `SortField.ignoring("path")`), which failed when the path went through a type in which approvalcrest detected a circular reference. Approvalcrest writes such types in synthetic `0xN` wrapper keys, and these keys stopped the path resolution. Path navigation now skips these wrapper keys automatically. Thus your path works identically, whether or not the target type has circular references.

Version 1.3.1 - 2026/06/01
-----

- Fixed `Optional<Interface>` and `Optional<AbstractClass>`, which approvalcrest wrote as an empty `{}`. Where the declared type parameter is an interface or an abstract class, approvalcrest now uses the runtime type. See [supported-types](docs/supported-types.md).
- Fixed the serialisation of polymorphic base classes in `Optional`. An `Optional<Animal>` that holds a `Dog` instance now serialises all the fields of `Dog`. See [supported-types](docs/supported-types.md).
- Added custom serialisers for `OptionalInt`, `OptionalLong` and `OptionalDouble`. Approvalcrest now serialises these primitive-specialised Optional types in the same `{"value": N}` / `{}` format as `java.util.Optional`. See [supported-types](docs/supported-types.md).
- Added `GsonConfiguration.addTypeToSkipInFallbackFactories(Class<?>)`. With it, you can tell `UnsafeFieldTypeAdapterFactory` and `GetterBasedTypeAdapterFactory` not to claim specific types. Then custom `TypeAdapter`/`TypeAdapterFactory` registrations have priority, for example for the Vavr types `Option` and `Either`. See [supported-types](docs/supported-types.md).
- Added documentation: a migration guide for paths ([file-control](docs/file-control.md)), and best practices for IDE run templates ([best-practices](docs/best-practices.md)).

Version 1.3.0 - 2026/06/01
-----

- Machine-readable output is now structured JSON. With `-DfileMatcherMachineReadable=true` (or `withMachineReadableOutput()`), failure messages are valid JSON objects. They contain `failureType`, `test`, `approvedFile`/`approveTo`, `expected` and `actual`. They also contain the metadata arrays `ignoredFields`, `aliasedFields` and `sortedFields`, which record the transformations that approvalcrest applied. See [file-control](docs/file-control.md).
- Added `fmAI` as one more short alias for the machine-readable output system property (`-DfmAI=true`). The passive AI discovery tip at the end of failures that are not machine-readable now names `fmAI`, because it is shorter. The original alias `fMMReadable` stays valid. See [system-properties](docs/system-properties.md).
- Confirmed that records and sealed classes work on JDK 17 and above.
- Fixed a serialisation defect for generic types with Immutables `@Gson.TypeAdapters`. Types that you register with `GsonBuilder.registerTypeAdapterFactory` no longer cause an `IllegalStateException` during recursive adapter resolution.
- When `LenientTypeAdapterFactory` catches an `IllegalStateException`, it now logs the full exception, with the stack trace. It also logs repeated warnings for the same type only one time, to keep the log short.

Version 1.2.0 - 2026/05/30
-----

- Removed the need for the `--add-opens` JVM flag. Approvalcrest now opens modules from code at runtime. It uses a field access strategy with three levels: force-open the module → getter-based fallback → direct field reflection. On JDK 9+, you do not need JVM flags. See [field-access-modes](docs/field-access-modes.md).
- Added the `approvalcrest-testng` module. It is a TestNG 6+ adapter with all the features of the other adapters: `sameBeanAs`, `sameJsonAsApproved`, `sameContentAsApproved`, DataProvider support, and optional `Method` injection (TestNG 6.14.2+). See [testng](docs/testng.md).

Version 1.1.0 - 2026/05/29
-----

- **[breaking]** Gson now serialises null fields by default. Before, the JSON serialisation left out fields with a null value, and nothing told you. Thus `.ignoring("field")` failed without a warning when the field value was null in a collection element. The serialisation removed the field before the ignore logic ran, so the ignore logic never removed the ignored element from the collection. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files that now include null fields. To get the old behaviour for all tests, set `-DapprovalcrestSerializeNulls=false`. To get it for one matcher, call `.withoutSerializingNulls()` on the matcher. See [ignoring-fields](docs/ignoring-fields.md).
- Added short alias names for all boolean system properties. You can now also set each property with a short alias, for example `-DfMUInPlace=true` instead of `-DfileMatcherUpdateInPlace=true`. You can set the full property name and an alias to the same value. If you set them to different values, approvalcrest throws `IllegalStateException` at startup. See [system-properties](docs/system-properties.md) for the full alias table.
- Extended the machine-readable output mode (`-DfileMatcherMachineReadable=true` / `withMachineReadableOutput()`). Failure messages now include structured headers: `FAILURE_TYPE: MISMATCH|NEW_FILE|TYPE_MISMATCH`, `TEST: ClassName#methodName`, `APPROVED_FILE:` or `APPROVE_TO:` (absolute path), and `ACTION:` with the exact property to set. In the mode that is not machine-readable, each failure message ends with the passive AI discovery tip. Thus AI agents can find the mode also in the default output. See [file-control](docs/file-control.md).
- Fixed the Kotlin extension functions of `DiagnosingCustomisableMatcher`. The Kotlin extension functions file did not have `withMachineReadableOutput()`, `withoutSerializingNulls()` and `sortType()` (added in 1.0.1). Thus you could not use them freely in Kotlin chains. Added the same extensions for `JsonMatcher`, and `withMachineReadableOutput()` for `ContentMatcher`.
- Added the Kotlin-only factory `Matchers.sameBeanAsType<T>(expected: Any)`. It creates a `DiagnosingCustomisableMatcher<T>` from any object, without an `as Any` cast. Use it for structural comparisons where the actual and expected objects have different classes. Use it together with `.skipClassComparison()`: `sameBeanAsType<Any>(expected).skipClassComparison()` instead of `sameBeanAs(expected as Any).skipClassComparison()`.

Version 1.0.1 - 2026/05/28
-----

- Added the `approvalcrest-junit-jupiter-kotlin` module: wrappers and matchers for JUnit Jupiter, for use from Kotlin. Updated Kotlin to 2.1.21 and Dokka to 1.9.20. See [kotlin](docs/kotlin.md).
- Added Kotlin extension functions for `DiagnosingCustomisableMatcher`, `JsonMatcher` and `ContentMatcher` in the `approvalcrest-junit-jupiter-kotlin` module. They avoid a limitation of the Kotlin type inference with F-bounded (recursive) generics ([KT-5464](https://youtrack.jetbrains.com/issue/KT-5464)). Chained calls such as `sameBeanAs(expected).with("field", myMatcher)`, `sameJsonAsApproved<T>().ignoring("field").withUniqueId("id")` and `sameContentAsApproved<T>().withUniqueId("id")` now compile in Kotlin without errors.
- Added the fluent method `sortType(Class<?>...)` to `sameBeanAs` and `sameJsonAsApproved`. Approvalcrest sorts collections and arrays whose element type matches one of the specified classes automatically, during the comparison and when it creates the file. You do not need a `sortField` path. It works in the same way as the automatic sort of sets, which already applied to unordered collections. See [sorting](docs/sorting.md).
- Added the `withMachineReadableOutput()` fluent API and the `-DfileMatcherMachineReadable=true` system property. When one of them is on, approvalcrest replaces assertion failure messages with structured text that AI agents and CI pipelines can act on. File matchers write the absolute path to the approved file and a full `=== ACTUAL (full) === / === END ACTUAL ===` block. Bean matchers write the expected and actual content inline, in `=== EXPECTED (full) === / === ACTUAL (full) ===` blocks. The IDE diff view still works, because `getExpected()`/`getActual()` on the thrown exception still return values. See [file-control](docs/file-control.md).
- Fixed JUnit 4 `ComparisonFailure.getMessage()`, which `ComparisonCompactor` changed when `withMachineReadableOutput()` was on. The structured text now stays as it is. `getExpected()`/`getActual()`, and thus the IDE diff view, still work.
- `approvalcrest-junit-jupiter` is compatible with JUnit Platform 6 (verified against 6.0.3) on Java 17, 21 and 25. See [junit5-jupiter](docs/junit5-jupiter.md).
- **[breaking]** Added strict mode (`fileMatcherStrictFileMatching` system property, on by default). In strict mode, `ignoring()` removes fields only from the actual side. Thus approved files that contain the value of an ignored field now fail. **Migration:** re-run tests with `-DfileMatcherUpdateInPlace=true` to regenerate the approved files without the ignored fields. To get the old two-sided behaviour for all tests, set `-DfileMatcherStrictFileMatching=false`. In tests, the config object uses the last boolean: `new FileMatcherConfig(false, false, false, false, false, /*strict=*/false)`. See [ignoring-fields](docs/ignoring-fields.md).
- **[breaking]** Added a type comparison to `sameBeanAs`. It fails with a message when the actual and expected objects have incompatible runtime types. **Migration:** add `.skipClassComparison()` to the matcher: `assertThat(actual, sameBeanAs(expected).skipClassComparison())`. Alternatively, set `-DbeanMatcherSkipClassComparison=true` to turn off the check for all tests. See [same-bean-as](docs/same-bean-as.md).
- Added `withAliasMap(AliasMap)` / `withAlias(value, alias)` / `withAlias(field, value, alias)`. They replace values that change from run to run (UUIDs, timestamps) with stable aliases, before the comparison and before approvalcrest creates the file. Approvalcrest applies aliases after the ignore rules and before the sort. See [aliasing](docs/aliasing.md).
- Added `withMatcher(Matcher<String>, Matcher<V>)`: a pattern-based custom matcher. It applies to all fields, at any depth, whose name matches the supplied `Matcher<String>`. `sameBeanAs` and `sameJsonAsApproved` both support it. See [custom-matching](docs/custom-matching.md).
- When the Java-bean reflection path is not available, custom matchers now use the JSON-serialised form. An example is a JSON string input to `sameJsonAsApproved`.
- Fixed the sort of root-level `List` / array inputs with `sortField("")`.
- Fixed the sort order to be bottom-up: approvalcrest sorts nested arrays before it calculates the key of their parent.
- **[breaking]** Fixed the sort of arrays of arrays: approvalcrest reorders only the outer array, and keeps the element order of the inner arrays. Before, `sortField("groups")` on a `groups: [[Z,B],[A,C]]` also sorted the inner arrays into `[[A,C],[B,Z]]`. Now the inner arrays stay as they are, and only the outer order changes. **Migration:** if the element order of the inner arrays is important, change the inner collection type to `Set`. Approvalcrest sorts a `Set` automatically. Alternatively, add an explicit `sortField("innerFieldName")` for the inner field. See [sorting](docs/sorting.md). **[Correction, 1.5.1]** this entry is wrong, and no release behaved as it describes. When you configure the sort of the outer array, approvalcrest sorts the inner arrays together with the outer array. Thus the migration advice above was not necessary, but it did no harm. A `Set` and an inner `sortField` both still sort.
- Fixed three defects in the filter of the sort key. The filter did not remove complex fields from the sort key. Multi-level paths (for example `addr.city`) did not work. `SortField<Matcher<String>>.ignoring()` had no effect.
- Fixed `withMatcher` pattern ignores in `sameBeanAs`, which approvalcrest applied after the sort, not before.
- Fixed an error when an ignore path fanned out through an array of primitives.
- Fixed the ignore of all fields of a bean `Map` key: approvalcrest now removes the full entry.
- Fixed an ignored field in a `Map` value, which left an orphaned empty array.
- **[breaking]** Changed the approved file location of `withPathName(relativeStr)`. It was `{relativeStr}/{file}` (relative to the working directory), and is now `{testClassPath}/{relativeStr}/{file}`. Example: `.withPathName("mydir")` wrote to `mydir/cd3006-approved.json`, and now writes to `{testClassPath}/mydir/cd3006-approved.json`. **Migration:** move the approved files into the `{testClassPath}` subtree. Absolute paths (for example `/abs/path`) do not change. See [file-control](docs/file-control.md).
- **[breaking]** Changed the approved file location of `withRelativePathName(relStr)`. It was `{testClassPath}/{relStr}/{file}`, and is now `{workingDir}/{relStr}/{classHash}/{file}`. The base is now the working directory, and approvalcrest adds the hash of the class name. Example: `.withRelativePathName("snapshots")` wrote to `{testClassPath}/snapshots/cd3006-approved.json`, and now writes to `{workingDir}/snapshots/87668f/cd3006-approved.json`. **Migration:** move the approved files to the new location. See [file-control](docs/file-control.md).
- **[breaking]** `withUniqueId(id)` when `id` starts with `-`: before, it gave a double separator (`hash--myid-approved.json`). Now it gives `hash-myid-approved.json`. Example: `.withUniqueId("-scenario1")` wrote `cd3006--scenario1-approved.json`, and now writes `cd3006-scenario1-approved.json`. **Migration:** rename the affected approved files to remove the extra `-`. See [file-control](docs/file-control.md).
- Changed the error for an absent `--add-opens` flag. It now gives the exact flag, and shows Maven/Gradle snippets that you can paste.
- Added `toString()` to `Junit4TestMetaBase` and `Junit5TestMetaBase` (`TestMeta[cn=…,mn=…,cp=…,ad=…,wd=…]`).

Version 1.0.0 - 2026/05/28
-----

Broken release: the artifacts on Maven Central were incomplete. Do not use it. All features are available in 1.0.1.

Version 0.62.3 - 2024/05/27
-----

- Fixed approvalcrest to work with JDK 17 & 21
- Updated dependencies to Guava 33.2.0-jre, Gson 2.11.0, Commons-lang3 3.14.0
- The approvalcrest tests now run with the newer JUnit5 5.10.2

Version 0.61.6 - 2023/08/28
-----

- Added support for directory name override
- Updated dependencies to Guava 32.1.1
- The approvalcrest tests now run with the newer JUnit5 5.4.0

Version 0.61.2 - 2022/11/17
-----

- Updated dependencies to Guava 31.1, Gson 2.10, Commons-lang3 3.12.0

Version 0.61.1 - 2022/07/07
-----

- Updated Gson to 2.9.0

Version 0.60.3 - 2021/04/20
-----

- Fixed a defect with an empty approved JSON file

Version 0.60.2 - 2021/04/19
-----

- Fixed a defect in the serialisation of exceptions
- Fixed a defect with an empty approved JSON file

Version 0.60.0 - 2021/04/18
-----

- Upgraded JUnit to the latest versions and made them provided dependencies. Thus your project supplies the JUnit version, and approvalcrest is easier to use with different JUnit versions.
- The JUnit 5 modules now require JUnit 5.7.0+
- **Non-backward compatible change!** Added an automatic sort of field names, so the approved files and the diff view show the fields in natural order. Without this sort, pull requests had changes with no reason. Only the serialisation order changed. Constructs where the order is important (for example Lists) do not change. This sort is on by default, and assertions fail when the approved file is not sorted. To get the **old behaviour**, use **"-DsortInputFile=true"**. The aim of this change was to stop these changes in pull requests. A longer migration would add such changes to many pull requests.
- Added support to sort parts of JSON files. Approvalcrest does not sort some collections by default, and their order is not deterministic. These can cause flaky tests. You can now sort them to make the tests stable. When you use this, approvalcrest also requires a sorted approved file. You can change this with **"-DsortInputFile=true"**.
- Fixed many defects. Some ignores did not work. `JsonMatcher` did not work correctly for a String that contains JSON. The same matcher worked differently for different inputs. Assert failures sometimes had no description and no actual / expected content.
- **Non-backward compatible change!** Ignored values should no longer be visible in approved files. This is backward compatible for some ignores, but not for all. Update the approved files.
- Made assertions for JUnit 4 and 5 work in the same way, so there should be no difference between the two. Thus JUnit 5 assertion errors do not contain the full actual / expected content in the descriptions. That content is already in the exception, which the major IDEs support. The description contains only the difference.
- Added one more convenience method for some ignores
- **Non-backward compatible change!** Until now, an assertion on an exception ignored the exception type. The type is now in the serialised format, and approvalcrest asserts it. Approvalcrest now ignores the stack trace of exceptions automatically. Without an ignore, the stack trace caused frequent test failures, and everyone had to add an ignore in many places.
- Extended support for floating point numbers.
- Fixed a defect where files and directories could have wrong permissions in some cases
- Preliminary Kotlin support.

Version 0.56.3 - 2020/09/13
-----

 - Fixed the permissions of created directories and files. A defect caused permission problems in some cases.
 - Fixed defects in the pass-on-create flag
 
Version 0.56.2 - 2020/05/30
-----

 - Fixed sameBeanAs return value

Version 0.56.1 - 2020/05/24
-----

 - Fixed a serialisation problem with OffsetTime
 - Upgraded Guava and Gson versions

Version 0.56 - 2020/05/18
-----

 - Added a new package for the JUnit5 Jupiter matcher. Thus you can migrate JUnit 4 projects gradually.

Version 0.55.4 - 2020/05/18
-----

 - Fixed illegal reflective access warnings

Version 0.55.3 - 2020/05/12
-----

 - The pom file was still not in the release

Version 0.55.2 - 2020/05/09
-----

 - Added fileMatcherUpdateInPlace as an alias for jsonMatcherUpdateInPlace
 - Fixed the partial previous release

Version 0.55 - 2020/05/03
-----

 - Fixed the dependencies in the released pom file
 - Added support for custom TestMetaInformation
 - Added support for parameterized Junit 5 tests
 - Added serialisation support for nio.Path

Version 0.54 - 2020/04/28
-----

 - Removed Java 6 support. Approvalcrest now requires Java 8
 - Upgraded dependencies
 - Added Junit 5 support
 - Added new Gson serialisers for util.Date, java.time.* and java.lang.Class, because many users asked for them
 - Added a preliminary assertThrows implementation (the serialisation format will change soon)
 - Fixed an NPE

Version 0.21 - 2019/02/21
-----

 - Added support to skip circular-reference detection for a field
 - Upgraded Gson to the latest version

Version 0.19 - 2018/09/06
-----

 - Fixed the circular-reference detection to skip ignored fields
 - Enabled custom matchers in JsonMatcher
 - Added a convenience method to set field ignores

Version 0.18 - 2018/08/09
-----

 - Fixed an NPE in sameJsonAs with data-driven tests.

Version 0.17 - 2018/01/29
-----

 - Added a flag for the in-place update of approved files. With one command-line property (jsonMatcherUpdateInPlace=true), you can regenerate the approved files of all tests that a change affects.
 - Fixed custom matchers for inherited fields

Version 0.16 - 2017/08/20
-----

 - Fixed an NPE in path ignore
 - Changed the words of some error messages

Version 0.15 - 2017/04/22
-----

 - First release of Approvalcrest
 - Added new matchers sameJsonAsApproved and sameContentAsApproved
 - Updated dependencies
 - Added the option to configure custom type adapters

Version 0.11 - 2015/03/04
-----

You can now ignore all the fields whose name matches a given Hamcrest matcher.
Fixed the diagnostic when the actual value is null.

Version 0.10 - 2015/02/16
-----

Added automatic detection of circular references.
Fixed the comparison of Guava Optional.

Version 0.9 - 2014/09/17
-----

Fixed random comparison failures for sets and maps.

Version 0.8 - 2014/07/16
-----

Approvalcrest now handles circular references.

Version 0.7 - 2013/10/20
-----

Fixed a NullPointerException when you apply a custom matcher to a null object.

Version 0.6 - 2013/10/16
-----

Approvalcrest now uses the IsEqual Hamcrest matcher to compare Enums.

Version 0.5 - 2013/10/14
-----

The ComparisonFailure now keeps the description that you give to assertThat.

Version 0.4 - 2013/10/10
-----

Added the option to match specific fields with custom matchers.
Approvalcrest now uses the IsEqual Hamcrest matcher to compare Strings and primitives.

Version 0.3 - 2013/08/16
-----

Added the option to ignore specific fields or Java types in the comparison.

Version 0.2 - 2013/05/15
-----

Fixed Map serialisation.

Version 0.1 - 2013/03/20
-----

Initial release.
