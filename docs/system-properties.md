# System Properties Reference

You can set each boolean system property of approvalcrest with `-Dproperty=value` on the command line, or with `System.setProperty(...)` in the test setup.

Maven Surefire passes each `-D` property of the command line to the test JVM. On Gradle, the approvalcrest plugin does this. See [getting-started](getting-started.md#gradle).

These values mean true (case-insensitive): `true`, `t`, `1`, `yes`, `y`.  
All other values mean `false`.

Each property also accepts a **short alias**. The alias is the initial of each camelCase word except the last, followed by the last word in full. The first word gives a lowercase initial, and each later word gives an uppercase initial.

You can set the full property name and the alias to the **same value**.  
If you set them to **different values**, approvalcrest throws `IllegalStateException` when the tests start.

## Property table

| Property | Alias | Default | Description | Docs |
|---|---|---|---|---|
| `approvalcrestReflection` | `aCReflection` | `safe` | Field access mode. `safe` opens modules automatically (when Unsafe is denied, only with the approvalcrest agent attached; without the agent, locked types are getter-based). `force` needs --add-opens. `fallback` is getter-based. | [field-access-modes](field-access-modes.md) |
| `fileMatcherUpdateInPlace` | `fMUInPlace` | `false` | When the output differs, rewrite the approved file in place from the actual output. Do not fail the test. Approvalcrest also accepts the legacy name `jsonMatcherUpdateInPlace`. | [same-json-as-approved](same-json-as-approved.md) |
| `fileMatcherSkipCustomMatchersOnUpdate` | `fMSCMOUpdate` | `false` | Do not evaluate custom matchers (`.with`, `.withMatcher`, `.alsoCheck`, `.alsoCheckMatching`) while `fileMatcherUpdateInPlace` rewrites approved files. Then a custom matcher that fails does not stop the regeneration of the rest of the run. This property has no effect unless an in-place update runs. | [file-control](file-control.md) |
| `fileMatcherPassOnCreate` | `fMPOnCreate` | `false` | Pass the test, and do not fail it, when there is no approved file yet. | [same-json-as-approved](same-json-as-approved.md) |
| `fileMatcherSourceRoot` | `fmSourceRoot` | `src/test/java`, or `src/test/kotlin` for the Kotlin matchers | The base directory of the test sources (relative to the project root) that contains the approved files. When tests are outside the standard directory, set it to a value such as `src/it/java`. Only the default is different for each framework. When you set the property, the value applies to all matchers. | [file-control](file-control.md) |
| `fileMatcherSharedDir` | `fmSharedDir` | `src/test/java/shared-approvals` | The path (relative to the project root) of the directory for shared canonical files. Shared approvals use this directory. | [shared-approvals](shared-approvals.md) |
| `fileMatcherSharedEnabled` | `fmSharedEnabled` | `false` | Turns on write-side integration. New approved files and in-place updates look for a canonical that matches, and write a pointer file instead of duplicate content. Accepts `true`/`all` for both file types, `json` or `content` for one type, and `false`/`none` for neither. | [shared-approvals](shared-approvals.md) |
| `fileMatcherSharedDirBucketDepth` | `fmSharedDirBucketDepth` | `2` | The number of characters from the start of the hash that make the name of the bucket subdirectory in the shared directory (1–6). | [shared-approvals](shared-approvals.md) |
| `sortInputFile` | `sIFile` | `false` | Sort the input before approvalcrest writes it to the approved file. | [sorting](sorting.md) |
| `fileMatcherStrictFileMatching` | `fMStrictMatching` | `true` | The ignore rules remove fields only from the actual side. Approved files must not contain the value of an ignored field. Set to `false` to get the old two-sided ignore behaviour. | [ignoring-fields](ignoring-fields.md) |
| `fileMatcherMachineReadable` | `fMMReadable`, `fmAI` | `false` | Replace the failure messages for people with structured output that AI agents and CI pipelines can process. | [file-control](file-control.md) |
| `beanMatcherSkipClassComparison` | `bMSCComparison` | `false` | Skip the runtime-type check in `sameBeanAs`. Then you can compare objects of different classes that have a compatible structure. | [same-bean-as](same-bean-as.md) |
| `approvalcrestSerializeNulls` | `aSerNulls` | `true` | Include fields with a null value in the Gson serialisation. Set to `false` to get the behaviour from before 1.0.2. That behaviour left out null fields without a warning. | [ignoring-fields](ignoring-fields.md) |
| `approvalcrestLegacySetCollapse` | `aLSCollapse` | `false` | Get the behaviour from before 1.4.3, where `Set` elements that serialise to the same JSON became one element. Use it only for a short time, while you re-approve the affected files. While it is on, a set that loses or gets a duplicate element cannot fail a test. | [supported-types](supported-types.md) |

## Example usage

```shell
# Update approved files in place, with the short alias
mvn test -DfMUInPlace=true        # Maven
./gradlew test -DfMUInPlace=true  # Gradle

# Disable null serialisation for all tests, with the short alias
mvn test -DaSerNulls=false        # Maven
./gradlew test -DaSerNulls=false  # Gradle

# Enable machine-readable output for AI agents
mvn test -DfmAI=true        # Maven
./gradlew test -DfmAI=true  # Gradle
```
