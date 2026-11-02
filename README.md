<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=61ed9090-b10b-49a1-8e28-7950aacf9ddd" />

Approvalcrest
===========
[![Java CI with Maven](https://github.com/karsaig/approvalcrest/actions/workflows/maven.yml/badge.svg)](https://github.com/karsaig/approvalcrest/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/karsaig/approvalcrest/graph/badge.svg)](https://codecov.io/gh/karsaig/approvalcrest)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.karsaig/approvalcrest.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.karsaig%22%20AND%20a:%22approvalcrest%22)

## Contents

- [What is Approvalcrest](#what-is-approvalcrest)
- [AI-friendly mode](#ai-friendly-mode)
- [Version support](#version-support)
- [Installation](#installation)
- [Documentation](#documentation)
- [Changelog](CHANGELOG.md)

## What is Approvalcrest

Approvalcrest is a library of Hamcrest matchers for Java and Kotlin tests. It compares whole objects and whole text, not single fields. It works with JUnit 4, 5 and 6 and TestNG, on Maven and Gradle. It does two jobs:

- **Deep comparison:** `sameBeanAs(expected)` compares two Java objects in memory, field by field, through the full object graph. The matcher serialises both objects to JSON and compares the two results. It does not use the `equals()` method of the objects, and this is intentional. An `equals()` method is often not sufficient for a test. It can leave out fields, it can contain a defect, or it can return `true` for two objects that differ in an important way.

- **Approval testing**, also called snapshot, [golden master or characterisation testing](https://en.wikipedia.org/wiki/Characterization_test): `sameJsonAsApproved()` serialises the actual object to JSON and compares the result with an *approved file*. You commit the approved file next to the test. `sameContentAsApproved()` does the same for text content, not for objects. Examples are templates, API responses, rendered HTML and log output.

The first run of an approval test fails and writes a `-not-approved` file. You review the file and rename it to `-approved`. Later runs compare the output with it. After an intended change, run the tests with `-DfileMatcherUpdateInPlace=true`, and review the change in version control.

A usual assertion checks only the fields that it names. A change in any other field passes, and so does a field that someone adds later. Approvalcrest is default-deny, like a firewall that drops each packet without an allow rule. `sameBeanAs` and the approved files cover every field, so each change fails the test until you allow it. You allow a change in one of three ways: approve the new file, update the expected object of `sameBeanAs`, or name the field in [`.ignoring()`](docs/ignoring-fields.md). An approval or an update allows that one change; `.ignoring()` allows every change in the field.

This helps when an AI agent changes the code: an approved file finds unintended side effects that per-field assertions do not find. You can use the library at each level of test. Unit, integration, API, UI and system tests all use the same API.

Approvalcrest started from [Shazamcrest](https://github.com/shazam/shazamcrest) as its base. It extends and changes the functions of Shazamcrest.

## AI-friendly mode

Enable machine-readable output to let AI agents and CI tools parse the details of a test failure:

```java
// Fluent API:
assertThat(actual, sameJsonAsApproved().withMachineReadableOutput());
```
```bash
# Or via system property (short alias fmAI also works):
mvn test -DfileMatcherMachineReadable=true        # Maven
./gradlew test -DfileMatcherMachineReadable=true  # Gradle
```

In this mode, a failure message is a JSON object. It contains the expected content, the actual content and the path of the approved file. It also contains three arrays. The `ignoredFields` array gives each field that the matcher removed, and the reason. The `aliasedFields` array gives each value replacement. The `sortedFields` array gives each array that the matcher sorted. See [file-control.md](docs/file-control.md) for the details.

## Version support

| Dimension | Supported | Tested on |
|---|---|---|
| JDK | 8+ | 8, 11, 17, 21, 25, 27 (27 with and without the agent) |
| JUnit | 4, 5, 6 | 4 (4.13.2), 5 (5.14.4), 6 (6.1.0) |
| TestNG | 6+ | 6.14.3, 7.10.2 |
| Kotlin | 2.1+ | 2.2.21 |
| Gradle | 8.14+ | 8.14.6 on JDK 8, 11, 17, 21 (plugin); 9.8.2 on JDK 17, 21, 25, 27 |

## Installation

Select the artifact for your test runner. All the artifacts run on JDK 27. JDK 26 and 27 only warn about the `sun.misc.Unsafe` memory-access methods, so the output does not change. A JDK can also deny these methods: JDK 24 and later with `--sun-misc-unsafe-memory-access=deny`, and, as JEP 471 plans, a later JDK by default. Then, without the agent, approvalcrest serialises a type from a locked JDK module from its getters, not from its fields. If you do not attach `com.github.karsaig:approvalcrest-agent` with `-javaagent`, you must regenerate each approved file that contains such a type. The agent keeps the serialised output byte-identical to the output when Unsafe works. See [field access modes](docs/field-access-modes.md#the-agent).

Each block below also adds Hamcrest. Each approvalcrest test artifact declares `org.hamcrest:hamcrest` and your test framework with the `provided` scope, so no artifact brings Hamcrest to the test classpath. If your build already adds Hamcrest, leave out that dependency.

On Gradle, also apply the approvalcrest plugin. Without it, a `-D` property of the command line does not get to the tests. Also, a changed approved file does not run the tests again. See [Gradle](docs/getting-started.md#gradle).

### JUnit 4 & JUnit 5 Vintage

<details><summary>Maven</summary>

```xml
<dependency>
  <groupId>com.github.karsaig</groupId>
  <artifactId>approvalcrest</artifactId>
  <version>1.6.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.hamcrest</groupId>
  <artifactId>hamcrest</artifactId>
  <version>3.0</version>
  <scope>test</scope>
</dependency>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

```kotlin
dependencies {
    testImplementation("com.github.karsaig:approvalcrest:1.6.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

```groovy
dependencies {
    testImplementation 'com.github.karsaig:approvalcrest:1.6.0'
    testImplementation 'org.hamcrest:hamcrest:3.0'
}
```

</details>

### JUnit 5 & 6 Jupiter

<details><summary>Maven</summary>

```xml
<dependency>
  <groupId>com.github.karsaig</groupId>
  <artifactId>approvalcrest-junit-jupiter</artifactId>
  <version>1.6.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.hamcrest</groupId>
  <artifactId>hamcrest</artifactId>
  <version>3.0</version>
  <scope>test</scope>
</dependency>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

```kotlin
dependencies {
    testImplementation("com.github.karsaig:approvalcrest-junit-jupiter:1.6.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

```groovy
dependencies {
    testImplementation 'com.github.karsaig:approvalcrest-junit-jupiter:1.6.0'
    testImplementation 'org.hamcrest:hamcrest:3.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

</details>

### Kotlin + JUnit 5 & 6

<details><summary>Maven</summary>

```xml
<dependency>
  <groupId>com.github.karsaig</groupId>
  <artifactId>approvalcrest-junit-jupiter-kotlin</artifactId>
  <version>1.6.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.hamcrest</groupId>
  <artifactId>hamcrest</artifactId>
  <version>3.0</version>
  <scope>test</scope>
</dependency>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

```kotlin
dependencies {
    testImplementation("com.github.karsaig:approvalcrest-junit-jupiter-kotlin:1.6.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

```groovy
dependencies {
    testImplementation 'com.github.karsaig:approvalcrest-junit-jupiter-kotlin:1.6.0'
    testImplementation 'org.hamcrest:hamcrest:3.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

</details>

### TestNG

<details><summary>Maven</summary>

```xml
<dependency>
  <groupId>com.github.karsaig</groupId>
  <artifactId>approvalcrest-testng</artifactId>
  <version>1.6.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.hamcrest</groupId>
  <artifactId>hamcrest</artifactId>
  <version>3.0</version>
  <scope>test</scope>
</dependency>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

```kotlin
dependencies {
    testImplementation("com.github.karsaig:approvalcrest-testng:1.6.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
}

tasks.test {
    useTestNG()
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

```groovy
dependencies {
    testImplementation 'com.github.karsaig:approvalcrest-testng:1.6.0'
    testImplementation 'org.hamcrest:hamcrest:3.0'
}

tasks.named('test') {
    useTestNG()
}
```

</details>

## Documentation

| Doc | Description |
|---|---|
| [Getting started](docs/getting-started.md) | Artifact selection, Gradle plugin, minimal examples, IDE diff view |
| [sameBeanAs](docs/same-bean-as.md) | Bean-to-bean comparison |
| [sameJsonAsApproved](docs/same-json-as-approved.md) | JSON approval workflow and file names |
| [sameContentAsApproved](docs/same-content-as-approved.md) | Raw-text approval workflow |
| [Ignoring fields](docs/ignoring-fields.md) | `.ignoring()` by path, Hamcrest matcher, or type; `.ignoringElementsWhere()` to drop array elements by value |
| [Custom matching](docs/custom-matching.md) | `.with(path, matcher)` to assert a field instead of a comparison, `.alsoCheck(path, matcher)` to do both |
| [Sorting](docs/sorting.md) | Stable order of collections |
| [Aliasing](docs/aliasing.md) | Replace values that change from run to run with readable placeholders |
| [Dynamic values](docs/dynamic-values.md) | UUIDs, timestamps and other values that change from run to run |
| [File control](docs/file-control.md) | withUniqueId, withFileName, withPath, in-place update, machine-readable diff |
| [Shared approvals](docs/shared-approvals.md) | Pointer files, one shared copy of identical approved files, the dedup tool |
| [System properties](docs/system-properties.md) | Every property and its short alias |
| [Field access modes](docs/field-access-modes.md) | Safe, force and fallback modes, access to JDK modules, and the agent that keeps field-based output when Unsafe is denied |
| [Supported types](docs/supported-types.md) | java.time.\*, Optional, Path, Throwable, circular references |
| [Best practices](docs/best-practices.md) | State management, CI workflow, rules for approved files |
| [JUnit 4 & Vintage](docs/junit4-vintage.md) | JUnit 4 / JUnit 5 Vintage specifics |
| [JUnit 5 & 6 Jupiter](docs/junit5-jupiter.md) | JUnit 5 & 6 Jupiter specifics |
| [TestNG](docs/testng.md) | TestNG specifics |
| [Kotlin](docs/kotlin.md) | Kotlin extension functions and KT-5464 workaround |

