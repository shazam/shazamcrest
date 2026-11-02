package io.github.karsaig.approvalcrest.docs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Builds Gradle projects from the snippets that the docs mark with {@code <!-- snippet: id.kts -->} or
 * {@code <!-- snippet: id.gradle -->}, so that what a reader pastes is what the tests run.
 */
class DocSnippetsTest {

    private static final String VERSION = System.getProperty("approvalcrest.version");
    private static final Path REPO_ROOT = Paths.get(System.getProperty("approvalcrest.repoRoot"));
    private static final String PLUGIN_REPO = System.getProperty("approvalcrest.pluginRepo").replace('\\', '/');
    private static final Map<String, String> SNIPPETS = extractSnippets();
    private static final String JUPITER = "org.junit.jupiter:junit-jupiter:5.14.4";
    private static final String JUNIT4 = "junit:junit:4.13.2";
    private static final String TESTNG = "org.testng:testng:7.10.2";

    private static final String LIST_TEST = "package fixture;\n\n"
            + "import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;\n"
            + "import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameJsonAsApproved;\n\n"
            + "import java.util.Arrays;\n\n"
            + "import org.junit.jupiter.api.Test;\n\n"
            + "class FixtureTest {\n"
            + "    @Test\n"
            + "    void approvedList() {\n"
            + "        assertThat(Arrays.asList(\"c\", \"a\", \"b\"), sameJsonAsApproved());\n"
            + "    }\n"
            + "}\n";

    private static final String AGENT_TEST = "package fixture;\n\n"
            + "import static org.junit.jupiter.api.Assertions.assertTrue;\n\n"
            + "import org.junit.jupiter.api.Test;\n\n"
            + "class AgentTest {\n"
            + "    @Test\n"
            + "    void instrumentation() {\n"
            + "        assertTrue(com.github.karsaig.approvalcrest.ReflectUtil.isInstrumentationAvailable());\n"
            + "    }\n"
            + "}\n";

    private static final String JUNIT4_TEST = "package fixture;\n\n"
            + "import static com.github.karsaig.approvalcrest.MatcherAssert.assertThat;\n"
            + "import static com.github.karsaig.approvalcrest.matcher.Matchers.sameBeanAs;\n"
            + "import static com.github.karsaig.approvalcrest.matcher.Matchers.sameJsonAsApproved;\n\n"
            + "import java.util.Arrays;\n\n"
            + "import org.junit.Test;\n\n"
            + "public class FixtureTest {\n"
            + "    @Test\n"
            + "    public void approvedList() {\n"
            + "        assertThat(Arrays.asList(\"c\", \"a\", \"b\"), sameJsonAsApproved());\n"
            + "    }\n\n"
            + "    // Needs org.hamcrest.Matchers, which the hamcrest-core 1.3 of JUnit 4 does not have.\n"
            + "    @Test\n"
            + "    public void sameString() {\n"
            + "        assertThat(\"a\", sameBeanAs(\"a\"));\n"
            + "    }\n"
            + "}\n";

    private static final String TESTNG_TEST = "package fixture;\n\n"
            + "import static com.github.karsaig.approvalcrest.testng.MatcherAssert.assertThat;\n"
            + "import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameJsonAsApproved;\n\n"
            + "import java.util.Arrays;\n\n"
            + "import org.testng.annotations.Test;\n\n"
            + "public class FixtureTest {\n"
            + "    @Test\n"
            + "    public void approvedList() {\n"
            + "        assertThat(Arrays.asList(\"c\", \"a\", \"b\"), sameJsonAsApproved());\n"
            + "    }\n"
            + "}\n";

    private static final String KOTLIN_TEST = "package fixture\n\n"
            + "import com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat\n"
            + "import com.github.karsaig.approvalcrest.kotlin.matcher.Matchers.sameJsonAsApproved\n"
            + "import org.junit.jupiter.api.Test\n\n"
            + "class FixtureTest {\n"
            + "    @Test\n"
            + "    fun approvedList() {\n"
            + "        assertThat(listOf(\"c\", \"a\", \"b\"), sameJsonAsApproved<List<String>>())\n"
            + "    }\n"
            + "}\n";

    /** In force mode, a {@code ZoneId} needs the {@code java.time} opens of the snippet; most JDK types have their own adapter. */
    private static String modeTest(String mode) {
        return "package fixture;\n\n"
                + "import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;\n"
                + "import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;\n"
                + "import static org.junit.jupiter.api.Assertions.assertEquals;\n\n"
                + "import java.time.ZoneId;\n\n"
                + "import org.junit.jupiter.api.Test;\n\n"
                + "class ModeTest {\n"
                + "    @Test\n"
                + "    void mode() {\n"
                + "        assertEquals(\"" + mode + "\", com.github.karsaig.approvalcrest.ReflectUtil.getMode());\n"
                + "        assertThat(ZoneId.of(\"Europe/London\"), sameBeanAs(ZoneId.of(\"Europe/London\")));\n"
                + "    }\n"
                + "}\n";
    }

