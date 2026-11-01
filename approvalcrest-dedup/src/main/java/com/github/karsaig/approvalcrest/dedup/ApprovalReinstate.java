package com.github.karsaig.approvalcrest.dedup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;

/**
 * Replaces pointer files in a directory with standalone approved files containing the canonical
 * content, then removes the canonical files that nothing references any more.
 *
 * <p>After reinstating the whole project, the repository is in the same state as before any
 * deduplication was run.
 */
public class ApprovalReinstate {

    private final Path workingDirectory;
    private final Path scanDir;
    private final String sharedApprovalDir;
    private final boolean dryRun;
    private final ApprovedFileScanner scanner;

    public ApprovalReinstate(Path workingDirectory, Path scanDir, String sharedApprovalDir) {
        this(workingDirectory, scanDir, sharedApprovalDir, false);
    }

    /**
     * Reinstates every approved file type.
     */
    public ApprovalReinstate(Path workingDirectory, Path scanDir, String sharedApprovalDir, boolean dryRun) {
        this(workingDirectory, scanDir, sharedApprovalDir, dryRun, ApprovedFileType.all());
    }

    /**
     * @param selectedTypes which approved file types to reinstate. Pointers of any other type are
     *                      left in place, and so are their canonicals.
     */
    ApprovalReinstate(Path workingDirectory, Path scanDir, String sharedApprovalDir, boolean dryRun,
                      Set<ApprovedFileType> selectedTypes) {
        DedupPaths.requireSharedDirDoesNotContainScanDir(workingDirectory, scanDir, sharedApprovalDir);
        this.workingDirectory = workingDirectory;
        this.scanDir = scanDir;
        this.sharedApprovalDir = sharedApprovalDir;
        this.dryRun = dryRun;
        this.scanner = new ApprovedFileScanner(sharedApprovalDir, 2, selectedTypes);
    }

    /**
     * Replaces pointers under the scan directory with their content, then deletes the canonicals
     * that are no longer referenced.
     *
     * <p>Deletion is reference-checked rather than unconditional. Reinstating a narrowed scan
     * directory leaves pointers elsewhere in the project untouched, and those still need their
     * canonicals; removing them would leave the pointers resolving to nothing.
     */
    public ReinstateResult reinstate() throws IOException {
        Path sharedDirPath = workingDirectory.resolve(sharedApprovalDir).normalize();

        List<Path> allApprovedFiles = scanner.collectApprovedFiles(scanDir, sharedDirPath);

        int pointersReinstated = 0;
        Set<Path> reinstated = new HashSet<>();
        for (Path file : allApprovedFiles) {
            ApprovedFileType ext = scanner.getType(file);
            if (ext == null || !scanner.isSelectedType(file)) {
                continue;
            }
            FileStoreMatcherUtils utils = scanner.utilsFor(ext);
            if (utils.isPointerFile(file)) {
                String comment = utils.readComment(file);
                // readFile follows pointer chains and returns the final canonical content
                String content = utils.readFile(file, workingDirectory);
                if (!dryRun) {
                    utils.writeStandaloneFile(file, content, comment);
                }
                reinstated.add(file.toAbsolutePath().normalize());
                pointersReinstated++;
            }
        }

        int canonicalsDeleted = deleteUnreferencedCanonicals(sharedDirPath, reinstated);

        return new ReinstateResult(pointersReinstated, canonicalsDeleted, dryRun);
    }

    private int deleteUnreferencedCanonicals(Path sharedDirPath, Set<Path> reinstated) throws IOException {
        if (!Files.exists(sharedDirPath)) {
            return 0;
        }

        // In a dry run the reinstated pointers are still on disk; excluding them describes the state
        // that a real run leaves behind.
        Set<String> referenced = DedupPaths.collectReferencedCanonicals(scanner, workingDirectory, sharedDirPath, reinstated);

        int canonicalsDeleted = 0;
        for (Path canonical : scanner.collectApprovedFiles(sharedDirPath, null)) {
            // Only canonicals of the selected types are candidates, so reinstating one type cannot
            // strand the other type's pointers.
            if (!scanner.isSelectedType(canonical)) {
                continue;
            }
            if (!referenced.contains(DedupPaths.relativize(workingDirectory, canonical))) {
                if (!dryRun) {
                    Files.delete(canonical);
                }
                canonicalsDeleted++;
            }
        }
        if (!dryRun) {
            deleteEmptyDirs(sharedDirPath);
        }
        return canonicalsDeleted;
    }

    private void deleteEmptyDirs(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Path> children = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(children::add);
        }
        for (Path child : children) {
            if (Files.isDirectory(child)) {
                deleteEmptyDirs(child);
            }
        }
        List<Path> remaining = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(remaining::add);
        }
        if (remaining.isEmpty()) {
            Files.delete(dir);
        }
    }

    public static class ReinstateResult {
        private final int pointersReinstated;
        private final int canonicalsDeleted;
        private final boolean dryRun;

        public ReinstateResult(int pointersReinstated, int canonicalsDeleted) {
            this(pointersReinstated, canonicalsDeleted, false);
        }

        public ReinstateResult(int pointersReinstated, int canonicalsDeleted, boolean dryRun) {
            this.pointersReinstated = pointersReinstated;
            this.canonicalsDeleted = canonicalsDeleted;
            this.dryRun = dryRun;
        }

        public int getPointersReinstated() {
            return pointersReinstated;
        }

        public int getCanonicalsDeleted() {
            return canonicalsDeleted;
        }

        @Override
        public String toString() {
            return (dryRun ? ApprovalDeduplicator.DeduplicatorResult.DRY_RUN_PREFIX : "")
                    + "Reinstate complete: " + pointersReinstated + " pointer(s) replaced with standalone content, "
                    + canonicalsDeleted + " canonical(s) deleted.";
        }
    }
}
