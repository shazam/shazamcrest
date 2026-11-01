package com.github.karsaig.approvalcrest.matcher.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.matcher.AbstractFileMatcherTest;
import com.github.karsaig.approvalcrest.matcher.ContentMatcher;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;
import com.github.karsaig.approvalcrest.util.InMemoryFiles;
import com.github.karsaig.approvalcrest.util.InMemoryFsInfo;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * The write-side shared-approval integration for {@code ContentMatcher}.
 *
 * <p>Only the {@code JsonMatcher} side was covered before, so the content side of this feature was
 * untested — these fill that gap as well as covering the per-type selection.
 */
public class ContentMatcherSharedApprovalTest extends AbstractFileMatcherTest {

    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";
    private static final String CONTENT = "shared content";

    private Path canonicalPathFor(InMemoryFsInfo imfsi, String content, String sharedDir) {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils("content", getDefaultFileMatcherConfig());
        String key = utils.computeContentKey(content);
        String bucket = key.substring(0, 2);
        return imfsi.getWorkingDirectory().resolve(sharedDir).resolve(bucket).resolve(key + "-approved.content");
    }

    private String relativeToWorkDir(InMemoryFsInfo imfsi, Path path) {
        return imfsi.getWorkingDirectory().relativize(path).toString().replace('\\', '/');
    }

    private String notApprovedContent(List<InMemoryFiles> files) {
        return files.stream()
                .filter(f -> f.getPath().contains("-not-approved.content"))
                .findFirst()
                .map(InMemoryFiles::getContent)
                .orElseThrow(() -> new AssertionError("not-approved file not found in: " + files));
    }

    private FileMatcherConfig sharedFor(ApprovedFileType... types) {
        return new FileMatcherConfig(false, true, false, true, DEFAULT_SHARED_DIR,
                types.length == 0 ? ApprovedFileType.none() : EnumSet.copyOf(java.util.Arrays.asList(types)), 2);
    }

    /** With content selected and a matching canonical present, a new file becomes a pointer. */
    @Test
    public void newContentFileWithSharedEnabledForContentWritesPointer() {
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            ContentMatcher<String> underTest =
                    MATCHER_FACTORY.contentMatcher(dummyTestInfo, sharedFor(ApprovedFileType.CONTENT));

            Path canonical = canonicalPathFor(imfsi, CONTENT, DEFAULT_SHARED_DIR);
            writeFile(canonical, "/*shared*/\n" + CONTENT);
            String canonicalRelative = relativeToWorkDir(imfsi, canonical);

            MatcherAssert.assertThat(CONTENT, underTest);

            String created = notApprovedContent(getFiles(imfsi));
            assertTrue(created.contains("/*pointer:"), "expected a pointer, was: " + created);
            assertTrue(created.contains(canonicalRelative), "expected it to reference the canonical");
        });
    }

    /** With no matching canonical the content is written normally, even when enabled. */
    @Test
    public void newContentFileWithNoMatchingCanonicalWritesContent() {
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            ContentMatcher<String> underTest =
                    MATCHER_FACTORY.contentMatcher(dummyTestInfo, sharedFor(ApprovedFileType.CONTENT));

            MatcherAssert.assertThat(CONTENT, underTest);

            String created = notApprovedContent(getFiles(imfsi));
            assertFalse(created.contains("/*pointer:"), "expected standalone content, was: " + created);
            assertTrue(created.contains(CONTENT));
        });
    }

    /**
     * The selection is honoured: with only json enabled, a matching content canonical is ignored and
     * the content file keeps its own copy.
     */
    @Test
    public void contentIsNotPointerisedWhenOnlyJsonIsSelected() {
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            ContentMatcher<String> underTest =
                    MATCHER_FACTORY.contentMatcher(dummyTestInfo, sharedFor(ApprovedFileType.JSON));

            writeFile(canonicalPathFor(imfsi, CONTENT, DEFAULT_SHARED_DIR), "/*shared*/\n" + CONTENT);

            MatcherAssert.assertThat(CONTENT, underTest);

            String created = notApprovedContent(getFiles(imfsi));
            assertFalse(created.contains("/*pointer:"),
                    "a json-only selection must leave content files standalone, was: " + created);
            assertTrue(created.contains(CONTENT));
        });
    }

    /** Disabled entirely behaves as before the property gained values. */
    @Test
    public void contentIsNotPointerisedWhenSharedIsDisabled() {
        inMemoryUnixFs(imfsi -> {
            DummyInformation dummyTestInfo = dummyInformation(imfsi);
            ContentMatcher<String> underTest = MATCHER_FACTORY.contentMatcher(dummyTestInfo, sharedFor());

            writeFile(canonicalPathFor(imfsi, CONTENT, DEFAULT_SHARED_DIR), "/*shared*/\n" + CONTENT);

            MatcherAssert.assertThat(CONTENT, underTest);

            assertFalse(notApprovedContent(getFiles(imfsi)).contains("/*pointer:"));
        });
    }

    /** The matcher knows its own type, which is what the per-type check depends on. */
    @Test
    public void contentMatcherReportsItsApprovedFileType() {
        assertEquals(ApprovedFileType.CONTENT,
                new FileStoreMatcherUtils("content", getDefaultFileMatcherConfig()).getApprovedFileType());
        assertEquals(ApprovedFileType.JSON,
                new FileStoreMatcherUtils("json", getDefaultFileMatcherConfig()).getApprovedFileType());
    }
}
