package com.github.karsaig.approvalcrest.dedup;

import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.DEFAULT_JIMFS_PERMISSIONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.DIRECTORY_CREATE_PERMISSONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.FILE_CREATE_PERMISSONS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

public class DedupCliTest {

    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";
    private static final int DEFAULT_BUCKET_DEPTH = 2;

    private FileSystem newFs() {
        return Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG);
    }

    private Path workingDir(FileSystem fs) {
        return fs.getPath("/work");
    }

    private Path writeApprovedFile(Path workDir, String relPath, String comment, String content) throws IOException {
        Path file = workDir.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.write(file, ("/*" + comment + "*/\n" + content).getBytes(UTF_8));
        return file;
    }

    private String writeCanonicalAndGetRelative(Path workDir, String content, String sharedDir) throws IOException {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                new FileMatcherConfig(false, false, false, true, sharedDir, true, DEFAULT_BUCKET_DEPTH));
        return utils.writeCanonical(content, "shared", workDir, sharedDir, DEFAULT_BUCKET_DEPTH);
    }

    private Path writePointerFile(Path workDir, String relPath, String comment, String canonicalRelative) throws IOException {
        Path file = workDir.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.write(file, ("/*" + comment + "*/\n/*pointer:" + canonicalRelative + "*/").getBytes(UTF_8));
        return file;
    }

    private String computeKey(String content) {
        FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                new FileMatcherConfig(false, false, false, true, DEFAULT_SHARED_DIR, true, DEFAULT_BUCKET_DEPTH));
        return utils.computeContentKey(content);
    }

    private static String ap(Path workDir, String rel) {
        return workDir.resolve(rel).toAbsolutePath().toString();
    }

    private String run(String[] args, Path workDir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captured = new PrintStream(baos, true, "UTF-8");
        DedupCli.run(args, workDir, captured);
        return baos.toString("UTF-8");
    }

    @Test
    public void noArgsScanDefaultDirAndApplyConfigDefaults() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"cli\":true}";

            writeApprovedFile(workDir, "src/test/java/a/file1-approved.json", "a.file1", content);
            writeApprovedFile(workDir, "src/test/java/b/file2-approved.json", "b.file2", content);

            String output = run(new String[]{}, workDir);

            assertTrue(output.contains("Deduplication complete:"), "Output must report completion");

            Path canonical = workDir.resolve(DEFAULT_SHARED_DIR)
                    .resolve(computeKey(content).substring(0, DEFAULT_BUCKET_DEPTH))
                    .resolve(computeKey(content) + "-approved.json");
            assertTrue(Files.exists(canonical), "Canonical must be created under default shared dir");

            String key = computeKey(content);
            String bucket = key.substring(0, DEFAULT_BUCKET_DEPTH);
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/file1-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/file2-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket + "/" + key + "-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void dirArgOverridesDefaultScanDirectory() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"custom\":\"scan\"}";

            writeApprovedFile(workDir, "custom/scan/a/file1-approved.json", "a.file1", content);
            writeApprovedFile(workDir, "custom/scan/b/file2-approved.json", "b.file2", content);

            String output = run(new String[]{"--dir", "custom/scan"}, workDir);

            assertTrue(output.contains("Deduplication complete:"));
            assertTrue(readFile(workDir.resolve("custom/scan/a/file1-approved.json")).contains("/*pointer:"),
                    "File in custom scan dir should be a pointer");

            // writeApprovedFile creates custom/... with DEFAULT; writeCanonical creates DEFAULT_SHARED_DIR with DIRECTORY_CREATE
            String key = computeKey(content);
            String bucket = key.substring(0, DEFAULT_BUCKET_DEPTH);
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "custom"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/a/file1-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/b/file2-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket + "/" + key + "-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void sharedDirArgOverridesDefault() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"custom\":\"shared\"}";

            writeApprovedFile(workDir, "src/test/java/a/file1-approved.json", "a.file1", content);
            writeApprovedFile(workDir, "src/test/java/b/file2-approved.json", "b.file2", content);

            String output = run(new String[]{"--shared-dir", "my/shared"}, workDir);

            assertTrue(output.contains("Deduplication complete:"));
            assertFalse(Files.exists(workDir.resolve(DEFAULT_SHARED_DIR)),
                    "Default shared dir must not be created when --shared-dir overrides it");

            FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                    new FileMatcherConfig(false, false, false, true, "my/shared", true, DEFAULT_BUCKET_DEPTH));
            String key = utils.computeContentKey(content);
            String bucket = key.substring(0, DEFAULT_BUCKET_DEPTH);
            assertTrue(Files.exists(workDir.resolve("my/shared").resolve(bucket).resolve(key + "-approved.json")),
                    "Canonical must be under my/shared");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/file1-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/file2-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "my"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "my/shared"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "my/shared/" + bucket), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "my/shared/" + bucket + "/" + key + "-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void bucketDepthArgIsRespected() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"bucket\":\"depth\"}";

            writeApprovedFile(workDir, "src/test/java/a/file1-approved.json", "a.file1", content);
            writeApprovedFile(workDir, "src/test/java/b/file2-approved.json", "b.file2", content);

            String output = run(new String[]{"--bucket-depth", "3"}, workDir);

            assertTrue(output.contains("Deduplication complete:"));

            FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                    new FileMatcherConfig(false, false, false, true, DEFAULT_SHARED_DIR, true, 3));
            String key = utils.computeContentKey(content);
            String bucket = key.substring(0, 3);

            assertEquals(3, bucket.length(), "Bucket dir name must be exactly 3 characters");
            assertTrue(Files.exists(workDir.resolve(DEFAULT_SHARED_DIR).resolve(bucket).resolve(key + "-approved.json")),
                    "Canonical must exist under 3-char bucket");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/file1-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/file2-approved.json"), FILE_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, DEFAULT_SHARED_DIR + "/" + bucket + "/" + key + "-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void dryRunFlagPreventsAnyWrites() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"dry\":true}";

            Path file1 = writeApprovedFile(workDir, "src/test/java/a/file1-approved.json", "a.file1", content);
            Path file2 = writeApprovedFile(workDir, "src/test/java/b/file2-approved.json", "b.file2", content);
            String original1 = readFile(file1);
            String original2 = readFile(file2);

            String output = run(new String[]{"--dry-run"}, workDir);

            assertTrue(output.contains("Deduplication complete:"));
            assertEquals(original1, readFile(file1), "file1 must not be modified");
            assertEquals(original2, readFile(file2), "file2 must not be modified");
            assertFalse(Files.exists(workDir.resolve(DEFAULT_SHARED_DIR)), "No shared dir must be created");

            // Dry-run writes nothing; all entries keep DEFAULT permissions from test setup
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/file1-approved.json"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/b/file2-approved.json"), DEFAULT_JIMFS_PERMISSIONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void reinstateArgDispatchesToApprovalReinstate() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"pointed\":true}";

            // writeCanonical creates all intermediate dirs with DIRECTORY_CREATE
            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, DEFAULT_SHARED_DIR);
            // writePointerFile (raw Files.write) creates src/test/java/a with DEFAULT
            Path pointer = writePointerFile(workDir, "src/test/java/a/ptr-approved.json", "a.ptr", canonicalRelative);

            String output = run(new String[]{"--reinstate"}, workDir);

            assertTrue(output.contains("Reinstate complete:"), "Output must report reinstate");
            assertFalse(readFile(pointer).contains("/*pointer:"), "Pointer must be reinstated");
            assertTrue(readFile(pointer).contains(content), "Reinstated file must contain original content");

            // After reinstate: canonical deleted, empty shared dirs removed, pointer reinstated to FILE_CREATE
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/ptr-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void allDedupFlagsTogetherAreRespected() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"all\":\"flags\"}";

            writeApprovedFile(workDir, "custom/scan/a/file1-approved.json", "a.file1", content);
            writeApprovedFile(workDir, "custom/scan/b/file2-approved.json", "b.file2", content);

            String output = run(new String[]{"--dir", "custom/scan", "--shared-dir", "my/shared",
                    "--bucket-depth", "3", "--dry-run"}, workDir);

            assertTrue(output.contains("Deduplication complete:"));
            assertFalse(Files.exists(workDir.resolve("my/shared")), "Dry-run must not create shared dir");
            assertFalse(readFile(workDir.resolve("custom/scan/a/file1-approved.json")).contains("/*pointer:"),
                    "Dry-run must not convert files to pointers");

            // Dry-run with custom dir; only setup files exist, all with DEFAULT
            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "custom"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/a/file1-approved.json"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/b"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "custom/scan/b/file2-approved.json"), DEFAULT_JIMFS_PERMISSIONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    @Test
    public void reinstateHonoursDryRunFlag() throws IOException {
        // --dry-run reports what reinstate would do without touching the filesystem.
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"nodryrun\":true}";

            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, DEFAULT_SHARED_DIR);
            Path pointer = writePointerFile(workDir, "src/test/java/a/ptr-approved.json", "a.ptr", canonicalRelative);
            String pointerBefore = readFile(pointer);

            String output = run(new String[]{"--reinstate", "--dry-run"}, workDir);

            assertTrue(output.contains("Reinstate complete:"), "Reinstate must still report what it would do");
            assertEquals(pointerBefore, readFile(pointer), "--dry-run must not rewrite the pointer");
            assertTrue(Files.exists(workDir.resolve(canonicalRelative)),
                    "--dry-run must not delete the canonical");
        }
    }

    @Test
    public void reinstateWithoutDryRunRewritesThePointer() throws IOException {
        try (FileSystem fs = newFs()) {
            Path workDir = workingDir(fs);
            String content = "{\"nodryrun\":true}";

            String canonicalRelative = writeCanonicalAndGetRelative(workDir, content, DEFAULT_SHARED_DIR);
            Path pointer = writePointerFile(workDir, "src/test/java/a/ptr-approved.json", "a.ptr", canonicalRelative);

            String output = run(new String[]{"--reinstate"}, workDir);

            assertTrue(output.contains("Reinstate complete:"));
            assertFalse(readFile(pointer).contains("/*pointer:"), "the pointer is replaced by its content");

            Set<InMemoryPermissions> expected = new HashSet<>(Arrays.asList(
                    new InMemoryPermissions(ap(workDir, "src"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java"), DIRECTORY_CREATE_PERMISSONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a"), DEFAULT_JIMFS_PERMISSIONS),
                    new InMemoryPermissions(ap(workDir, "src/test/java/a/ptr-approved.json"), FILE_CREATE_PERMISSONS)
            ));
            assertEquals(expected, new HashSet<>(InMemoryFsUtil.getPermissons(fs)));
        }
    }

    private String readFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), UTF_8);
    }
}
