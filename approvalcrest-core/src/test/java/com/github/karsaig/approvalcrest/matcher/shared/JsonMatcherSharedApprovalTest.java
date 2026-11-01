package com.github.karsaig.approvalcrest.matcher.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.matcher.AbstractFileMatcherTest;
import com.github.karsaig.approvalcrest.matcher.JsonMatcher;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;
import com.github.karsaig.approvalcrest.testdata.BeanWithPrimitives;
import com.github.karsaig.approvalcrest.util.InMemoryFiles;
import com.github.karsaig.approvalcrest.util.InMemoryFsInfo;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

public class JsonMatcherSharedApprovalTest extends AbstractFileMatcherTest {

    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";

    private Path canonicalPathFor(InMemoryFsInfo imfsi, String content, String sharedDir) {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json", getDefaultFileMatcherConfig());
        String key = utils.computeContentKey(content);
        String bucket = key.substring(0, 2);
        return imfsi.getWorkingDirectory().resolve(sharedDir).resolve(bucket).resolve(key + "-approved.json");
    }

    private String relativeToWorkDir(InMemoryFsInfo imfsi, Path path) {
        return imfsi.getWorkingDirectory().relativize(path).toString().replace('\\', '/');
    }

    private String approvedFileContent(List<InMemoryFiles> files) {
        return files.stream()
                .filter(f -> f.getPath().contains("11b2ef-approved.json"))
                .findFirst()
                .map(InMemoryFiles::getContent)
                .orElseThrow(() -> new AssertionError("approved file not found in: " + files));
    }

    @Test
    public void approvedFileAsPointerPassesTest() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

            String beanJson = getBeanWithPrimitivesAsJsonString();
            Path canonical = canonicalPathFor(imfsi, beanJson, DEFAULT_SHARED_DIR);
            writeFile(canonical, "/*shared*/\n" + beanJson);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n/*pointer:" + canonicalRelative + "*/");

