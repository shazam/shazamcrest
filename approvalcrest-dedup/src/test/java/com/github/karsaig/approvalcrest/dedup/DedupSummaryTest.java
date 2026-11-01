package com.github.karsaig.approvalcrest.dedup;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.github.karsaig.approvalcrest.util.InMemoryFsUtil;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.Test;

public class DedupSummaryTest {

    private static final String SHARED_DIR = "src/test/java/shared-approvals";
    private static final String DUPLICATE = "{\n  \"total\": 42\n}";

    private Path writeApprovedFile(Path workDir, String relPath, String comment, String content) throws IOException {
        Path file = workDir.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.write(file, ("/*" + comment + "*/\n" + content).getBytes(UTF_8));
        return file;
    }

    private void writeThreeDuplicatesAndOneUnique(Path workDir) throws IOException {
        writeApprovedFile(workDir, "src/test/java/orders/c1/c2-approved.json", "orders.OrderTest.c", DUPLICATE);
        writeApprovedFile(workDir, "src/test/java/billing/a1/a2-approved.json", "billing.InvoiceTest.a", DUPLICATE);
        writeApprovedFile(workDir, "src/test/java/shipping/b1/b2-approved.json", "shipping.ParcelTest.b", DUPLICATE);
        writeApprovedFile(workDir, "src/test/java/orders/c1/u1-approved.json", "orders.OrderTest.unique", "{\n  \"total\": 7\n}");
    }

    private Map<String, String> snapshot(Path workDir) throws IOException {
        Map<String, String> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(workDir)) {
            for (Path p : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                files.put(workDir.relativize(p).toString(), new String(Files.readAllBytes(p), UTF_8));
            }
        }
        return files;
    }

    private ApprovalDeduplicator deduplicator(Path workDir, boolean dryRun) {
        return new ApprovalDeduplicator(workDir, workDir.resolve("src/test/java"), SHARED_DIR, 2, dryRun);
    }

    private ApprovalReinstate reinstater(Path workDir, boolean dryRun) {
        return new ApprovalReinstate(workDir, workDir.resolve("src/test/java"), SHARED_DIR, dryRun);
    }

    @Test
    public void dedupDryRunSummarySaysNothingChanged() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeThreeDuplicatesAndOneUnique(workDir);
            Map<String, String> before = snapshot(workDir);

            String summary = deduplicator(workDir, true).deduplicate().toString();

            assertEquals("Dry run, no files changed. Deduplication complete: 1 canonical(s) created, 3 pointer(s) written, 0 orphaned canonical(s) removed.", summary);
            assertEquals(before, snapshot(workDir));
        }
    }

    @Test
    public void dedupSummaryReportsTheChangesItMade() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeThreeDuplicatesAndOneUnique(workDir);

            String summary = deduplicator(workDir, false).deduplicate().toString();

            assertEquals("Deduplication complete: 1 canonical(s) created, 3 pointer(s) written, 0 orphaned canonical(s) removed.", summary);
        }
    }

    @Test
    public void reinstateDryRunSummarySaysNothingChanged() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeThreeDuplicatesAndOneUnique(workDir);
            deduplicator(workDir, false).deduplicate();
            Map<String, String> before = snapshot(workDir);

            String summary = reinstater(workDir, true).reinstate().toString();

            assertEquals("Dry run, no files changed. Reinstate complete: 3 pointer(s) replaced with standalone content, 1 canonical(s) deleted.", summary);
            assertEquals(before, snapshot(workDir));
        }
    }

    @Test
    public void reinstateSummaryReportsTheChangesItMade() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeThreeDuplicatesAndOneUnique(workDir);
            deduplicator(workDir, false).deduplicate();

            String summary = reinstater(workDir, false).reinstate().toString();

            assertEquals("Reinstate complete: 3 pointer(s) replaced with standalone content, 1 canonical(s) deleted.", summary);
        }
    }
}
