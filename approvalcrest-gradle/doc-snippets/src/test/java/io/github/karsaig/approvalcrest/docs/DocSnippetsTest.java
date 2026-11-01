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

    private void fixture(String dsl, String... buildParts) throws IOException {
        boolean kts = "kts".equals(dsl);
        String settings = kts
                ? "pluginManagement { repositories { maven { url = uri(\"" + PLUGIN_REPO + "\") }; gradlePluginPortal() } }\n"
                + "dependencyResolutionManagement { repositories { mavenLocal { content { includeGroup(\"com.github.karsaig\") } }; mavenCentral() } }\n"
                + "rootProject.name = \"fixture\"\n"
                : "pluginManagement { repositories { maven { url = uri('" + PLUGIN_REPO + "') }; gradlePluginPortal() } }\n"
                + "dependencyResolutionManagement { repositories { mavenLocal { content { includeGroup('com.github.karsaig') } }; mavenCentral() } }\n"
                + "rootProject.name = 'fixture'\n";
        write(kts ? "settings.gradle.kts" : "settings.gradle", settings);
        // The snippets expect the reader's own test framework; the fixture adds JUnit Jupiter as a project would have it.
        String junit = kts
                ? "dependencies { testImplementation(\"org.junit.jupiter:junit-jupiter:5.14.4\") }\n"
                : "dependencies { testImplementation 'org.junit.jupiter:junit-jupiter:5.14.4' }\n";
        write(kts ? "build.gradle.kts" : "build.gradle", String.join("\n", buildParts) + "\n" + junit);
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
        assertEquals(expected, result.task(":test").getOutcome(), result.getOutput());
    }

    private void write(String relative, String content) throws IOException {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(UTF_8));
    }

    private Path single(String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(projectDir.resolve("src/test/java"))) {
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
        fixture(dsl, snippet("plugin." + dsl), snippet("dependency-jupiter." + dsl));
        write("src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail("test"));
        Path approved = approve(single("-not-approved.json"));
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
        fixture(dsl, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet("test-task-without-plugin." + dsl));
        write("src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail("test"));
        Path approved = approve(single("-not-approved.json"));
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
            fixture(dsl, snippet("plugin." + dsl), snippet("dependency-jupiter." + dsl), snippet("agent-plugin." + dsl));
        } else {
            fixture(dsl, javaPlugin(dsl), snippet("dependency-jupiter." + dsl), snippet("agent-without-plugin." + dsl));
        }
        write("src/test/java/fixture/AgentTest.java", AGENT_TEST);

        assertOutcome(SUCCESS, run("test"));
    }

    @ParameterizedTest
    @CsvSource({"kts, force-mode", "gradle, force-mode", "kts, fallback-mode", "gradle, fallback-mode",
            "kts, resources-root", "gradle, resources-root", "kts, dependency-junit4", "gradle, dependency-junit4",
            "kts, dependency-testng", "gradle, dependency-testng"})
    void configures(String dsl, String id) throws IOException {
        fixture(dsl, javaPlugin(dsl), snippet(id + "." + dsl));

        run("help");
    }

    @ParameterizedTest
    @ValueSource(strings = {"kts", "gradle"})
    void kotlinDependencyConfigures(String dsl) throws IOException {
        String kotlinPlugin = "kts".equals(dsl)
                ? "plugins {\n    kotlin(\"jvm\") version \"2.3.21\"\n}\n"
                : "plugins {\n    id 'org.jetbrains.kotlin.jvm' version '2.3.21'\n}\n";
        fixture(dsl, kotlinPlugin, snippet("dependency-kotlin." + dsl));

        run("help");
    }
}
