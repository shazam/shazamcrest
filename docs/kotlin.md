<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=bf2ead15-ef93-47bf-bbfb-32d354a9f801" />

# kotlin

How to use approvalcrest with Kotlin and JUnit 5 & 6.

## Dependency

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

On Gradle, also apply the approvalcrest plugin (see [getting-started](getting-started.md#gradle)). Without it, `-D` properties do not reach the tests, and Gradle does not run the tests again when only an approved file changes.

## Imports

```kotlin
import com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat
import com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThrows
import com.github.karsaig.approvalcrest.kotlin.matcher.Matchers.sameBeanAs
import com.github.karsaig.approvalcrest.kotlin.matcher.Matchers.sameJsonAsApproved
import com.github.karsaig.approvalcrest.kotlin.matcher.Matchers.sameContentAsApproved
```

## Why a Kotlin-Specific Module?

The type inference of Kotlin does not work correctly with F-bounded (recursive) generics: [KT-5464](https://youtrack.jetbrains.com/issue/KT-5464). Without the Kotlin module, a chain of methods such as `.with()` / `.alsoCheck()`, `.ignoring()` and `.withUniqueId()` does not compile. The `approvalcrest-junit-jupiter-kotlin` module has extension functions that let these chains compile.

## Basic Tests

```kotlin
@Test
fun sameBeanComparison() {
    val actual   = buildBean()
    val expected = buildBean()
    assertThat(actual, sameBeanAs(expected))
}

@Test
fun approvalTest() {
    assertThat(buildBean(), sameJsonAsApproved<MyBean>())
}

@Test
fun contentApprovalTest() {
    assertThat("hello world", sameContentAsApproved<String>())
}
```

## Chaining

These chains compile because of the extension functions. With the plain Java API, they do not compile:

```kotlin
// sameBeanAs with .with() and .ignoring()
assertThat(actual, sameBeanAs(expected)
    .ignoring("transientId")
    .with("beanInteger", equalTo(42)))

// sameJsonAsApproved with .withUniqueId() and .ignoring()
assertThat(actual, sameJsonAsApproved<MyBean>()
    .withUniqueId("myCase")
    .ignoring(is("createdAt")))

// sameContentAsApproved with .withUniqueId()
assertThat("content", sameContentAsApproved<String>().withUniqueId("myCase"))
```

## Parameterized Tests

Use the same pattern as for JUnit 5 & 6 with Java. Add `TestInfo` as a parameter:

```kotlin
@ParameterizedTest
@MethodSource("data")
fun testParameterized(name: String, value: BeanOne, testInfo: TestInfo) {
    assertThat(value, sameJsonAsApproved<BeanOne>(testInfo).withUniqueId(name))
}

companion object {
    @JvmStatic
    fun data(): Stream<Arguments> = Stream.of(
        Arguments.of("case1", BeanOne("dummy1", "val1")),
        Arguments.of("case2", BeanOne("dummy2", "val2"))
    )
}
```

## Inherited test methods

The Kotlin matchers use the JUnit Jupiter mechanism, so the same rule applies. Sometimes an abstract base class declares a test function, and several subclasses run it. Such a test fails, because the stack frame names the class that declares the function, not the subclass. The error message lists the fixes. Pass `TestInfo` to get one approved file for each subclass. See [junit5-jupiter](junit5-jupiter.md#inherited-test-methods).

## Where approved files are stored

By default, the Kotlin matchers keep approved files in `src/test/kotlin`, next to the test class. By default, the Java matchers use `src/test/java`. If your Kotlin tests are in a different directory, set the shared `fileMatcherSourceRoot` property to that directory:

```bash
mvn verify -DfileMatcherSourceRoot=src/it/kotlin                              # Maven
./gradlew integrationTest -DfileMatcherSourceRoot=src/integrationTest/kotlin  # Gradle
```

On Gradle, the approvalcrest plugin sets the property for a test suite other than `test` that has a source set of the same name and only a `kotlin` source directory, such as `src/integrationTest/kotlin`. A value that you set wins. See [getting-started](getting-started.md#gradle).

Only the default value is different for the Kotlin matchers and the Java matchers. All the matchers use the same property. Thus, in a project with Java and Kotlin tests, the property moves the approved files of both. See [file-control](file-control.md#configuring-the-test-source-root).

## Related

- [junit5-jupiter](junit5-jupiter.md)
- [custom-matching](custom-matching.md)
- [same-json-as-approved](same-json-as-approved.md)
