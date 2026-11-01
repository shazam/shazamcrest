<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=932587be-8781-4843-af47-fa58cbfd05d1" />

# Field Access Modes

Approvalcrest uses Gson to serialise Java objects to JSON for the comparison. On JDK 9+, the module system limits reflective access to the internal fields of platform classes (`java.time.*`, `java.lang.*`, `java.math.*`, and others). In the past, you had to add `--add-opens` JVM flags to your test configuration for this.

From version 1.2.0, approvalcrest handles this automatically. By default, you do not need `--add-opens` flags. The `approvalcrestReflection` system property (alias: `aCReflection`) controls the behaviour. It selects one of three modes.

## Mode Overview

| Mode | Property value | How it accesses fields | Output format | `--add-opens` needed | JDK support |
|---|---|---|---|---|---|
| **Safe** | `safe` (default) | Opens modules from code (through the agent if it is attached, otherwise through Unsafe), then uses standard reflection | Field-based (identical to the output before modules) | No | 8 and later; when Unsafe is denied, only the agent keeps it field-based |
| **Force** | `force` | `setAccessible(true)` directly | Field-based (identical to the output before modules) | Yes (on JDK 9+) | 8 and later (with --add-opens) |
| **Fallback** | `fallback` | Public getter methods (`getX()` / `isX()`) | Getter-based (different property names) | No | 8 and later |

## Safe Mode (Default)

Safe mode is the default and recommended mode. It needs **no configuration**. Add the dependency. Then run your tests.

### How it works

When approvalcrest finds a type from a locked module (for example `java.time.LocalDate`), it uses a strategy with three levels:

