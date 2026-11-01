<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=31251b12-91b4-47c8-aa8c-d7a3cf292eae" />

# junit4-vintage

How to use approvalcrest with JUnit 4 and JUnit 5 Vintage.

## Dependency

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

On Gradle, also apply the approvalcrest plugin (see [getting-started](getting-started.md#gradle)). Without it, `-D` properties do not reach the tests, and Gradle does not run the tests again when only an approved file changes.

## Imports

```java
import static com.github.karsaig.approvalcrest.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.MatcherAssert.assertThrows;
import static com.github.karsaig.approvalcrest.matcher.Matchers.sameBeanAs;
import static com.github.karsaig.approvalcrest.matcher.Matchers.sameJsonAsApproved;
import static com.github.karsaig.approvalcrest.matcher.Matchers.sameContentAsApproved;
```

## Basic Test

```java
public class MyTest {

    @Test
    public void myTest() {
        MyBean actual = buildMyBean();
        assertThat(actual, sameJsonAsApproved());
    }

    @Test
    public void myContentTest() {
        String actual = renderTemplate();
        assertThat(actual, sameContentAsApproved());
    }
}
```

A test that is not parameterized needs no extra runner or rule.

## Parameterized Tests

JUnit 4 cannot give test metadata to the matcher automatically. Add a `Junit4DesciptionWatcher` rule. You can also pass the description to the matcher:

```java
import com.github.karsaig.approvalcrest.Junit4DesciptionWatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MyParameterizedTest {

    @Rule
    public Junit4DesciptionWatcher testWatcher = new Junit4DesciptionWatcher();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "case1", new BeanOne("dummy1", "val1") },
            { "case2", new BeanOne("dummy2", "val2") }
        });
    }

    private final String name;
    private final BeanOne value;

    public MyParameterizedTest(String name, BeanOne value) {
        this.name  = name;
        this.value = value;
    }

    @Test
    public void testParameterized() {
        // withUniqueId disambiguates the approved file per case
        assertThat(value, sameJsonAsApproved().withUniqueId(name));
    }

    @Test
    public void testParameterizedWithDescription() {
        // Pass the description explicitly for full metadata resolution
        assertThat(value, sameJsonAsApproved(testWatcher.getDescription()).withUniqueId(name));
    }

    @Test
    public void testContentParameterized() {
        // sameContentAsApproved works the same way — pass .withUniqueId per case
        String content = renderForCase(name);
        assertThat(content, sameContentAsApproved().withUniqueId(name));
    }
}
```

## `assertThrows`

Verify that an assertion fails with an expected exception:

```java
assertThrows(sameBeanAs(expectedException),
    () -> assertThat(actual, sameBeanAs(wrongExpected)));
```

## Inherited test methods

Sometimes an abstract base class declares a test method, and several subclasses run it. The default stack-trace detection cannot identify the subclass that runs such a method. A stack frame names the class that *declares* the method, not the subclass that runs it. Thus all the subclasses would use the same approved file and overwrite each other. To prevent this, approvalcrest fails with an error message that lists the fixes.

Use `Junit4DesciptionWatcher` as a `@Rule`. Then create the matcher with a `Description`, which knows the concrete test class:

```java
public abstract class AbstractContractTest {
    @Rule
    public Junit4DesciptionWatcher watcher = new Junit4DesciptionWatcher();

    @Test
    public void sharedTest() {
        assertThat(actual, sameJsonAsApproved(watcher.getDescription()));
    }
}
```

A **concrete** base class with subclasses has the same problem, but approvalcrest cannot detect it.
