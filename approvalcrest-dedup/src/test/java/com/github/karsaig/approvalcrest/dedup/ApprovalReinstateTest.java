package com.github.karsaig.approvalcrest.dedup;

import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.DEFAULT_JIMFS_PERMISSIONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.DIRECTORY_CREATE_PERMISSONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.FILE_CREATE_PERMISSONS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;
import com.github.karsaig.approvalcrest.util.InMemoryFsUtil;
import com.github.karsaig.approvalcrest.util.InMemoryPermissions;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.Test;

public class ApprovalReinstateTest {

    private static final String SHARED_DIR = "src/test/java/shared-approvals";

    private FileSystem newFs() {
        return Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG);
    }

    private Path workingDir(FileSystem fs) {
        return fs.getPath("/work");
    }

    private String readFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), UTF_8);
    }

    private String writeCanonicalAndGetRelative(Path workDir, String content, String extension) throws IOException {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils(extension,
                new FileMatcherConfig(false, false, false, true, SHARED_DIR, true, 2));
        return utils.writeCanonical(content, "shared", workDir, SHARED_DIR, 2);
    }

    private Path writePointerFile(Path workDir, String relPath, String comment, String canonicalRelative) throws IOException {
        Path file = workDir.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.write(file, ("/*" + comment + "*/\n/*pointer:" + canonicalRelative + "*/").getBytes(UTF_8));
        return file;
    }

    private String computeKey(String content, String extension) {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils(extension,
                new FileMatcherConfig(false, false, false, true, SHARED_DIR, true, 2));
        return utils.computeContentKey(content);
    }

    private static String ap(Path workDir, String rel) {
        return workDir.resolve(rel).toAbsolutePath().toString();
    }

    @Test
    public void pointerFilesAreReplacedWithStandaloneContent() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"reinstated\":true}";

            // writeCanonical creates all intermediate dirs with DIRECTORY_CREATE
            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, "json");
            // writePointerFile (raw Files.write) creates src/test/java/a with DEFAULT
            Path pointer = writePointerFile(workDir, "src/test/java/a/test-approved.json", "a.test", canonicalRelative);

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            ApprovalReinstate.ReinstateResult result = reinstate.reinstate();

            assertEquals(1, result.getPointersReinstated());
            String fileContent = readFile(pointer);
            assertFalse(fileContent.contains("/*pointer:"), "Reinstated file must not contain pointer");
            assertTrue(fileContent.contains(content), "Reinstated file must contain the canonical content");
            assertTrue(fileContent.startsWith("/*a.test*/"), "Reinstated file must preserve original comment");

            // After reinstate: canonical deleted, empty dirs removed, pointer reinstated to FILE_CREATE
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/test-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void canonicalsAreDeletedAfterReinstate() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"canonical\":true}";

            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, "json");
            Path canonical = workDir.resolve(canonicalRelative);
            assertTrue(Files.exists(canonical), "Canonical must exist before reinstate");

            writePointerFile(workDir, "src/test/java/a/test-approved.json", "a.test", canonicalRelative);

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            ApprovalReinstate.ReinstateResult result = reinstate.reinstate();

            assertEquals(1, result.getCanonicalsDeleted());
            assertFalse(Files.exists(canonical), "Canonical must be deleted after reinstate");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/test-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void sharedDirectoryIsRemovedWhenEmptyAfterReinstate() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"cleanup\":true}";

            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, "json");
            writePointerFile(workDir, "src/test/java/a/test-approved.json", "a.test", canonicalRelative);

            Path sharedDirPath = workDir.resolve(SHARED_DIR);
            assertTrue(Files.exists(sharedDirPath));

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            reinstate.reinstate();

            assertFalse(Files.exists(sharedDirPath), "Shared dir should be deleted when empty after reinstate");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/test-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void multiplePointerFilesAllReinstated() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content1 = "{\"a\":1}";
            String content2 = "{\"b\":2}";

            // Both writeCanonical calls create intermediate dirs; second call skips existing ones.
            String canonical1 = writeCanonicalAndGetRelative(workDir, content1, "json");
            String canonical2 = writeCanonicalAndGetRelative(workDir, content2, "json");

            Path ptr1 = writePointerFile(workDir, "src/test/java/a/test1-approved.json", "a.test1", canonical1);
            Path ptr2 = writePointerFile(workDir, "src/test/java/b/test2-approved.json", "b.test2", canonical1);
            Path ptr3 = writePointerFile(workDir, "src/test/java/c/test3-approved.json", "c.test3", canonical2);

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            ApprovalReinstate.ReinstateResult result = reinstate.reinstate();

            assertEquals(3, result.getPointersReinstated());
            assertEquals(2, result.getCanonicalsDeleted());

            assertTrue(readFile(ptr1).contains(content1));
            assertTrue(readFile(ptr2).contains(content1));
            assertTrue(readFile(ptr3).contains(content2));

            // All shared dirs are deleted after reinstate (both canonicals gone, deleteEmptyDirs cleans up).
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/test1-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/test2-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/c"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/c/test3-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void nonPointerApprovedFilesAreNotModifiedDuringReinstate() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String standaloneContent = "{\"standalone\":true}";
            String pointerContent = "{\"pointed\":true}";

            // writeCanonical creates all intermediate dirs with DIRECTORY_CREATE
            String canonicalRelative = writeCanonicalAndGetRelative(workDir, pointerContent, "json");

            // Raw Files.write creates standalone and its parent with DEFAULT
            Path standalone = workDir.resolve("src/test/java/a/standalone-approved.json");
            Files.createDirectories(standalone.getParent());
            Files.write(standalone, ("/*a.standalone*/\n" + standaloneContent).getBytes(UTF_8));
            String originalStandalone = readFile(standalone);

            writePointerFile(workDir, "src/test/java/b/pointed-approved.json", "b.pointed", canonicalRelative);

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            ApprovalReinstate.ReinstateResult result = reinstate.reinstate();

            assertEquals(1, result.getPointersReinstated(), "Only pointer files should be counted");
            assertEquals(originalStandalone, readFile(standalone), "Standalone file must be unchanged");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/standalone-approved.json"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/pointed-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void reinstateHandlesContentPointersAndSkipsNonApprovedFiles() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "plain text body";

            // A .content pointer → exercises getExtension("content") and utilsFor("content").
            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, "content");
            Path contentPointer = writePointerFile(workDir,
                    "src/test/java/a/test-approved.content", "a.test", canonicalRelative);

            // A -not-approved file and a non-approved file must both be ignored by isApprovedFileName.
            Path notApproved = workDir.resolve("src/test/java/a/test-not-approved.json");
            Files.write(notApproved, "ignore me".getBytes(UTF_8));
            Path random = workDir.resolve("src/test/java/a/README.txt");
            Files.write(random, "readme".getBytes(UTF_8));

            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            ApprovalReinstate.ReinstateResult result = reinstate.reinstate();

            assertEquals(1, result.getPointersReinstated(), "Only the .content pointer is reinstated");
            String reinstated = readFile(contentPointer);
            assertFalse(reinstated.contains("/*pointer:"));
            assertTrue(reinstated.contains(content));
            // Non-approved files are untouched.
            assertEquals("ignore me", readFile(notApproved));
            assertEquals("readme", readFile(random));
        }
    }

    @Test
    public void reinstateRoundtripMatchesOriginalContent() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"key\":\"value\",\"nested\":{\"a\":1}}";

            // Raw Files.write creates dirs and files with DEFAULT
            Path originalFile = workDir.resolve("src/test/java/x/original-approved.json");
            Files.createDirectories(originalFile.getParent());
            Files.write(originalFile, ("/*x.original*/\n" + content).getBytes(UTF_8));

            Path anotherFile = workDir.resolve("src/test/java/y/another-approved.json");
            Files.createDirectories(anotherFile.getParent());
            Files.write(anotherFile, ("/*y.another*/\n" + content).getBytes(UTF_8));

            // Dedup: creates shared-approvals dirs with DIRECTORY_CREATE; overwrites both files to FILE_CREATE
            ApprovalDeduplicator dedup = new ApprovalDeduplicator(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR, 2, false);
            dedup.deduplicate();

            assertTrue(readFile(originalFile).contains("/*pointer:"), "Should be pointer after dedup");

            // Reinstate: rewrites both files with FILE_CREATE; deletes canonical; deletes empty shared dirs
            ApprovalReinstate reinstate = new ApprovalReinstate(
                    workDir, workDir.resolve("src/test/java"), SHARED_DIR);
            reinstate.reinstate();

            String restoredContent = readFile(originalFile);
            assertFalse(restoredContent.contains("/*pointer:"), "Should not be pointer after reinstate");
            assertTrue(restoredContent.contains(content), "Content must match original after reinstate");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/x"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/x/original-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/y"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/y/another-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void dryRunCountsOnlyTheCanonicalsThatTheRealRunDeletes() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String sharedWithOtherModule = writeCanonicalAndGetRelative(workDir, "{\"v\":\"shared\"}", "json");
            String onlyInScanDir = writeCanonicalAndGetRelative(workDir, "{\"v\":\"local\"}", "json");
            writePointerFile(workDir, "moduleA/src/test/java/c/c1-approved.json", "c.c1", sharedWithOtherModule);
            writePointerFile(workDir, "moduleA/src/test/java/a/a1-approved.json", "a.a1", sharedWithOtherModule);
            writePointerFile(workDir, "moduleA/src/test/java/b/b1-approved.json", "b.b1", sharedWithOtherModule);
            writePointerFile(workDir, "moduleA/src/test/java/d/d1-approved.json", "d.d1", onlyInScanDir);
            writePointerFile(workDir, "moduleB/src/test/java/e/e1-approved.json", "e.e1", sharedWithOtherModule);
            Path scanDir = workDir.resolve("moduleA/src/test/java");

            ApprovalReinstate.ReinstateResult dryRun = new ApprovalReinstate(workDir, scanDir, SHARED_DIR, true).reinstate();
            ApprovalReinstate.ReinstateResult realRun = new ApprovalReinstate(workDir, scanDir, SHARED_DIR, false).reinstate();

            assertEquals("Dry run, no files changed. Reinstate complete: 4 pointer(s) replaced with standalone content, 1 canonical(s) deleted.", dryRun.toString());
            assertEquals("Reinstate complete: 4 pointer(s) replaced with standalone content, 1 canonical(s) deleted.", realRun.toString());
            assertTrue(Files.exists(workDir.resolve(sharedWithOtherModule)), "moduleB still points to this canonical");
            assertFalse(Files.exists(workDir.resolve(onlyInScanDir)), "no pointer uses this canonical after the reinstate");
        }
    }
}