            MatcherAssert.assertThat(actual, underTest);
        });
    }

    @Test
    public void stalePointerInStrictModeFailsTest() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfig());

            String beanJson = getBeanWithPrimitivesAsJsonString();
            Path canonical = canonicalPathFor(imfsi, beanJson, "old-shared");
            writeFile(canonical, "/*shared*/\n" + beanJson);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n/*pointer:" + canonicalRelative + "*/");

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));
            assertTrue(error.getMessage().contains("Stale pointer"), "Expected stale pointer error but got: " + error.getMessage());
            assertTrue(error.getMessage().contains(canonicalRelative), "Expected message to contain the target path");
        });
    }

    @Test
    public void stalePointerInLenientModePassesTest() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, getDefaultFileMatcherConfigWithLenientMatching());

            String beanJson = getBeanWithPrimitivesAsJsonString();
            Path canonical = canonicalPathFor(imfsi, beanJson, "old-shared");
            writeFile(canonical, "/*shared*/\n" + beanJson);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n/*pointer:" + canonicalRelative + "*/");

            MatcherAssert.assertThat(actual, underTest);
        });
    }

    @Test
    public void inPlaceOverwriteOnPointerWithSharedDisabledDetachesToContent() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, enableInPlaceOverwrite());

            // Old canonical with different content → causes a mismatch, triggering in-place overwrite
            String oldContent = "{}";
            Path oldCanonical = canonicalPathFor(imfsi, oldContent, DEFAULT_SHARED_DIR);
            writeFile(oldCanonical, "/*old*/\n" + oldContent);
            String oldCanonicalRelative = relativeToWorkDir(imfsi, oldCanonical);

            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n/*pointer:" + oldCanonicalRelative + "*/");

            // Mismatch (actual=bean, approved→"{}") → overwrite with beanJson → sharedEnabled=false → detach
            MatcherAssert.assertThat(actual, underTest);

            String approvedContent = approvedFileContent(getFiles(imfsi));
            assertFalse(approvedContent.contains("/*pointer:"), "Expected standalone content after detach, not a pointer");
            assertTrue(approvedContent.contains("beanBoolean"), "Expected bean JSON content");
        });
    }

    /**
     * Selecting a single type through the property must actually enable it for that type. Written
     * against the property rather than the enum so it compiles before the change too: previously
     * "json" was read as a boolean, came out false, and nothing was shared at all.
     */
    @Test
    public void sharedEnabledSetToJsonPointerisesJsonFiles() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        System.setProperty("fileMatcherSharedEnabled", "json");
        try {
            inMemoryUnixFs(imfsi -> {
                DummyInformation dummyTestInfo = dummyInformation(imfsi);
                FileMatcherConfig config = new FileMatcherConfig();
                JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, config);

                String beanJson = getBeanWithPrimitivesAsJsonString();
                Path canonical = canonicalPathFor(imfsi, beanJson, DEFAULT_SHARED_DIR);
                writeFile(canonical, "/*shared*/\n" + beanJson);

                assertThrows(AssertionError.class, () -> MatcherAssert.assertThat(actual, underTest));

                String created = getFiles(imfsi).stream()
                        .filter(f -> f.getPath().contains("-not-approved.json"))
                        .findFirst()
                        .map(f -> f.getContent())
                        .orElseThrow(() -> new AssertionError("not-approved file not found"));
                assertTrue(created.contains("/*pointer:"),
                        "json must be pointerised when the property selects json, was: " + created);
            });
        } finally {
            System.clearProperty("fileMatcherSharedEnabled");
        }
    }

    @Test
    public void inPlaceOverwriteWithSharedEnabledAndMatchingCanonicalWritesPointer() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            FileMatcherConfig config = new FileMatcherConfig(true, false, false, true, DEFAULT_SHARED_DIR, true, 2);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, config);

            String beanJson = getBeanWithPrimitivesAsJsonString();
            Path canonical = canonicalPathFor(imfsi, beanJson, DEFAULT_SHARED_DIR);
            writeFile(canonical, "/*shared*/\n" + beanJson);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            // Start with old non-matching content to force a mismatch and trigger overwrite
            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n{}");

            // Mismatch → overwrite → sharedEnabled + canonical found → writes pointer
            MatcherAssert.assertThat(actual, underTest);

            String approvedContent = approvedFileContent(getFiles(imfsi));
            assertTrue(approvedContent.contains("/*pointer:"), "Expected pointer after update with matching canonical");
            assertTrue(approvedContent.contains(canonicalRelative), "Expected pointer to reference the matching canonical");
        });
    }

    @Test
    public void inPlaceOverwriteWithSharedEnabledAndNoMatchingCanonicalDetachesToContent() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            FileMatcherConfig config = new FileMatcherConfig(true, false, false, true, DEFAULT_SHARED_DIR, true, 2);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, config);

            // Old canonical with different content
            String oldContent = "{}";
            Path oldCanonical = canonicalPathFor(imfsi, oldContent, DEFAULT_SHARED_DIR);
            writeFile(oldCanonical, "/*old*/\n" + oldContent);
            String oldCanonicalRelative = relativeToWorkDir(imfsi, oldCanonical);

            // Approved file is a pointer to the old canonical (mismatch with actual)
            Path approvedFile = imfsi.getTestPath().resolve("4ac405").resolve("11b2ef-approved.json");
            writeFile(approvedFile, "/*dummyTestClassName.dummyTestMethodName*/\n/*pointer:" + oldCanonicalRelative + "*/");

            // No canonical for beanJson → detach → write standalone content
            MatcherAssert.assertThat(actual, underTest);

            String approvedContent = approvedFileContent(getFiles(imfsi));
            assertFalse(approvedContent.contains("/*pointer:"), "Expected standalone content after detach, not a pointer");
            assertTrue(approvedContent.contains("beanBoolean"), "Expected bean JSON content after detach");
        });
    }

    @Test
    public void sharedEnabledWithExistingCanonicalCreatesPointerNotApproved() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            FileMatcherConfig config = new FileMatcherConfig(false, false, false, true, DEFAULT_SHARED_DIR, true, 2);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, config);

            String beanJson = getBeanWithPrimitivesAsJsonString();
            Path canonical = canonicalPathFor(imfsi, beanJson, DEFAULT_SHARED_DIR);
            writeFile(canonical, "/*shared*/\n" + beanJson);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));
            assertTrue(error.getMessage().contains("Not approved file created"), "Expected not-approved creation message");

            List<InMemoryFiles> files = getFiles(imfsi);
            String notApprovedContent = files.stream()
                    .filter(f -> f.getPath().contains("11b2ef-not-approved.json"))
                    .findFirst()
                    .map(InMemoryFiles::getContent)
                    .orElseThrow(() -> new AssertionError("not-approved file not found in: " + files));
            assertTrue(notApprovedContent.contains("/*pointer:"), "Expected pointer in not-approved file");
            assertTrue(notApprovedContent.contains(canonicalRelative), "Expected pointer to reference the canonical");
        });
    }

    @Test
    public void sharedEnabledWithNoCanonicalCreatesContentNotApproved() {
        BeanWithPrimitives actual = getBeanWithPrimitives();
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            FileMatcherConfig config = new FileMatcherConfig(false, false, false, true, DEFAULT_SHARED_DIR, true, 2);
            JsonMatcher<BeanWithPrimitives> underTest = MATCHER_FACTORY.jsonMatcher(dummyTestInfo, config);

            AssertionError error = assertThrows(AssertionError.class,
                    () -> MatcherAssert.assertThat(actual, underTest));
            assertTrue(error.getMessage().contains("Not approved file created"), "Expected not-approved creation message");

            List<InMemoryFiles> files = getFiles(imfsi);
            String notApprovedContent = files.stream()
                    .filter(f -> f.getPath().contains("11b2ef-not-approved.json"))
                    .findFirst()
                    .map(InMemoryFiles::getContent)
                    .orElseThrow(() -> new AssertionError("not-approved file not found in: " + files));
            assertFalse(notApprovedContent.contains("/*pointer:"), "Expected content not-approved, not a pointer");
            assertTrue(notApprovedContent.contains("beanBoolean"), "Expected bean JSON content in not-approved file");
        });
    }
}
