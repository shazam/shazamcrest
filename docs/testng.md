<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=aa4f2c40-565a-40c3-a884-4f86efb2581f" />

# TestNG

How to use approvalcrest with TestNG.

## Dependency

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

On Gradle, also apply the approvalcrest plugin (see [getting-started](getting-started.md#gradle)). Without it, `-D` properties do not reach the tests, and Gradle does not run the tests again when only an approved file changes.

## Imports

```java
import static com.github.karsaig.approvalcrest.testng.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.testng.MatcherAssert.assertThrows;
import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameBeanAs;
import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameJsonAsApproved;
import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameContentAsApproved;
```

## Basic Test

A test that is not parameterized needs no extra configuration:

```java
@Test
public void myTest() {
    MyBean actual = buildMyBean();
    assertThat(actual, sameJsonAsApproved());
}
```

## Parameterized Tests with `@DataProvider`

Add `Method` as a parameter. TestNG injects it automatically. Pass it to `sameJsonAsApproved(method)`, so that the matcher can find the correct test method. Use `.withUniqueId(name)` to make a separate approved file for each case:

```java
@DataProvider
public Object[][] data() {
    return new Object[][] {
        { "case1", new BeanOne("dummy1", "val1") },
        { "case2", new BeanOne("dummy2", "val2") }
    };
}

@Test(dataProvider = "data")
public void testParameterized(String name, BeanOne value, Method method) {
    assertThat(value, sameJsonAsApproved(method).withUniqueId(name));
}
```

The same pattern applies to `sameContentAsApproved`:

```java
@Test(dataProvider = "templates")
public void testContentParameterized(String name, String content, Method method) {
    assertThat(content, sameContentAsApproved(method).withUniqueId(name));
}
```

`Method` injection needs **TestNG 6.14.2+**. On older versions, use `sameJsonAsApproved()` with no arguments. It finds the test method from the stack trace. This works on all versions from TestNG 6.0, but the test method must be public.

## `assertThrows`

Verify that an assertion fails with a specific matcher:

```java
assertThrows(sameBeanAs(expectedException),
    () -> assertThat(actual, sameBeanAs(wrongExpected)));
```

## Version Compatibility

| Feature | Minimum TestNG version |
|---|---|
| Basic API (`sameJsonAsApproved()`) | 6.0 |
| Method injection (`sameJsonAsApproved(Method)`) | 6.14.2 |

The `approvalcrest-testng` artifact has TestNG as a `provided` dependency. Add TestNG to your project. Approvalcrest uses that version at runtime.

## Inherited test methods

Sometimes an abstract base class declares a test method, and several subclasses run it. Approvalcrest cannot identify that subclass automatically. The stack frame and the `java.lang.reflect.Method` that TestNG injects both give the class that *declares* the method, not the subclass that runs it. Thus all the subclasses would use the same approved file and overwrite each other. To prevent this, approvalcrest fails with an error message that lists the fixes.

Give each subclass its own test method. Alternatively, supply a custom `TestMetaInformation` that knows the concrete class. A **concrete** base class has the same problem, but approvalcrest cannot detect it.

## Related

- [same-json-as-approved](same-json-as-approved.md)
- [same-bean-as](same-bean-as.md)
- [same-content-as-approved](same-content-as-approved.md)
- [file-control](file-control.md)
