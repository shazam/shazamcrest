package io.github.karsaig.approvalcrest.gradle;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApprovalcrestPluginFunctionalTest {

    private static final String APPROVALCREST_VERSION = System.getProperty("approvalcrest.version");
    private static final String GRADLE8_JAVA_HOME = System.getProperty("approvalcrest.gradle8JavaHome");
    private static final int GRADLE8_JAVA_VERSION = Integer.getInteger("approvalcrest.gradle8JavaVersion");
    private static final String OLD_JVM_DEPRECATION = "Executing Gradle on JVM versions 16 and lower has been deprecated.";

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

    @TempDir
    Path projectDir;

    private String gradleVersion;

    private void fixture(String gradleVersion, String appBuildExtra) throws IOException {
        this.gradleVersion = gradleVersion;
        write("settings.gradle.kts", "dependencyResolutionManagement {\n"
                + "    repositories {\n"
                + "        mavenLocal { content { includeGroup(\"com.github.karsaig\") } }\n"
                + "        mavenCentral()\n"
                + "    }\n"
                + "}\n"
                + "rootProject.name = \"fixture\"\n"
                + "include(\"app\")\n");
        if (gradleVersion.startsWith("8.")) {
            write("gradle.properties", "org.gradle.java.home=" + GRADLE8_JAVA_HOME.replace('\\', '/') + "\n");
        }
        write("app/build.gradle.kts", "plugins {\n"
                + "    java\n"
                + "    id(\"io.github.karsaig.approvalcrest\")\n"
                + "}\n"
                + "dependencies {\n"
                + "    testImplementation(\"com.github.karsaig:approvalcrest-junit-jupiter:" + APPROVALCREST_VERSION + "\")\n"
                + "    testImplementation(\"org.hamcrest:hamcrest:3.0\")\n"
                + "    testImplementation(platform(\"org.junit:junit-bom:5.14.4\"))\n"
                + "    testImplementation(\"org.junit.jupiter:junit-jupiter-api\")\n"
                + "    testRuntimeOnly(\"org.junit.jupiter:junit-jupiter-engine\")\n"
                + "    testRuntimeOnly(\"org.junit.platform:junit-platform-launcher\")\n"
                + "}\n"
                + "tasks.withType<Test>().configureEach { useJUnitPlatform() }\n"
                + appBuildExtra);
    }

    private BuildResult run(String... arguments) {
        return withoutOtherDeprecations(runner(arguments).build());
    }

    private BuildResult runAndFail(String... arguments) {
        return withoutOtherDeprecations(runner(arguments).buildAndFail());
    }

    /** Gradle 8.14 deprecates running on JDK 16 and older, so those builds allow that one deprecation and no other. */
    private boolean onOldJvm() {
        return gradleVersion.startsWith("8.") && GRADLE8_JAVA_VERSION < 17;
    }

    private BuildResult withoutOtherDeprecations(BuildResult result) {
        if (onOldJvm()) {
            List<String> others = Arrays.stream(result.getOutput().split("\\R"))
                    .filter(line -> line.contains("has been deprecated") && !line.contains(OLD_JVM_DEPRECATION))
                    .collect(Collectors.toList());
            assertEquals(new ArrayList<String>(), others, result.getOutput());
        }
        return result;
    }

    private GradleRunner runner(String... arguments) {
        List<String> all = new ArrayList<>(Arrays.asList(arguments));
        all.add("--configuration-cache");
        all.add(onOldJvm() ? "--warning-mode=all" : "--warning-mode=fail");
        all.add("--stacktrace");
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                .withArguments(all);
    }

    private static void assertOutcome(TaskOutcome expected, BuildResult result, String task) {
        assertEquals(expected, result.task(task).getOutcome(), result.getOutput());
    }

    private void write(String relative, String content) throws IOException {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(UTF_8));
    }

    private String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), UTF_8);
    }

    /** Every file under {@code dir}, relative to it, with its content. */
    private Map<String, String> files(String dir) throws IOException {
        Map<String, String> files = new TreeMap<>();
        Path root = projectDir.resolve(dir);
        if (!Files.exists(root)) {
            return files;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.filter(Files::isRegularFile).collect(Collectors.toList())) {
                files.put(root.relativize(p).toString().replace('\\', '/'), read(p));
            }
        }
        return files;
    }

    /** The single file under {@code dir} whose name ends with {@code suffix}; fails when there is not exactly one. */
    private Path single(String dir, String suffix) throws IOException {
        List<Path> found;
        try (Stream<Path> walk = Files.walk(projectDir.resolve(dir))) {
            found = walk.filter(p -> p.getFileName().toString().endsWith(suffix)).collect(Collectors.toList());
        }
        assertEquals(1, found.size(), "files ending with " + suffix + " under " + dir + ": " + found);
        return found.get(0);
    }

    private Path approve(Path notApproved) throws IOException {
        Path approved = notApproved.resolveSibling(notApproved.getFileName().toString().replace("-not-approved", "-approved"));
        Files.move(notApproved, approved);
        return approved;
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void approvalWorkflowWithCommandLineFlags(String gradle) throws IOException {
        fixture(gradle, "");
        write("app/src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
        Path notApproved = single("app/src/test/java", "-not-approved.json");
        String relative = projectDir.resolve("app/src/test/java").relativize(notApproved).toString().replace('\\', '/');
        assertTrue(relative.matches("fixture/[0-9a-f]{6}/[0-9a-f]{6}-not-approved\\.json"), relative);
        String correct = read(notApproved);
        String stale = correct.replace("\"c\"", "\"z\"");
        Path approved = approve(notApproved);

        assertOutcome(SUCCESS, run(":app:test"), ":app:test");
        BuildResult upToDate = run(":app:test");
        assertOutcome(UP_TO_DATE, upToDate, ":app:test");
        assertTrue(upToDate.getOutput().contains("Reusing configuration cache."), upToDate.getOutput());

        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");

        assertOutcome(SUCCESS, run(":app:test", "-DfileMatcherUpdateInPlace=true"), ":app:test");
        assertEquals(correct, read(approved));

        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
        assertEquals(stale, read(approved));

        assertOutcome(SUCCESS, run(":app:test", "-DfMUInPlace=true"), ":app:test");
        assertEquals(correct, read(approved));

        // The same stale bytes and the same flag as the run before: a write run must still execute.
        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(SUCCESS, run(":app:test", "-DfMUInPlace=true"), ":app:test");
        assertEquals(correct, read(approved));

        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(SUCCESS, run("clean", ":app:test", "--build-cache", "-DfMUInPlace=true"), ":app:test");
        assertEquals(correct, read(approved));
        Files.write(approved, stale.getBytes(UTF_8));
        assertOutcome(SUCCESS, run("clean", ":app:test", "--build-cache", "-DfMUInPlace=true"), ":app:test");
        assertEquals(correct, read(approved));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void commandLineWinsOverTheBuildScript(String gradle) throws IOException {
        fixture(gradle, "tasks.test { systemProperty(\"fileMatcherPassOnCreate\", \"false\") }\n");
        write("app/src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
        assertOutcome(SUCCESS, run(":app:test", "-DfileMatcherPassOnCreate=true"), ":app:test");

        assertEquals(1, files("app/src/test/java/fixture").keySet().stream().filter(f -> f.endsWith("-not-approved.json")).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void canonicalInASharedDirectoryIsAnInput(String gradle) throws IOException {
        fixture(gradle, "tasks.test { systemProperty(\"fileMatcherSharedDir\", \"approvals-shared\") }\n");
        write("app/src/test/java/fixture/FixtureTest.java", LIST_TEST);
        runAndFail(":app:test");
        Path notApproved = single("app/src/test/java", "-not-approved.json");
        String content = read(notApproved);
        String header = content.substring(0, content.indexOf('\n') + 1);
        String json = content.substring(content.indexOf('\n') + 1);
        Path approved = approve(notApproved);
        Files.write(approved, (header + "/*pointer:approvals-shared/ab/abcdef012345-10-approved.json*/").getBytes(UTF_8));
        write("app/approvals-shared/ab/abcdef012345-10-approved.json", "/*shared*/\n" + json);

        assertOutcome(SUCCESS, run(":app:test"), ":app:test");
        assertOutcome(UP_TO_DATE, run(":app:test"), ":app:test");

        write("app/approvals-shared/ab/abcdef012345-10-approved.json", "/*shared*/\n" + json.replace("\"c\"", "\"z\""));
        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void approvedFilesUnderACustomRoot(String gradle) throws IOException {
        fixture(gradle, "tasks.test { systemProperty(\"fileMatcherSourceRoot\", \"approvals\") }\n");
        assertCustomRootIsAnInput();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void rootSetInALaterAfterEvaluateIsAnInput(String gradle) throws IOException {
        fixture(gradle, "afterEvaluate { tasks.test { systemProperty(\"fileMatcherSourceRoot\", \"approvals\") } }\n");
        assertCustomRootIsAnInput();
    }

    private static final String PROPERTY_TEST = "package fixture;\n\n"
            + "import static org.junit.jupiter.api.Assertions.assertEquals;\n"
            + "import static org.junit.jupiter.api.Assertions.assertNull;\n\n"
            + "import org.junit.jupiter.api.Test;\n\n"
            + "class PropertyTest {\n"
            + "    @Test\n"
            + "    void onlyTheTestPropertiesArrive() {\n"
            + "        assertEquals(\"on\", System.getProperty(\"custom.flag\"));\n"
            + "        assertNull(System.getProperty(\"org.gradle.approvalcrest.marker\"));\n"
            + "    }\n"
            + "}\n";

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void gradleOwnOptionsStayOutOfTheTestJvm(String gradle) throws IOException {
        fixture(gradle, "");
        write("app/src/test/java/fixture/PropertyTest.java", PROPERTY_TEST);

        assertOutcome(SUCCESS, run(":app:test", "-Dcustom.flag=on", "-Dorg.gradle.approvalcrest.marker=1"), ":app:test");
    }

    private void assertCustomRootIsAnInput() throws IOException {
        write("app/src/test/java/fixture/FixtureTest.java", LIST_TEST);

        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
        Path approved = approve(single("app/approvals/fixture", "-not-approved.json"));
        assertOutcome(SUCCESS, run(":app:test"), ":app:test");
        assertOutcome(UP_TO_DATE, run(":app:test"), ":app:test");

        Files.write(approved, read(approved).replace("\"c\"", "\"z\"").getBytes(UTF_8));
        assertOutcome(FAILED, runAndFail(":app:test"), ":app:test");
    }

    private static final String INTEGRATION_TEST_SUITE = "val integrationTest = sourceSets.create(\"integrationTest\")\n"
            + "configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())\n"
            + "configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())\n"
            + "tasks.register<Test>(\"integrationTest\") {\n"
            + "    testClassesDirs = integrationTest.output.classesDirs\n"
            + "    classpath = integrationTest.runtimeClasspath\n"
            + "}\n";

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void suiteRootFollowsTheSourceSet(String gradle) throws IOException {
        fixture(gradle, INTEGRATION_TEST_SUITE);
        write("app/src/integrationTest/java/fixture/FixtureTest.java", LIST_TEST);

        runAndFail(":app:integrationTest");

        single("app/src/integrationTest/java/fixture", "-not-approved.json");
        assertFalse(Files.exists(projectDir.resolve("app/src/test/java")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void explicitSuiteRootWins(String gradle) throws IOException {
        fixture(gradle, INTEGRATION_TEST_SUITE
                + "tasks.named<Test>(\"integrationTest\") { systemProperty(\"fileMatcherSourceRoot\", \"src/custom\") }\n");
        write("app/src/integrationTest/java/fixture/FixtureTest.java", LIST_TEST);

        runAndFail(":app:integrationTest");

        single("app/src/custom/fixture", "-not-approved.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void commandLineSuiteRootWins(String gradle) throws IOException {
        fixture(gradle, INTEGRATION_TEST_SUITE);
        write("app/src/integrationTest/java/fixture/FixtureTest.java", LIST_TEST);

        runAndFail(":app:integrationTest", "-DfmSourceRoot=src/cli");

        single("app/src/cli/fixture", "-not-approved.json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void mixedSuiteGetsNoRoot(String gradle) throws IOException {
        fixture(gradle, INTEGRATION_TEST_SUITE);
        write("app/src/integrationTest/java/fixture/FixtureTest.java", LIST_TEST);
        Files.createDirectories(projectDir.resolve("app/src/integrationTest/kotlin"));

        runAndFail(":app:integrationTest");

        single("app/src/test/java/fixture", "-not-approved.json");
    }

    // Java 8 has no modules, so approvalcrest never uses the agent there; only the JVM argument shows it.
    private static final String AGENT_TEST = "package fixture;\n\n"
            + "import static org.junit.jupiter.api.Assertions.assertEquals;\n\n"
            + "import java.lang.management.ManagementFactory;\n\n"
            + "import org.junit.jupiter.api.Test;\n\n"
            + "class AgentTest {\n"
            + "    @Test\n"
            + "    void instrumentation() {\n"
            + "        boolean expected = Boolean.getBoolean(\"expectAgent\");\n"
            + "        assertEquals(expected, ManagementFactory.getRuntimeMXBean().getInputArguments().stream()\n"
            + "                .anyMatch(a -> a.startsWith(\"-javaagent:\") && a.contains(\"approvalcrest-agent\")));\n"
            + "        if (!System.getProperty(\"java.version\").startsWith(\"1.\")) {\n"
            + "            assertEquals(expected, com.github.karsaig.approvalcrest.ReflectUtil.isInstrumentationAvailable());\n"
            + "        }\n"
            + "    }\n"
            + "}\n";

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void attachesTheAgentWhenAsked(String gradle) throws IOException {
        fixture(gradle, "approvalcrest { attachAgent = true }\n"
                + "tasks.test { systemProperty(\"expectAgent\", \"true\") }\n");
        write("app/src/test/java/fixture/AgentTest.java", AGENT_TEST);

        assertOutcome(SUCCESS, run(":app:test"), ":app:test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void noAgentByDefault(String gradle) throws IOException {
        fixture(gradle, "tasks.test { systemProperty(\"expectAgent\", \"false\") }\n");
        write("app/src/test/java/fixture/AgentTest.java", AGENT_TEST);

        assertOutcome(SUCCESS, run(":app:test"), ":app:test");
    }

    private static final String DUPLICATE = "\n{\n  \"total\": 42,\n  \"currency\": \"GBP\"\n}";
    private static final String UNIQUE = "/*orders.OrderTest.unique*/\n{\n  \"total\": 7\n}";
    private static final String POINTER = "/*pointer:src/test/java/shared-approvals/75/752a56a58607-38-approved.json*/";

    private Map<String, String> standaloneApprovals() {
        Map<String, String> files = new TreeMap<>();
        files.put("billing/a7b8c9/0d1e2f-approved.json", "/*billing.InvoiceTest.a*/" + DUPLICATE);
        files.put("orders/c1a2b3/9f8e7d-approved.json", UNIQUE);
        files.put("orders/c1a2b3/d4e5f6-approved.json", "/*orders.OrderTest.c*/" + DUPLICATE);
        files.put("shipping/b3c4d5/e6f7a8-approved.json", "/*shipping.ParcelTest.b*/" + DUPLICATE);
        files.put("notes/a1b2c3/111111-approved.content", "/*notes.NoteTest.a*/\nsame text");
        files.put("notes/a1b2c3/222222-approved.content", "/*notes.NoteTest.b*/\nsame text");
        return files;
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.14.6", "9.8.2"})
    void dedupAndReinstateTasks(String gradle) throws IOException {
        fixture(gradle, "");
        Map<String, String> original = standaloneApprovals();
        for (Map.Entry<String, String> file : original.entrySet()) {
            write("app/src/test/java/" + file.getKey(), file.getValue());
        }

        BuildResult dryRun = run(":app:approvalcrestDedup", "--preview");
        assertTrue(dryRun.getOutput().contains("Dry run, no files changed."), dryRun.getOutput());
        assertEquals(original, files("app/src/test/java"));

        run(":app:approvalcrestDedup", "--types", "json");
        Map<String, String> deduplicated = new TreeMap<>(original);
        deduplicated.put("billing/a7b8c9/0d1e2f-approved.json", "/*billing.InvoiceTest.a*/\n" + POINTER);
        deduplicated.put("orders/c1a2b3/d4e5f6-approved.json", "/*orders.OrderTest.c*/\n" + POINTER);
        deduplicated.put("shipping/b3c4d5/e6f7a8-approved.json", "/*shipping.ParcelTest.b*/\n" + POINTER);
        deduplicated.put("shared-approvals/75/752a56a58607-38-approved.json", "/*shared*/" + DUPLICATE);
        assertEquals(deduplicated, files("app/src/test/java"));

        run(":app:approvalcrestReinstate");
        assertEquals(original, files("app/src/test/java"));
    }
}
