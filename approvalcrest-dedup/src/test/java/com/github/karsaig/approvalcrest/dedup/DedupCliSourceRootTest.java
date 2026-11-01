package com.github.karsaig.approvalcrest.dedup;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.github.karsaig.approvalcrest.util.InMemoryFsUtil;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class DedupCliSourceRootTest {

    private static final String DUPLICATE = "{\n  \"total\": 42\n}";
    private static final String POINTER = "/*pointer:src/test/java/shared-approvals/a5/a5e115a0bbfb-17-approved.json*/";

    @AfterEach
    public void clearProperties() {
        System.clearProperty("fileMatcherSourceRoot");
        System.clearProperty("fmSourceRoot");
    }

    private void writeApprovedFile(Path workDir, String relPath, String comment, String content) throws IOException {
        Path file = workDir.resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.write(file, ("/*" + comment + "*/\n" + content).getBytes(UTF_8));
    }

    private void writeDuplicatesUnder(Path workDir, String root) throws IOException {
        writeApprovedFile(workDir, root + "/orders/c1/c2-approved.json", "orders.OrderTest.c", DUPLICATE);
        writeApprovedFile(workDir, root + "/billing/a1/a2-approved.json", "billing.InvoiceTest.a", DUPLICATE);
        writeApprovedFile(workDir, root + "/shipping/b1/b2-approved.json", "shipping.ParcelTest.b", DUPLICATE);
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

    private Map<String, String> pointersUnder(String root) {
        Map<String, String> expected = new TreeMap<>();
        expected.put(root + "/billing/a1/a2-approved.json", "/*billing.InvoiceTest.a*/\n" + POINTER);
        expected.put(root + "/orders/c1/c2-approved.json", "/*orders.OrderTest.c*/\n" + POINTER);
        expected.put(root + "/shipping/b1/b2-approved.json", "/*shipping.ParcelTest.b*/\n" + POINTER);
        return expected;
    }

    private Map<String, String> standaloneUnder(String root) {
        Map<String, String> expected = new TreeMap<>();
        expected.put(root + "/billing/a1/a2-approved.json", "/*billing.InvoiceTest.a*/\n" + DUPLICATE);
        expected.put(root + "/orders/c1/c2-approved.json", "/*orders.OrderTest.c*/\n" + DUPLICATE);
        expected.put(root + "/shipping/b1/b2-approved.json", "/*shipping.ParcelTest.b*/\n" + DUPLICATE);
        return expected;
    }

    private Map<String, String> expectedAfterDedupOf(String dedupedRoot, String untouchedRoot) {
        Map<String, String> expected = new TreeMap<>();
        expected.putAll(pointersUnder(dedupedRoot));
        expected.putAll(standaloneUnder(untouchedRoot));
        expected.put("src/test/java/shared-approvals/a5/a5e115a0bbfb-17-approved.json", "/*shared*/\n" + DUPLICATE);
        return expected;
    }

    private void run(Path workDir, String... args) throws IOException {
        DedupCli.run(args, workDir, new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
    }

    @Test
    public void withoutDirScansTheSourceRootThatTheLongNameSets() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeDuplicatesUnder(workDir, "src/it/java");
            writeDuplicatesUnder(workDir, "src/test/java");
            System.setProperty("fileMatcherSourceRoot", "src/it/java");

            run(workDir);

            assertEquals(expectedAfterDedupOf("src/it/java", "src/test/java"), snapshot(workDir));
        }
    }

    @Test
    public void withoutDirScansTheSourceRootThatTheAliasSets() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeDuplicatesUnder(workDir, "src/it/java");
            writeDuplicatesUnder(workDir, "src/test/java");
            System.setProperty("fmSourceRoot", "src/it/java");

            run(workDir);

            assertEquals(expectedAfterDedupOf("src/it/java", "src/test/java"), snapshot(workDir));
        }
    }

    @Test
    public void backslashesInTheSourceRootActAsSeparators() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeDuplicatesUnder(workDir, "src/it/java");
            writeDuplicatesUnder(workDir, "src/test/java");
            System.setProperty("fmSourceRoot", "src\\it\\java");

            run(workDir);

            assertEquals(expectedAfterDedupOf("src/it/java", "src/test/java"), snapshot(workDir));
        }
    }

    @Test
    public void dirWinsOverTheSourceRoot() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeDuplicatesUnder(workDir, "src/it/java");
            writeDuplicatesUnder(workDir, "src/test/java");
            System.setProperty("fmSourceRoot", "src/it/java");

            run(workDir, "--dir", "src/test/java");

            assertEquals(expectedAfterDedupOf("src/test/java", "src/it/java"), snapshot(workDir));
        }
    }

    @Test
    public void conflictingSourceRootNamesStopTheRun() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            writeDuplicatesUnder(workDir, "src/it/java");
            Map<String, String> before = snapshot(workDir);
            System.setProperty("fileMatcherSourceRoot", "src/it/java");
            System.setProperty("fmSourceRoot", "src/other/java");

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> run(workDir));

            assertEquals("Ambiguous configuration: conflicting values set for properties: [fileMatcherSourceRoot, fmSourceRoot]", e.getMessage());
            assertEquals(before, snapshot(workDir));
        }
    }
}
