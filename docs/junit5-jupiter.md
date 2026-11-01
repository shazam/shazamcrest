<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=0726f4f4-27ae-4491-99ae-1553b09dad73" />

# junit5-jupiter

How to use approvalcrest with JUnit 5 & 6 Jupiter (Java). The `approvalcrest-junit-jupiter` artifact works with JUnit 5 and with JUnit 6 (JUnit Platform). You do not have to change your code.

## Dependency

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

On Gradle, also apply the approvalcrest plugin (see [getting-started](getting-started.md#gradle)). Without it, `-D` properties do not reach the tests, and Gradle does not run the tests again when only an approved file changes.

## Imports

```java
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThrows;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameContentAsApproved;
```

## Basic Test

A test that is not parameterized needs no extra extension:

```java
@Test
public void myTest() {
    MyBean actual = buildMyBean();
    assertThat(actual, sameJsonAsApproved());
}
```

## Parameterized Tests

Add `TestInfo` as a parameter. JUnit 5 & 6 inject it automatically. Pass it to `sameJsonAsApproved(testInfo)`, so that the matcher can find the correct test method. Use `.withUniqueId(name)` to make a separate approved file for each case:

```java
@ParameterizedTest
@MethodSource("data")
public void testParameterized(String name, BeanOne value, TestInfo testInfo) {
    assertThat(value, sameJsonAsApproved(testInfo).withUniqueId(name));
}

static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("case1", new BeanOne("dummy1", "val1")),
        Arguments.of("case2", new BeanOne("dummy2", "val2"))
    );
}
```

The same pattern applies to `sameContentAsApproved`:

```java
@ParameterizedTest
@MethodSource("templates")
public void testContentParameterized(String name, String content, TestInfo testInfo) {
    assertThat(content, sameContentAsApproved(testInfo).withUniqueId(name));
}
```

Without `TestInfo`:
- **Private** test methods fail. The matcher cannot find the method, and throws a `NullPointerException` with a message that tells you to add `TestInfo`.
- **Public** test methods work. The matcher finds the method metadata from the stack trace.

### Automatic per-case file names

When you pass `TestInfo`, the matcher uses the index at the front of the default display name (`[1] ...`) as the unique id. Thus each case gets its own approved file, and you do not have to set an id:

```java
@ParameterizedTest
@ValueSource(strings = {"alpha", "beta"})
void valueSource(String value, TestInfo testInfo) {
    // writes ...-1-approved.json and ...-2-approved.json
    assertThat(beanFor(value), sameJsonAsApproved(testInfo));
}
```

This applies to each argument source (`@ValueSource`, `@CsvSource`, `@EnumSource`, `@NullSource`, `@EmptySource`, `@MethodSource`, `@ArgumentsSource`) and to `sameContentAsApproved`. A custom `@ParameterizedTest(name = ...)` pattern that does not start with the index stops this. Then use `withUniqueId` to keep one file for each case. `withUniqueId` also gives file names that are easier to read:

```java
@ParameterizedTest(name = "case {0}")
@ValueSource(strings = {"alpha", "beta"})
void customName(String value, TestInfo testInfo) {
    assertThat(beanFor(value), sameJsonAsApproved(testInfo).withUniqueId(value));
}
```

## Repeated Tests

`@RepeatedTest` has the meta-annotation `@TestTemplate`, so the matcher finds the test method from the stack trace without `TestInfo`. The repetition number does not have the `[1] ...` shape, so the matcher does not get it automatically. Pass it in yourself. If you do not, all the repetitions compare with one approved file:

```java
@RepeatedTest(3)
void repeated(RepetitionInfo repetitionInfo) {
    assertThat(actual, sameJsonAsApproved().withUniqueId("rep" + repetitionInfo.getCurrentRepetition()));
}
```

## Dynamic Tests

A `DynamicTest` executable is a lambda, not a test method. When it runs, the factory has returned, and the stack has no test method to find. Thus the matchers without arguments fail with the "pass `TestInfo`" error. Add a `TestInfo` parameter to the factory. `TestInfo` names the factory method, so give each case a unique id:

```java
@TestFactory
Stream<DynamicTest> dynamicTests(TestInfo testInfo) {
    return Stream.of("case1", "case2")
            .map(name -> dynamicTest(name,
                    () -> assertThat(beanFor(name), sameJsonAsApproved(testInfo).withUniqueId(name))));
}
```

The same applies to tests nested in a `DynamicContainer`.

## Test Templates

A custom `TestTemplateInvocationContextProvider` can supply the invocations of a `@TestTemplate`. The matcher can find such a test method with `TestInfo` or from the stack trace. The provider sets the display name of each invocation, so you need `withUniqueId` to get one file for each invocation.

## Lifecycle Methods

A matcher that you create in `@BeforeEach`, `@AfterEach` or `@BeforeAll` cannot find the current test, because no test method is on the stack. Instead, keep the injected `TestInfo` in a field. It names the test that is about to run. Thus a shared fixture still gets one approved file for each test:

```java
private TestInfo testInfo;

@BeforeEach
void captureTestInfo(TestInfo testInfo) {
    this.testInfo = testInfo;
}

@AfterEach
void verifyState() {
    assertThat(state, sameJsonAsApproved(testInfo).withUniqueId("afterEach"));
}
```

`@TestInstance(PER_CLASS)` does not change any of this.

## Display Names, Extensions and Composed Annotations

None of these move an approved file:

- `@DisplayName` and `@DisplayNameGeneration`. The method name identifies a test, not its display name. The automatic index above is the only exception.
- `@Nested` — a nested class is its own test class and gets its own directory.
- parameters that an extension injects, `@RegisterExtension` and an `InvocationInterceptor`.
- an annotation that has `@Test` or `@TestTemplate` as a meta-annotation, directly or through another annotation.

## `assertThrows`

Verify that an assertion fails with a specific matcher:

```java
assertThrows(sameBeanAs(expectedException),
    () -> assertThat(actual, sameBeanAs(wrongExpected)));
```

## Inherited test methods

Sometimes an abstract base class declares a test method, and several subclasses run it. The default stack-trace detection cannot identify the subclass that runs such a method. A stack frame names the class that *declares* the method, not the subclass that runs it. Thus all the subclasses would use the same approved file and overwrite each other. To prevent this, approvalcrest fails with an error message that lists the fixes.

Pass `TestInfo` to get one approved file for each subclass. JUnit supplies `TestInfo`, and it knows the concrete class:

```java
public abstract class AbstractContractTest {
    @Test
    void sharedTest(TestInfo testInfo) {
        assertThat(actual, sameJsonAsApproved(testInfo));
    }
}
```

A **concrete** base class with subclasses has the same problem. Approvalcrest cannot tell it from a usual test class, so it cannot detect the problem. Pass `TestInfo` there also.

## Related

- [same-json-as-approved](same-json-as-approved.md)
- [same-bean-as](same-bean-as.md)
- [file-control](file-control.md)
- [kotlin](kotlin.md)