    @TempDir
    Path projectDir;

    private static Map<String, String> extractSnippets() {
        Pattern marker = Pattern.compile("<!-- snippet: ([a-z0-9-]+\\.(?:kts|gradle)) -->");
        Map<String, String> snippets = new TreeMap<>();
        List<Path> files = new ArrayList<>();
        files.add(REPO_ROOT.resolve("README.md"));
        try (DirectoryStream<Path> docs = Files.newDirectoryStream(REPO_ROOT.resolve("docs"), "*.md")) {
            docs.forEach(files::add);
            for (Path file : files) {
                List<String> lines = Arrays.asList(new String(Files.readAllBytes(file), UTF_8).replace("\r\n", "\n").split("\n", -1));
                for (int i = 0; i < lines.size(); i++) {
                    Matcher m = marker.matcher(lines.get(i));
                    if (!m.find()) {
                        continue;
                    }
                    if (!lines.get(i + 1).startsWith("```")) {
                        throw new IllegalStateException(file + ":" + (i + 1) + ": a snippet marker must be directly before a fence");
                    }
                    StringBuilder body = new StringBuilder();
                    int j = i + 2;
                    while (!lines.get(j).startsWith("```")) {
                        body.append(lines.get(j)).append('\n');
                        j++;
                    }
                    if (snippets.put(m.group(1), body.toString()) != null) {
                        throw new IllegalStateException("snippet " + m.group(1) + " appears more than once");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return snippets;
    }

    /** The snippet with the versions of the build under test, as a reader would get them on release day. */
    private static String snippet(String id) {
        String text = SNIPPETS.get(id);
        assertNotNull(text, "no snippet " + id + " in README.md or docs/; found " + SNIPPETS.keySet());
        return text
                .replaceAll("(com\\.github\\.karsaig:approvalcrest[a-z0-9-]*):[0-9][0-9A-Za-z._-]*", "$1:" + VERSION)
                .replaceAll("(id[ (][\"']io\\.github\\.karsaig\\.approvalcrest[\"']\\)? version [\"'])[^\"']+", "$1" + VERSION);
    }

    /** A reader's build: the plugins, the test framework that the reader already has, then the pasted snippets. */
    private void fixture(String dsl, String framework, String plugins, String... snippets) throws IOException {
        boolean kts = "kts".equals(dsl);
        String settings = kts
                ? "pluginManagement { repositories { maven { url = uri(\"" + PLUGIN_REPO + "\") }; gradlePluginPortal() } }\n"
                + "dependencyResolutionManagement { repositories { mavenLocal { content { includeGroup(\"com.github.karsaig\") } }; mavenCentral() } }\n"
                + "rootProject.name = \"fixture\"\n"
                : "pluginManagement { repositories { maven { url = uri('" + PLUGIN_REPO + "') }; gradlePluginPortal() } }\n"
                + "dependencyResolutionManagement { repositories { mavenLocal { content { includeGroup('com.github.karsaig') } }; mavenCentral() } }\n"
                + "rootProject.name = 'fixture'\n";
        write(kts ? "settings.gradle.kts" : "settings.gradle", settings);
        // The snippets expect the reader's own test framework, so the fixture adds only that jar.
        String frameworkDependency = kts
                ? "dependencies { testImplementation(\"" + framework + "\") }\n"
                : "dependencies { testImplementation '" + framework + "' }\n";
        write(kts ? "build.gradle.kts" : "build.gradle", plugins + "\n" + frameworkDependency + "\n" + String.join("\n", snippets));
    }

    private static String javaPlugin(String dsl) {
        return "kts".equals(dsl) ? "plugins {\n    java\n}\n" : "plugins {\n    id 'java'\n}\n";
    }

    private BuildResult run(String... arguments) {
        return runner(arguments).build();
    }

    private BuildResult runAndFail(String... arguments) {
        return runner(arguments).buildAndFail();
    }

    private GradleRunner runner(String... arguments) {
        List<String> all = new ArrayList<>(Arrays.asList(arguments));
        all.add("--configuration-cache");
        all.add("--warning-mode=fail");
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withArguments(all);
    }

    private static void assertOutcome(TaskOutcome expected, BuildResult result) {
        BuildTask test = result.task(":test");
        assertNotNull(test, result.getOutput());
        assertEquals(expected, test.getOutcome(), result.getOutput());
    }

    private void write(String relative, String content) throws IOException {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(UTF_8));
    }

    private Path single(String root, String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(projectDir.resolve(root))) {
            List<Path> found = walk.filter(p -> p.getFileName().toString().endsWith(suffix)).collect(Collectors.toList());
            assertEquals(1, found.size(), String.valueOf(found));
            return found.get(0);
        }
    }

    private Path approve(Path notApproved) throws IOException {
        Path approved = notApproved.resolveSibling(notApproved.getFileName().toString().replace("-not-approved", "-approved"));
        Files.move(notApproved, approved);
        return approved;
    }

    /** The first run fails and writes a not-approved file under the root. After its approval, the test passes. */
    private Path firstApproval(String root) throws IOException {
        assertOutcome(FAILED, runAndFail("test"));
        Path approved = approve(single(root, "-not-approved.json"));
        assertOutcome(SUCCESS, run("test"));
        return approved;
    }

    @Test
    void everySnippetHasAKotlinAndAGroovyForm() {
        for (String id : SNIPPETS.keySet()) {
            String base = id.substring(0, id.lastIndexOf('.'));
            assertTrue(SNIPPETS.containsKey(base + ".kts") && SNIPPETS.containsKey(base + ".gradle"), id);
        }
        assertFalse(SNIPPETS.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void withThePlugin(String dsl) throws IOException {
        fixture(dsl, JUPITER, snippet("plugin." + dsl), snippet("dependency-jupiter." + dsl));
        write("src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail("test"));
        Path approved = approve(single("src/test/java", "-not-approved.json"));
        String correct = new String(Files.readAllBytes(approved), UTF_8);
        assertOutcome(SUCCESS, run("test"));
        assertOutcome(UP_TO_DATE, run("test"));

        Files.write(approved, correct.replace("\"c\"", "\"z\"").getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail("test"));
        assertOutcome(SUCCESS, run("test", "-DfileMatcherUpdateInPlace=true"));
        assertEquals(correct, new String(Files.readAllBytes(approved), UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void withoutThePlugin(String dsl) throws IOException {
        fixture(dsl, JUPITER, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet("test-task-without-plugin." + dsl));
        write("src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail("test"));
        Path approved = approve(single("src/test/java", "-not-approved.json"));
        String correct = new String(Files.readAllBytes(approved), UTF_8);
        String stale = correct.replace("\"c\"", "\"z\"");
        assertOutcome(SUCCESS, run("test"));
        assertOutcome(UP_TO_DATE, run("test"));

        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail("test"));
        assertOutcome(SUCCESS, run("test", "--rerun", "-DfileMatcherUpdateInPlace=true"));
        assertEquals(correct, new String(Files.readAllBytes(approved), UTF_8));

        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail("test"));
    }

    @ParameterizedTest
    @CsvSource({"kts, plugin", "gradle, plugin", "kts, without", "gradle, without"})
    void agent(String dsl, String variant) throws IOException {
        if ("plugin".equals(variant)) {
            fixture(dsl, JUPITER, snippet("plugin." + dsl), snippet("dependency-jupiter." + dsl), snippet("agent-plugin." + dsl));
        } else {
            fixture(dsl, JUPITER, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet("agent-without-plugin." + dsl));
        }
        write("src/test/java/fixture/AgentTest.java", AGENT_TEST);

        assertOutcome(SUCCESS, run("test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void junit4Dependency(String dsl) throws IOException {
        fixture(dsl, JUNIT4, snippet("plugin." + dsl), snippet("dependency-junit4." + dsl));
        write("src/test/java/fixture/FixtureTest.java", JUNIT4_TEST);

        firstApproval("src/test/java");
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void testngDependency(String dsl) throws IOException {
        fixture(dsl, TESTNG, snippet("plugin." + dsl), snippet("dependency-testng." + dsl));
        write("src/test/java/fixture/FixtureTest.java", TESTNG_TEST);

        firstApproval("src/test/java");
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void kotlinDependency(String dsl) throws IOException {
        // The reader's Kotlin build sets its own targets: no stable Kotlin release targets JDK 27 yet.
        String kotlinPlugin = "kts".equals(dsl)
                ? "plugins {\n    kotlin(\"jvm\") version \"2.3.21\"\n}\n"
                + "java { targetCompatibility = JavaVersion.VERSION_11 }\n"
                + "kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }\n"
                : "plugins {\n    id 'org.jetbrains.kotlin.jvm' version '2.3.21'\n}\n"
                + "java { targetCompatibility = JavaVersion.VERSION_11 }\n"
                + "kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }\n";
        fixture(dsl, JUPITER, kotlinPlugin, snippet("dependency-kotlin." + dsl));
        write("src/test/kotlin/fixture/FixtureTest.kt", KOTLIN_TEST);

        firstApproval("src/test/kotlin");
    }

    @ParameterizedTest
    @CsvSource({"kts, force", "gradle, force", "kts, fallback", "gradle, fallback"})
    void reflectionMode(String dsl, String mode) throws IOException {
        fixture(dsl, JUPITER, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet(mode + "-mode." + dsl));
        write("src/test/java/fixture/ModeTest.java", modeTest(mode));

        assertOutcome(SUCCESS, run("test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void resourcesRoot(String dsl) throws IOException {
        fixture(dsl, JUPITER, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet("resources-root." + dsl));
        write("src/test/java/fixture/FixtureTest.java", LIST_TEST);

        Path approved = firstApproval("src/test/resources/approvalcrest");
        assertOutcome(UP_TO_DATE, run("test"));

        String correct = new String(Files.readAllBytes(approved), UTF_8);
        Files.write(approved, correct.replace("\"c\"", "\"z\"").getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail("test"));
    }
}
