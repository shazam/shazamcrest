<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=283a15c6-bb2b-48c5-86bc-78dae2cf24c9" />

# Getting Started

Add approvalcrest to your project. Then write your first assertion.

## Choosing the Right Artifact

| Test framework | Artifact |
|---|---|
| JUnit 4 / JUnit 5 Vintage | `com.github.karsaig:approvalcrest:1.6.0` |
| JUnit 5 & 6 Jupiter (Java) | `com.github.karsaig:approvalcrest-junit-jupiter:1.6.0` |
| Kotlin + JUnit 5 & 6 | `com.github.karsaig:approvalcrest-junit-jupiter-kotlin:1.6.0` |
| TestNG | `com.github.karsaig:approvalcrest-testng:1.6.0` |

For all frameworks, you can also add `com.github.karsaig:approvalcrest-agent:1.6.0`, and attach it with `-javaagent`. This Java agent keeps the output field-based when the JDK denies the `sun.misc.Unsafe` calls that approvalcrest uses to open locked modules. JDK 24 and later deny them with `--sun-misc-unsafe-memory-access=deny`, and JEP 471 plans to make this the default in a later JDK. See [field-access-modes](field-access-modes.md#the-agent).

Each block below also adds Hamcrest. Each approvalcrest test artifact declares `org.hamcrest:hamcrest` and your test framework with the `provided` scope, so no artifact brings Hamcrest to the test classpath. The approvalcrest build uses Hamcrest 3.0. If your build already adds Hamcrest, leave out that dependency.

On Gradle, also apply the approvalcrest plugin, as [Gradle](#gradle) shows.

### JUnit 4 / Vintage

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

<!-- snippet: dependency-junit4.kts -->
```kotlin
dependencies {
    testImplementation("com.github.karsaig:approvalcrest:1.6.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

<!-- snippet: dependency-junit4.gradle -->
```groovy
dependencies {
    testImplementation 'com.github.karsaig:approvalcrest:1.6.0'
    testImplementation 'org.hamcrest:hamcrest:3.0'
}
```

</details>

On Gradle, JUnit 5 Vintage also needs `useJUnitPlatform()`, the `junit-platform-launcher` runtime dependency, and the `junit-vintage-engine` runtime dependency. Without the Vintage engine, the JUnit Platform finds no JUnit 4 tests.

### JUnit 5 & 6 Jupiter

On Gradle, JUnit Jupiter needs `useJUnitPlatform()`, so that Gradle runs the tests on the JUnit Platform. It also needs a `junit-platform-launcher` runtime dependency, because Gradle 9 does not add the launcher. The Gradle blocks include both. The launcher line has no version: it gets the version of your JUnit Jupiter dependency.

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

<!-- snippet: dependency-jupiter.kts -->
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

<!-- snippet: dependency-jupiter.gradle -->
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

<!-- snippet: dependency-kotlin.kts -->
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

<!-- snippet: dependency-kotlin.gradle -->
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

<!-- snippet: dependency-testng.kts -->
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

<!-- snippet: dependency-testng.gradle -->
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

## Gradle

Apply the approvalcrest plugin in each project that runs approvalcrest tests. The plugin is on the Gradle Plugin Portal, so Gradle finds it with no further setup:

<details><summary>Gradle (Kotlin DSL)</summary>

<!-- snippet: plugin.kts -->
```kotlin
plugins {
    java
    id("io.github.karsaig.approvalcrest") version "1.6.1"
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

<!-- snippet: plugin.gradle -->
```groovy
plugins {
    id 'java'
    id 'io.github.karsaig.approvalcrest' version '1.6.1'
}
```

</details>

The plugin needs Gradle 8.14 or later. It does these things for each `Test` task:

- It passes each `-D` property of the command line to the test JVM. Thus `./gradlew test -DfileMatcherUpdateInPlace=true` works as `mvn test -DfileMatcherUpdateInPlace=true` does. A value on the command line wins over a value that the build script sets. The plugin passes all `-D` properties except the `org.gradle.*` options of Gradle, also the ones that approvalcrest does not read, such as proxy settings. They become inputs of the task. Surefire also passes all `-D` properties to the test JVM.
- It makes the approved files inputs of the task. Without this, Gradle does not run the tests again when only an approved file changes.
- A run with `fileMatcherUpdateInPlace` or `fileMatcherPassOnCreate` is never up to date and never comes from the build cache. Such a run writes approved files, so a stored result does not apply.
- For a test suite other than `test`, such as `integrationTest`, it sets `fileMatcherSourceRoot` to `src/integrationTest/java`, or to `src/integrationTest/kotlin` when only that exists. It does this only when the task has a source set of the same name. When both directories exist, set the property yourself, because one property cannot name two roots. A value that you set wins.

The plugin also adds the tasks `approvalcrestDedup` and `approvalcrestReinstate` (see [shared-approvals](shared-approvals.md)). With `approvalcrest { attachAgent = true }`, it attaches the agent (see [field-access-modes](field-access-modes.md#the-agent)).

<details><summary>Gradle without the plugin</summary>

A build that cannot apply the plugin can configure the `test` task itself. This configuration passes the `-D` properties to the test JVM, and makes the approved files inputs of the task.

Kotlin DSL:

<!-- snippet: test-task-without-plugin.kts -->
```kotlin
// The -D properties of the command line, without the options of Gradle itself.
val commandLine = providers.systemPropertiesPrefixedBy("").get()
    .filterKeys { it in gradle.startParameter.systemPropertiesArgs && !it.startsWith("org.gradle.") }

tasks.test {
    systemProperties(commandLine)
    // Approved files are not sources. Without this input, Gradle skips the tests when only an approved file changes.
    inputs.files(fileTree("src/test/java") { include("**/*-approved.*") })
        .withPropertyName("approvedFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
```

Groovy DSL:

<!-- snippet: test-task-without-plugin.gradle -->
```groovy
// The -D properties of the command line, without the options of Gradle itself.
def commandLine = providers.systemPropertiesPrefixedBy('').get()
    .findAll { name, value -> gradle.startParameter.systemPropertiesArgs.containsKey(name) && !name.startsWith('org.gradle.') }

tasks.named('test') {
    systemProperties(commandLine)
    // Approved files are not sources. Without this input, Gradle skips the tests when only an approved file changes.
    inputs.files(fileTree('src/test/java') { include '**/*-approved.*' })
        .withPropertyName('approvedFiles')
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
```

For Kotlin tests, change `src/test/java` to `src/test/kotlin`. If you set `fileMatcherSourceRoot`, use its value.

Add `--rerun` to a run with `fileMatcherUpdateInPlace` or `fileMatcherPassOnCreate`. Otherwise Gradle can find the task up to date, and the run writes no approved file:

```bash
./gradlew test --rerun -DfileMatcherUpdateInPlace=true
```

Without the plugin, a test suite other than `test` needs the same configuration and its own `fileMatcherSourceRoot`. To deduplicate approved files, run the standalone JAR (see [shared-approvals](shared-approvals.md#adding-the-dedup-tool)).

</details>

## Your First `sameBeanAs` Assertion

`sameBeanAs` serialises both objects to JSON and compares the results. If a field differs, the error message shows the difference.

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;

@Test
void comparesTwoBeansFieldByField() {
    Address expected = new Address("Baker Street", "London");
    Address actual   = new Address("Baker Street", "London");

    assertThat(actual, sameBeanAs(expected));
}
```

You do not have to list the fields. The matcher compares the full object graph. See [same-bean-as](same-bean-as.md) for more information.

## Your First `sameJsonAsApproved` Assertion

`sameJsonAsApproved` serialises the actual value to JSON and compares it with an approved file. You approve this file one time and commit it.

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;

@Test
void myBeanMatchesApprovedSnapshot() {
    MyBean actual = buildMyBean();
    assertThat(actual, sameJsonAsApproved());
}
```

**Approval workflow:**

1. **First run** — there is no approved file yet. The matcher writes `<classHash>/<methodHash>-not-approved.json` next to your test source.
2. **Review** the generated file to make sure that the output is correct.
3. **Rename** it from `*-not-approved.json` to `*-approved.json`.
4. **Later runs** compare the output with the approved file. A run passes if the two match. If they do not match, the run fails and shows a diff.

See [same-json-as-approved](same-json-as-approved.md) for parameterized tests, file name options and more information.

## Your First `sameContentAsApproved` Assertion

`sameContentAsApproved` works like `sameJsonAsApproved`, but for any text. Examples are rendered templates, API response bodies, log output, generated reports and HTML fragments. The file extension is `.content`, and the matcher does not parse JSON.

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameContentAsApproved;

@Test
void renderedTemplateMatchesSnapshot() {
    String actual = renderWelcomeEmail("Alice");
    assertThat(actual, sameContentAsApproved());
}
```

The approval workflow is the same as for `sameJsonAsApproved`. The first run writes a `*-not-approved.content` file. You review the file and rename it to `*-approved.content`. Later runs compare the output with it.

See [same-content-as-approved](same-content-as-approved.md) for parameterized tests and more details.

## IDE Diff View

All matchers throw an exception that is compatible with `ComparisonFailure`. Thus IDEs such as IntelliJ and Eclipse show a side-by-side diff.

![IDE diff view](DiffScreenshot.png)

**Important:** use `MatcherAssert.assertThat` from the approvalcrest package, **not** the `assertThat` of Hamcrest. The approvalcrest method throws the `ComparisonFailure` that opens the IDE diff view. The `assertThat` of Hamcrest does not.
