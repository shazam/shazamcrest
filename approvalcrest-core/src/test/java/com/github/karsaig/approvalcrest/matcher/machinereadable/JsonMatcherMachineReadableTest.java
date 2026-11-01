package com.github.karsaig.approvalcrest.matcher.machinereadable;

import com.github.karsaig.approvalcrest.matcher.AbstractFileMatcherTest;
import com.github.karsaig.approvalcrest.matcher.JsonMatcher;
import com.github.karsaig.approvalcrest.testdata.BeanWithPrimitives;
import com.github.karsaig.approvalcrest.FieldsIgnorer;
import com.github.karsaig.approvalcrest.testdata.ChildBean;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonParser;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.karsaig.approvalcrest.testdata.ChildBean.Builder.child;
import static org.junit.jupiter.api.Assertions.*;

public class JsonMatcherMachineReadableTest extends AbstractFileMatcherTest {

    private static final String EXISTING_APPROVED_CONTENT = "{\n  \"beanBoolean\": false\n}";

    // Case B — mismatch via fluent API (.withMachineReadableOutput())
    @Test
    public void shouldOutputMachineReadableMessageOnMismatchWhenFluentApiEnabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            String approvedPath = jsonDir.resolve("11b2ef-approved.json").toAbsolutePath().toString();
            assertAll(
                    () -> assertEquals("MISMATCH", json.get("failureType").getAsString(), "Should contain failureType"),
                    () -> assertEquals("dummyTestClassName#dummyTestMethodName", json.get("test").getAsString(), "Should contain test info"),
                    () -> assertEquals(approvedPath, json.get("approvedFile").getAsString(), "Should contain approved file path"),
                    () -> assertTrue(json.has("action"), "Should contain action"),
                    () -> assertTrue(json.has("actual"), "Should contain actual field"),
                    () -> assertTrue(json.has("ignoredFields"), "Should contain ignoredFields array"),
                    () -> assertTrue(json.has("aliasedFields"), "Should contain aliasedFields array"),
                    () -> assertTrue(json.has("sortedFields"), "Should contain sortedFields array")
            );
        });
    }

    /**
     * An alsoCheck field is not ignored, so it produces no ignoredFields record. The counterpart of
     * shouldTrackCustomMatcherPathInIgnoredFields on the bean route.
     */
    @Test
    public void shouldNotTrackAnAlsoCheckPathInIgnoredFields() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY
                    .jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            underTest.alsoCheck("beanInteger", org.hamcrest.Matchers.notNullValue())
                    .withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            JsonObject json = JsonParser.parseString(error.getMessage()).getAsJsonObject();
            com.google.gson.JsonArray ignoredFields = json.getAsJsonArray("ignoredFields");
            // The loop below is vacuous on an empty array, so assert the size first -- that is the actual claim.
            assertEquals(0, ignoredFields.size(),
                    "alsoCheck removes nothing, so nothing is reported as ignored: " + ignoredFields);
            for (int i = 0; i < ignoredFields.size(); i++) {
                assertNotEquals("beanInteger", ignoredFields.get(i).getAsJsonObject().get("path").getAsString(),
                        "alsoCheck removes nothing, so it must not appear in ignoredFields");
            }
        });
    }

    /**
     * Guards the removal-set rewiring on the JsonMatcher route: {@code with(...)} no longer writes into
     * pathsToIgnore, so the reason map is now built from a different collection. This pins that a
     * {@code with(...)} path keeps its CUSTOM_MATCHER attribution and an {@code ignoring(...)} path keeps
     * IGNORE_PATH.
     *
     * <p>This checks attribution, not order. The rewiring also changes the order of the ignoredFields array,
     * because the removal set is now built in two stages rather than one and a HashSet's iteration order
     * depends on that -- but the divergence needs about a dozen paths before the two sizings differ, so it is
     * documented as an accepted consequence rather than pinned here.
     */
    @Test
    public void ignoredFieldsKeepsTheReasonAttributionForWithAndForIgnoring() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY
                    .jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            underTest.with("beanInteger", org.hamcrest.Matchers.notNullValue())
                    .ignoring("beanLong")
                    .withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            JsonObject json = JsonParser.parseString(error.getMessage()).getAsJsonObject();
            com.google.gson.JsonArray ignoredFields = json.getAsJsonArray("ignoredFields");

            Map<String, String> reasonByPath = new LinkedHashMap<>();
            for (int i = 0; i < ignoredFields.size(); i++) {
                JsonObject entry = ignoredFields.get(i).getAsJsonObject();
                reasonByPath.put(entry.get("path").getAsString(), entry.get("reason").getAsString());
            }
            assertEquals("CUSTOM_MATCHER", reasonByPath.get("beanInteger"),
                    "a with(...) path keeps its CUSTOM_MATCHER attribution after the rewiring");
            assertEquals("IGNORE_PATH", reasonByPath.get("beanLong"),
                    "an explicit ignoring(...) path is still attributed to the ignore rule");
        });
    }

    // Case B — mismatch via system property
    @Test
    public void shouldOutputMachineReadableMessageOnMismatchWhenSystemPropertyEnabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        System.setProperty("fileMatcherMachineReadable", "true");
        try {
            inMemoryUnixFs(imfsi -> {
                DummyInformation dummyTestInfo = dummyInformation(imfsi);
                JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

                AssertionFailedError error = assertThrows(AssertionFailedError.class,
                        () -> assertThat(actual, underTest));

                String msg = error.getMessage();
                JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                String approvedPath = jsonDir.resolve("11b2ef-approved.json").toAbsolutePath().toString();
                assertEquals(approvedPath, json.get("approvedFile").getAsString());
                assertTrue(json.has("actual"));
            });
        } finally {
            System.clearProperty("fileMatcherMachineReadable");
        }
    }

    // Case B — mismatch with machine-readable OFF (default): structured output absent
    @Test
    public void shouldNotOutputMachineReadableMarkersWhenMachineReadableDisabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            assertAll(
                    () -> assertFalse(msg.contains("\"failureType\""), "Should NOT contain JSON failureType field"),
                    () -> assertFalse(msg.contains("\"approvedFile\""), "Should NOT contain JSON approvedFile field")
            );
        });
    }

    // Case A — no approved file, machine-readable ON: absolute paths in message
    @Test
    public void shouldOutputAbsolutePathsWhenNoApprovedFileAndMachineReadableEnabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            Path notApprovedFile = jsonDir.resolve("11b2ef-not-approved.json");
            Path approvedFile = jsonDir.resolve("11b2ef-approved.json");

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));

            String msg = error.getMessage();
            String expectedMsg = getMachineReadableNotApprovedCreationMessage(notApprovedFile, approvedFile);
            assertEquals(expectedMsg, msg);
        });
    }

    // Case A — no approved file, machine-readable OFF (default): old relative-path format unchanged
    @Test
    public void shouldOutputRelativePathWhenNoApprovedFileAndMachineReadableDisabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));

            assertEquals(getNotApprovedCreationMessage("4ac405", "11b2ef-not-approved.json", "11b2ef-approved.json"),
                    error.getMessage());
        });
    }

    // Deep path — Case B (content mismatch), working dir completely unrelated to file path
    @Test
    public void shouldShowCorrectAbsolutePathWithDeepDirectoryStructureAndUnrelatedWorkingDirectory() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            FileSystem fs = imfsi.getInMemoryFileSystem();
            Path deepTestPath;
            try {
                deepTestPath = Files.createDirectories(fs.getPath("/projects/myapp/module/src/test/java"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path workingDir = fs.getPath("/home/ci/builds/workspace");

            DummyInformation dummyTestInfo = new DummyInformation(deepTestPath, workingDir);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path classHashDir = deepTestPath.resolve("4ac405");
            Path approvedFile = classHashDir.resolve("11b2ef-approved.json");
            writeApprovedFile(classHashDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            String expectedAbsPath = approvedFile.toAbsolutePath().toString();
            assertEquals(expectedAbsPath, json.get("approvedFile").getAsString(),
                    "Absolute path must point to the correct file even when working dir is unrelated.");
        });
    }

    // Deep path — Case A (no approved file), working dir completely unrelated to file path
    @Test
    public void shouldShowCorrectAbsoluteNotApprovedPathWithDeepDirectoryStructureAndUnrelatedWorkingDirectory() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            FileSystem fs = imfsi.getInMemoryFileSystem();
            Path deepTestPath;
            try {
                deepTestPath = Files.createDirectories(fs.getPath("/projects/myapp/module/src/test/java"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path workingDir = fs.getPath("/home/ci/builds/workspace");

            DummyInformation dummyTestInfo = new DummyInformation(deepTestPath, workingDir);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path classHashDir = deepTestPath.resolve("4ac405");
            Path notApprovedFile = classHashDir.resolve("11b2ef-not-approved.json");
            Path approvedFile = classHashDir.resolve("11b2ef-approved.json");

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));

            String expectedMsg = getMachineReadableNotApprovedCreationMessage(notApprovedFile, approvedFile);
            assertEquals(expectedMsg, error.getMessage(),
                    "Both paths in Case A message must be absolute and correct even when working dir is unrelated");
        });
    }

    // Deep path — Case B (content mismatch), working dir is an ancestor of the approved file
    @Test
    public void shouldShowCorrectAbsolutePathWithDeepDirectoryStructureAndWorkingDirAsAncestor() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            FileSystem fs = imfsi.getInMemoryFileSystem();
            Path deepTestPath;
            try {
                deepTestPath = Files.createDirectories(fs.getPath("/deep/project/src/test/java/classes"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path workingDir = fs.getPath("/deep/project");

            DummyInformation dummyTestInfo = new DummyInformation(deepTestPath, workingDir);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path classHashDir = deepTestPath.resolve("4ac405");
            Path approvedFile = classHashDir.resolve("11b2ef-approved.json");
            writeApprovedFile(classHashDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            String expectedAbsPath = approvedFile.toAbsolutePath().toString();
            assertEquals(expectedAbsPath, json.get("approvedFile").getAsString(),
                    "Absolute path must be correct even when working dir is an ancestor of the file.");
        });
    }

    private void writeApprovedFile(Path dir, String fileName, String content) {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Alias test — fMMReadable alias should enable machine-readable output
    @Test
    public void shouldOutputMachineReadableMessageWhenAliasPropertyEnabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        System.setProperty("fMMReadable", "true");
        try {
            inMemoryUnixFs(imfsi -> {
                DummyInformation dummyTestInfo = dummyInformation(imfsi);
                JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

                AssertionFailedError error = assertThrows(AssertionFailedError.class,
                        () -> assertThat(actual, underTest));

                String msg = error.getMessage();
                JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                String approvedPath = jsonDir.resolve("11b2ef-approved.json").toAbsolutePath().toString();
                assertEquals(approvedPath, json.get("approvedFile").getAsString());
                assertTrue(json.has("actual"));
            });
        } finally {
            System.clearProperty("fMMReadable");
        }
    }

    // Alias test — fmAI alias should enable machine-readable output
    @Test
    public void shouldOutputMachineReadableMessageWhenFmAIAliasPropertyEnabled() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        System.setProperty("fmAI", "true");
        try {
            inMemoryUnixFs(imfsi -> {
                DummyInformation dummyTestInfo = dummyInformation(imfsi);
                JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

                AssertionFailedError error = assertThrows(AssertionFailedError.class,
                        () -> assertThat(actual, underTest));

                String msg = error.getMessage();
                JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
                String approvedPath = jsonDir.resolve("11b2ef-approved.json").toAbsolutePath().toString();
                assertEquals(approvedPath, json.get("approvedFile").getAsString());
                assertTrue(json.has("actual"));
            });
        } finally {
            System.clearProperty("fmAI");
        }
    }

    @Test
    public void shouldTrackIgnoredPathsInJsonMatcherOutput() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.ignoring("beanLong").withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            com.google.gson.JsonArray ignoredFields = json.getAsJsonArray("ignoredFields");
            assertTrue(ignoredFields.size() > 0, "ignoredFields should not be empty");

            boolean foundBeanLong = false;
            for (int i = 0; i < ignoredFields.size(); i++) {
                JsonObject entry = ignoredFields.get(i).getAsJsonObject();
                if ("beanLong".equals(entry.get("path").getAsString())
                        && "IGNORE_PATH".equals(entry.get("reason").getAsString())) {
                    foundBeanLong = true;
                    break;
                }
            }
            assertTrue(foundBeanLong, "Should track beanLong as IGNORE_PATH");
        });
    }

    @Test
    public void shouldShowNoteForPatternBasedIgnoringInJsonMatcherOutput() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.ignoring(org.hamcrest.Matchers.startsWith("beanL")).withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            assertTrue(json.has("note"), "Should have a note about pattern-based ignoring");
            String note = json.get("note").getAsString();
            assertTrue(note.contains("Pattern-based ignoring"), "Note should mention pattern-based ignoring, was: " + note);
        });
    }

    @Test
    public void shouldOutputCompactExpectedAndActualInMachineReadableJson() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", EXISTING_APPROVED_CONTENT);

            AssertionFailedError error = assertThrows(AssertionFailedError.class,
                    () -> assertThat(actual, underTest));

            String msg = error.getMessage();
            JsonObject json = JsonParser.parseString(msg).getAsJsonObject();
            String expectedValue = json.get("expected").getAsString();
            String actualValue = json.get("actual").getAsString();
            assertFalse(expectedValue.contains("\n"), "expected JSON value in machine-readable output must be compact (no newlines)");
            assertFalse(actualValue.contains("\n"), "actual JSON value in machine-readable output must be compact (no newlines)");
        });
    }

    @Test
    public void shouldNotLeakEitherSortingMarkerIntoMachineReadableOutput() {
        // The sorting markers are stripped from the written JSON by removeSetMarker, which never sees the
        // machine-readable message: tracker paths are built from the field name as resolved, so a missed
        // strip in getOriginalFieldName would show up here and nowhere else.
        Map<ChildBean, String> mapField = new LinkedHashMap<>();
        mapField.put(child().childString("k").build(), "v");
        Set<String> setField = new LinkedHashSet<>(Arrays.asList("b", "a"));
        Map<ChildBean, Map<ChildBean, String>> nestedMapField = new LinkedHashMap<>();
        nestedMapField.put(child().childString("outer").build(), mapField);
        MarkedFields actual = new MarkedFields(mapField, setField, nestedMapField);

        inMemoryUnixFs(imfsi -> {
            JsonMatcher<MarkedFields> underTest =
                    MATCHER_FACTORY.jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", "{}");

            AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> assertThat(actual, underTest));

            assertAll(
                    () -> assertFalse(error.getMessage().contains(FieldsIgnorer.MARKER),
                            "MARKER leaked into machine-readable output"),
                    () -> assertFalse(error.getMessage().contains(FieldsIgnorer.MAP_MARKER),
                            "MAP_MARKER leaked into machine-readable output"));
        });
    }

    @Test
    public void shouldNotLeakASortingMarkerIntoATrackedPath() {
        // The message guard above covers what is compared; a tracker path is built separately, from the key
        // as it stands in the tree, and nothing strips those. An entry ignored by pattern inside a
        // Map-typed field is the shortest route to one.
        Map<String, String> lookup = new LinkedHashMap<>();
        lookup.put("dropMe", "v");
        lookup.put("keepMe", "w");
        TrackedPathHolder actual = new TrackedPathHolder(lookup);

        inMemoryUnixFs(imfsi -> {
            JsonMatcher<TrackedPathHolder> underTest =
                    MATCHER_FACTORY.jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            underTest.withMachineReadableOutput();
            underTest.ignoring(org.hamcrest.Matchers.equalTo("dropMe"));

            Path jsonDir = imfsi.getTestPath().resolve("4ac405");
            writeApprovedFile(jsonDir, "11b2ef-approved.json", "{}");

            AssertionFailedError error = assertThrows(AssertionFailedError.class, () -> assertThat(actual, underTest));

            JsonObject json = JsonParser.parseString(error.getMessage()).getAsJsonObject();
            String ignoredFields = json.get("ignoredFields").toString();
            assertAll(
                    () -> assertFalse(ignoredFields.contains(FieldsIgnorer.MARKER),
                            "MARKER leaked into a tracked path: " + ignoredFields),
                    () -> assertFalse(ignoredFields.contains(FieldsIgnorer.MAP_MARKER),
                            "MAP_MARKER leaked into a tracked path: " + ignoredFields),
                    () -> assertTrue(ignoredFields.contains("lookup[0].dropMe"),
                            "Expected the path under its declared name, was: " + ignoredFields));
        });
    }

    static class TrackedPathHolder {
        final Map<String, String> lookup;

        TrackedPathHolder(Map<String, String> lookup) {
            this.lookup = lookup;
        }
    }

    static class MarkedFields {
        final Map<ChildBean, String> mapField;
        final Set<String> setField;
        /**
         * A field whose declared type describes a level below it, so its name carries more than one
         * sentinel. Without it every field here is a single prefix and the guard cannot catch a strip that
         * stops after the first.
         */
        final Map<ChildBean, Map<ChildBean, String>> nestedMapField;

        MarkedFields(Map<ChildBean, String> mapField,
                     Set<String> setField,
                     Map<ChildBean, Map<ChildBean, String>> nestedMapField) {
            this.mapField = mapField;
            this.setField = setField;
            this.nestedMapField = nestedMapField;
        }
    }
}