1. **Modules opened from code** — at start-up, approvalcrest opens locked packages with the route that is available. When you attach the [agent](#the-agent), approvalcrest uses `Instrumentation.redefineModule`. Otherwise, approvalcrest uses `sun.misc.Unsafe` to get a trusted `MethodHandles.Lookup`, and then `Module.implAddOpens()`. Each route is the runtime equivalent of `--add-opens`. Approvalcrest selects the route automatically.
2. **Standard reflection** — after approvalcrest opens the module package, `field.setAccessible(true)` works as usual. The built-in serialisation of Gson gives the same field-based JSON as before.
3. **Degradation** — if no route can open a module, approvalcrest uses getter-based serialisation (the same as fallback mode). With the agent attached, this does not occur, because `Instrumentation` needs no Unsafe. Without the agent, this covers both stages of the plan of the JDK. First, the memory-access methods throw `UnsupportedOperationException` when the JDK denies them, although `sun.misc.Unsafe` still exists. JDK 24 and later deny them with `--sun-misc-unsafe-memory-access=deny`, and JEP 471 plans to make this the default in a later release. Second, the JDK plans to remove the class completely in a later release.

### When to use

- **Always**, unless you have a specific reason to use another mode.
- If you migrate from `--add-opens`, remove the flags. Safe mode replaces them.

### Example output

For a `RuntimeException("test error")`:
```json
{
  "class": "java.lang.RuntimeException",
  "detailMessage": "test error",
  "cause": null,
  "suppressedExceptions": []
}
```

This output is field-based: the JSON keys match the actual field names in `java.lang.Throwable`.

## Force Mode

Force mode uses `field.setAccessible(true)` directly, and does not open modules. This is the traditional behaviour. It **always works when `--add-opens` is available on the JDK**.

### When to use

- You already configured `--add-opens`, and you prefer explicit control.
- You use a JDK where the Unsafe-based approach of safe mode does not work. This is unlikely, but possible with custom security managers.
- Corporate policy requires explicit `--add-opens` declarations.

### Configuration

```shell
mvn test -DapprovalcrestReflection=force        # Maven
./gradlew test -DapprovalcrestReflection=force  # Gradle
```

### Required `--add-opens` Flags

The exact flags depend on the types that your tests serialise. These are the flags that approvalcrest needs most frequently:

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.time=ALL-UNNAMED
--add-opens java.base/java.time.zone=ALL-UNNAMED
--add-opens java.base/java.math=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
--add-opens java.base/java.security=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.util.calendar=ALL-UNNAMED
--add-opens java.sql/java.sql=ALL-UNNAMED
```

If a flag is absent, approvalcrest detects the `InaccessibleObjectException`, and shows you the exact flag that you must add.

### Build Tool Configuration

<details><summary>Maven</summary>

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            -DapprovalcrestReflection=force
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.time=ALL-UNNAMED
            --add-opens java.base/java.time.zone=ALL-UNNAMED
            --add-opens java.base/java.math=ALL-UNNAMED
            --add-opens java.base/java.io=ALL-UNNAMED
            --add-opens java.base/java.security=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/sun.util.calendar=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

<!-- snippet: force-mode.kts -->
```kotlin
tasks.test {
    systemProperty("approvalcrestReflection", "force")
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.time.zone=ALL-UNNAMED",
        "--add-opens", "java.base/java.math=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.security=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/sun.util.calendar=ALL-UNNAMED"
    )
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

<!-- snippet: force-mode.gradle -->
```groovy
tasks.named('test') {
    systemProperty 'approvalcrestReflection', 'force'
    jvmArgs '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
            '--add-opens', 'java.base/java.time=ALL-UNNAMED',
            '--add-opens', 'java.base/java.time.zone=ALL-UNNAMED',
            '--add-opens', 'java.base/java.math=ALL-UNNAMED',
            '--add-opens', 'java.base/java.io=ALL-UNNAMED',
            '--add-opens', 'java.base/java.security=ALL-UNNAMED',
            '--add-opens', 'java.base/java.util=ALL-UNNAMED',
            '--add-opens', 'java.base/sun.util.calendar=ALL-UNNAMED'
}
```

</details>

### IntelliJ IDEA Configuration

In **Run/Debug Configuration** → **VM options**, add:

```
-DapprovalcrestReflection=force --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED
```

Or set it for all test runs: **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → VM Options**.

In a Gradle project, IntelliJ runs the tests through Gradle by default. Then the configuration of the `test` task applies, and you need no VM options.

### Example output

The same as safe mode, field-based JSON:
```json
{
  "class": "java.lang.RuntimeException",
  "detailMessage": "test error",
  "cause": null,
  "suppressedExceptions": []
}
```

## Fallback Mode

Fallback mode does not use Unsafe at all. It serialises locked-module types with their **public getter methods** (`getX()` / `isX()`). This gives different JSON property names from the field-based modes.

### When to use

- **Preparation for a JDK that denies Unsafe** — when Unsafe no longer works, safe mode automatically gives this getter-based output. This occurs when the JDK denies the Unsafe memory-access methods and the [agent](#the-agent) is not attached. Use fallback mode now to prepare your approved files for that. If you attach the agent instead, the output stays field-based and the files do not change.
- **Strict compliance** — your organisation does not allow any use of `sun.misc.Unsafe`, or workarounds that go around the module system.
- **Verification** — make sure that your test suite works without internal JDK APIs.

### Configuration

```shell
mvn test -DapprovalcrestReflection=fallback        # Maven
./gradlew test -DapprovalcrestReflection=fallback  # Gradle
```

### How it works

1. Types on the classpath (unnamed module): approvalcrest serialises them with **standard field access** (the same as safe/force mode). Examples are your own POJOs, test data and third-party library classes. The classpath is always accessible.
2. Types in locked JDK modules (`java.time.*`, `java.lang.*`, `java.math.*`): approvalcrest serialises them with **public getters**. The JSON keys are the getter names without the `get`/`is` prefix, with the first letter in lowercase.
3. Types with registered custom adapters (for example `LocalDate`, `Instant`, `Path`): these still use their custom adapters in all modes, and give the same string representations.

### Example output

For a `RuntimeException("test error")`:
```json
{
  "cause": null,
  "class": "java.lang.RuntimeException",
  "localizedMessage": "test error",
  "message": "test error",
  "suppressed": []
}
```

Compare with field-based output:
```json
{
  "class": "java.lang.RuntimeException",
  "detailMessage": "test error",
  "cause": null,
  "suppressedExceptions": []
}
```

Key differences:
- `detailMessage` → `message` (from `getMessage()`) and `localizedMessage` (from `getLocalizedMessage()`)
- `suppressedExceptions` → `suppressed` (from `getSuppressed()`)
- The property order is the order in which approvalcrest finds the getters

### Impact on approved files

When you change a project from safe/force to fallback mode, **regenerate the approved files for types in locked modules**. Your own POJO types (on the classpath) do not change, because they serialise identically in all modes.

To regenerate:
```shell
mvn test -DapprovalcrestReflection=fallback -DfileMatcherUpdateInPlace=true        # Maven
./gradlew test -DapprovalcrestReflection=fallback -DfileMatcherUpdateInPlace=true  # Gradle
```

## Setting the Mode

### System property

The `approvalcrestReflection` system property (alias: `aCReflection`) controls the mode:

```shell
# Safe mode, the default
mvn test -DapprovalcrestReflection=safe        # Maven
./gradlew test -DapprovalcrestReflection=safe  # Gradle

# Force mode, needs --add-opens
mvn test -DapprovalcrestReflection=force        # Maven
./gradlew test -DapprovalcrestReflection=force  # Gradle

# Fallback mode, getter-based output
mvn test -DapprovalcrestReflection=fallback        # Maven
./gradlew test -DapprovalcrestReflection=fallback  # Gradle

# Short alias
mvn test -DaCReflection=fallback        # Maven
./gradlew test -DaCReflection=fallback  # Gradle
```

### Programmatic (before any assertion)

```java
System.setProperty("approvalcrestReflection", "fallback");
```

Approvalcrest reads the mode one time, when it loads the `ReflectUtil` class. Set the mode before any code creates an approvalcrest matcher.

### Build tool

<details><summary>Maven</summary>

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-DapprovalcrestReflection=fallback</argLine>
    </configuration>
</plugin>
```

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

<!-- snippet: fallback-mode.kts -->
```kotlin
tasks.test {
    systemProperty("approvalcrestReflection", "fallback")
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

<!-- snippet: fallback-mode.gradle -->
```groovy
tasks.named('test') {
    systemProperty 'approvalcrestReflection', 'fallback'
}
```

</details>

## The agent

Add one JVM argument. Then safe mode continues to give field-based output when the JDK denies the Unsafe memory-access methods. You do not need `--add-opens` or other flags:

```
-javaagent:/path/to/approvalcrest-agent-1.6.0.jar
```

The agent does one thing: it gives approvalcrest a `java.lang.instrument.Instrumentation`. This is the supported way to open a module package. When you attach the agent, there are two other results. The JVM adds the agent jar to the end of the system classpath. Also, all other code on that classpath can read the `Instrumentation` from `ApprovalcrestAgent.getInstrumentation()`.

The jar declares only `Premain-Class`, and not `Can-Redefine-Classes` or `Can-Retransform-Classes`. Thus the `Instrumentation` that it gives cannot redefine or retransform classes. Because the jar declares only `Premain-Class`, you also cannot attach the agent to a JVM that already runs. The agent replaces the `sun.misc.Unsafe` route, which a JDK can deny. [JEP 451](https://openjdk.org/jeps/451) restricts only *dynamically attached* agents, so a `-javaagent` at start-up continues to work.

### Build tool setup

<details><summary>Maven</summary>

Add the artifact. Then put the agent on the command line of Surefire:

```xml
<dependency>
    <groupId>com.github.karsaig</groupId>
    <artifactId>approvalcrest-agent</artifactId>
    <version>1.6.0</version>
    <scope>test</scope>
</dependency>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>@{argLine} -javaagent:${settings.localRepository}/com/github/karsaig/approvalcrest-agent/1.6.0/approvalcrest-agent-1.6.0.jar</argLine>
    </configuration>
</plugin>
```

If nothing else in your build defines `argLine`, also declare an empty `argLine` in `<properties>`. When nothing defines the property, Surefire does not replace the `@{argLine}` token. The forked JVM then stops with an error about an unrecognised JVM argument:

```xml
<properties>
    <argLine></argLine>
</properties>
```

**Keep the `@{argLine}`.** The `prepare-agent` goal of JaCoCo publishes its own agent argument through the `argLine` *property*. A plain `<argLine>` replaces that property, and your coverage disappears without a warning. The same applies to the inline-mock agent of Mockito and to APM agents.

</details>

<details><summary>Gradle (Kotlin DSL)</summary>

With the approvalcrest plugin (see [getting-started](getting-started.md#gradle)), set `attachAgent`. The plugin then attaches the `approvalcrest-agent` of the plugin version to each `Test` task. You do not declare the dependency:

<!-- snippet: agent-plugin.kts -->
```kotlin
approvalcrest {
    attachAgent = true
}
```

</details>

<details><summary>Gradle (Groovy DSL)</summary>

With the approvalcrest plugin (see [getting-started](getting-started.md#gradle)), set `attachAgent`. The plugin then attaches the `approvalcrest-agent` of the plugin version to each `Test` task. You do not declare the dependency:

<!-- snippet: agent-plugin.gradle -->
```groovy
approvalcrest {
    attachAgent = true
}
```

</details>

<details><summary>Gradle without the plugin</summary>

Declare the agent in a separate configuration. Then give its file to the test JVM with `-javaagent`. Gradle resolves the file only when the test task runs.

Kotlin DSL:

<!-- snippet: agent-without-plugin.kts -->
```kotlin
val approvalcrestAgent = configurations.create("approvalcrestAgent")
dependencies {
    approvalcrestAgent("com.github.karsaig:approvalcrest-agent:1.6.0")
}
tasks.test {
    // A local copy: the configuration cache cannot store a lambda that reads a property of the build script.
    val agent: FileCollection = approvalcrestAgent
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:${agent.singleFile}") })
}
```

Groovy DSL:

<!-- snippet: agent-without-plugin.gradle -->
```groovy
configurations {
    approvalcrestAgent
}
dependencies {
    approvalcrestAgent 'com.github.karsaig:approvalcrest-agent:1.6.0'
}
def approvalcrestAgent = configurations.approvalcrestAgent
tasks.named('test') {
    jvmArgumentProviders.add({ ["-javaagent:${approvalcrestAgent.singleFile}".toString()] } as CommandLineArgumentProvider)
}
```

</details>

You can use several `-javaagent` options. The JVM runs each `premain` in the given order. The `premain` of approvalcrest only stores the `Instrumentation` and transforms nothing, so it and an agent that instruments classes do not affect each other.

### Checking it took effect

It is easy to add the dependency without the `-javaagent:` argument. When the JDK denies the Unsafe memory-access methods, approvalcrest then changes to getter-based output. It gives no warning and no error. If this is important to you, assert it in a test:

```java
// JDK 9+ only. On Java 8 there are no modules to open, so this is false even with the agent
// attached and working, and output is field-based regardless.
assertTrue(com.github.karsaig.approvalcrest.ReflectUtil.isInstrumentationAvailable());
```

`isModuleOpeningAvailable()` answers a wider question: whether *either* route works. Read it as "was a module opened", not as "what format will the output be". It is false on JDK 8, where the output is field-based anyway, because nothing is locked.

### Adding it before Unsafe is denied

While Unsafe works, the output does not change, whether or not the agent is attached. Thus, if you add the `-javaagent` argument now, it costs nothing, and a JDK that denies Unsafe needs no more action later.

It does not stop the `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` line on JDK 24 to 27. Safe mode still probes Unsafe at start-up to find out whether it works. The JVM warns about that probe.

### What it does not change

Force mode still requires `--add-opens`. It calls `setAccessible` directly, and never asks approvalcrest to open anything. Thus the agent has no effect on it. With the agent attached, safe mode already gives the same field-based output as force mode. Thus there is no reason to prefer force mode when Unsafe is denied.

## JDK Compatibility Matrix

| JDK | Safe mode | Force mode | Fallback mode |
|---|---|---|---|
| 8 | ✅ Field-based (no modules) | ✅ Field-based | ✅ Field-based (no locked modules) |
| 9–15 | ✅ Field-based (opens modules) | ✅ Field-based (with --add-opens) | ✅ Getter-based for locked types |
| 16 | ✅ Field-based (opens modules) | ✅ Field-based (with --add-opens) | ✅ Getter-based for locked types |
| 17 and later, Unsafe not denied (the default up to 27) | ✅ Field-based (opens modules) | ✅ Field-based (with --add-opens) | ✅ Getter-based for locked types |
| 24 and later, Unsafe denied, with the agent | ✅ Field-based (opens modules) | ✅ Field-based (with --add-opens) | ✅ Getter-based for locked types |
| 24 and later, Unsafe denied, no agent | ⚠️ Getter-based for locked types | ✅ Field-based (with --add-opens) | ✅ Getter-based for locked types |

JDK 16 made strong encapsulation the default (`--illegal-access=deny`). On JDK 9–15, the default was `--illegal-access=permit`, which allowed reflective access with a warning. Safe mode works identically on JDK 8 to 27, while Unsafe is not denied.

The change comes when the JDK denies the Unsafe memory-access methods. [JEP 471](https://openjdk.org/jeps/471) deprecated these methods for removal. [JEP 498](https://openjdk.org/jeps/498) made JDK 24 warn on the first use, and JDK 25 to 27 also only warn by default. This is the `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` line that is possibly already in your build logs. With `--sun-misc-unsafe-memory-access=deny`, the methods throw. JEP 471 plans to make this the default in a later release.

When the JDK denies the methods, safe mode can no longer open modules without help. **Attach the agent** (see [The agent](#the-agent) above). Then the field-based output does not change. Without the agent, locked-module types change to getter-based output.

## Migration Guide

### Removing `--add-opens` (recommended)

If you used `--add-opens` flags before:

1. Remove all `--add-opens` flags from your Maven/Gradle/IDE configuration.
2. If you set `-DapprovalcrestReflection=force`, remove it.
3. Run your tests. They pass without changes.

The default safe mode opens the same modules automatically. Your approved files stay valid, because the output format is identical.

### Switching from safe to fallback

1. Set `-DapprovalcrestReflection=fallback`.
2. Regenerate affected approved files: `mvn test -DapprovalcrestReflection=fallback -DfileMatcherUpdateInPlace=true` or `./gradlew test -DapprovalcrestReflection=fallback -DfileMatcherUpdateInPlace=true`.
3. Review the updated approved files.
4. Commit them.

Only the approved files for types in locked JDK modules change. Your own POJO types give identical JSON in all modes.

## FAQ

### Will switching to safe mode break my existing approved files?

No. Safe mode gives the same field-based JSON as force mode (with `--add-opens`). Your approved files match without changes.

### What happens when Unsafe stops working?

Attach the [agent](#the-agent). Then nothing changes. Approvalcrest still opens modules, the output stays field-based, and your approved files continue to match.

Without the agent, safe mode changes to getter-based serialisation for locked-module types, exactly as fallback mode does. Your tests do not crash. But you must regenerate the approved files for locked-module types, because the output format changes from field-based to getter-based.

When the JDK denies the memory-access methods, it disables `sun.misc.Unsafe`, but does not remove it. The class still loads and its methods still resolve, but a call to one of them throws `UnsupportedOperationException`. Thus approvalcrest makes a probe call at start-up, and treats a failed probe as "no Unsafe". It does not assume that Unsafe works because the class is present. The same degradation occurs when a later release removes the class.

There are two alternatives to the agent, and both keep the field-based output. One is `--sun-misc-unsafe-memory-access=allow` (available from JDK 24). It only delays the change until the JDK removes Unsafe completely. The other is force mode with `--add-opens`.

### Do circular references still work in all modes?

Yes. The detection of circular references works identically in all modes. The format of the circular reference marker (`"0x1"`, `"0x2"`, and so on) is the same in all field access modes.

### Can I mix modes in the same JVM?

No. Approvalcrest reads the mode one time, when it loads the `ReflectUtil` class. The mode applies to all approvalcrest assertions in that JVM. To test several modes, use separate Surefire executions or separate Gradle `Test` tasks.

### My POJO has no public getters — will fallback mode work?

For your own types on the classpath (unnamed module): yes. Approvalcrest always reads them with field reflection, in all modes. Fallback mode uses getters only for types in **locked JDK modules**.

### Does this affect `sameBeanAs` or only `sameJsonAsApproved`?

All matchers use the same serialisation pipeline. The mode affects `sameBeanAs`, `sameJsonAsApproved` and `sameContentAsApproved` in the same way.
