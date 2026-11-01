package com.github.karsaig.approvalcrest.dedup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;

/**
 * Scans a directory for duplicate approved files and replaces them with pointer files referencing
 * a single canonical file in a shared directory.
 *
 * <p>Files that are already pointer files are not modified; their existing canonical references
 * are tracked to prevent garbage-collection of still-referenced canonicals.
 *
 * <p>After deduplication, any canonical in the shared directory that no pointer references is
 * removed. See {@link #deduplicate()} for why that lookup spans the whole project rather than only
 * the scan directory.
 */
public class ApprovalDeduplicator {

    private final Path workingDirectory;
    private final Path scanDir;
    private final String sharedApprovalDir;
    private final int bucketDepth;
    private final boolean dryRun;
    private final ApprovedFileScanner scanner;

    /**
     * Processes every approved file type.
     */
    public ApprovalDeduplicator(Path workingDirectory, Path scanDir, String sharedApprovalDir, int bucketDepth, boolean dryRun) {
        this(workingDirectory, scanDir, sharedApprovalDir, bucketDepth, dryRun, ApprovedFileType.all());
    }

    /**
     * @param selectedTypes which approved file types to convert. Files of any other type are left
     *                      alone, and so are their canonicals.
     */
    ApprovalDeduplicator(Path workingDirectory, Path scanDir, String sharedApprovalDir, int bucketDepth, boolean dryRun,
                         Set<ApprovedFileType> selectedTypes) {
        DedupPaths.requireSharedDirDoesNotContainScanDir(workingDirectory, scanDir, sharedApprovalDir);
        this.workingDirectory = workingDirectory;
        this.scanDir = scanDir;
        this.sharedApprovalDir = sharedApprovalDir;
        this.bucketDepth = bucketDepth;
        this.dryRun = dryRun;
        this.scanner = new ApprovedFileScanner(sharedApprovalDir, bucketDepth, selectedTypes);
    }

    /**
     * Converts duplicate approved files under the scan directory into pointer files, then removes
     * canonicals that nothing references any more.
     *
     * <p>The scan directory decides which files get <em>converted</em>. Which canonicals are still
     * <em>referenced</em> is a separate question, answered across the whole project: a pointer
     * outside the scan directory keeps its canonical alive exactly as much as one inside it.
     * Answering it on the narrower scan would delete those canonicals and strand the pointers with
     * content that exists nowhere on disk.
     */
    public DeduplicatorResult deduplicate() throws IOException {
        Path sharedDirPath = workingDirectory.resolve(sharedApprovalDir).normalize();

        List<Path> allApprovedFiles = scanner.collectApprovedFiles(scanDir, sharedDirPath);

        Map<String, List<ApprovedFileEntry>> contentGroups = new LinkedHashMap<>();
        for (Path file : allApprovedFiles) {
            ApprovedFileType ext = scanner.getType(file);
            if (ext == null || !scanner.isSelectedType(file)) {
                continue;
            }
            FileStoreMatcherUtils utils = scanner.utilsFor(ext);
            if (utils.isPointerFile(file)) {
                continue;
            }
            String content = utils.readFile(file, workingDirectory);
            String key = utils.computeContentKey(content);
            String comment = utils.readComment(file);
            contentGroups.computeIfAbsent(key + "." + ext.extension(), k -> new ArrayList<>())
                    .add(new ApprovedFileEntry(file, key, content, comment, ext));
        }

        int pointersWritten = 0;
        int canonicalsCreated = 0;
        Set<String> canonicalsUsedThisRun = new HashSet<>();

        for (Map.Entry<String, List<ApprovedFileEntry>> entry : contentGroups.entrySet()) {
            List<ApprovedFileEntry> group = entry.getValue();
            ApprovedFileEntry first = group.get(0);
            FileStoreMatcherUtils utils = scanner.utilsFor(first.extension);

            Optional<String> existingCanonical = utils.findMatchingCanonical(
                    first.content, workingDirectory, sharedApprovalDir, bucketDepth);

            String canonicalRelative;
            if (existingCanonical.isPresent()) {
                canonicalRelative = existingCanonical.get();
            } else if (group.size() >= 2) {
                // Derive the path the same way in both modes so a dry-run preview cannot describe
                // a different file from the one a real run would write.
                canonicalRelative = utils.canonicalRelativePath(
                        first.content, workingDirectory, sharedApprovalDir, bucketDepth);
                if (!dryRun) {
                    utils.writeCanonical(first.content, "shared", workingDirectory, sharedApprovalDir, bucketDepth);
                }
                canonicalsCreated++;
            } else {
                // Single unique file with no existing canonical — nothing to do
                continue;
            }

            canonicalsUsedThisRun.add(canonicalRelative);

            for (ApprovedFileEntry fileEntry : group) {
                if (!dryRun) {
                    scanner.utilsFor(fileEntry.extension)
                            .writePointerFile(fileEntry.path, fileEntry.comment, canonicalRelative);
                }
                pointersWritten++;
            }
        }

        int orphansRemoved = collectGarbage(sharedDirPath, canonicalsUsedThisRun);

        return new DeduplicatorResult(pointersWritten, canonicalsCreated, orphansRemoved,
                isGarbageCollectionSkipped(), dryRun);
    }

    /**
     * Deletes canonicals that no pointer anywhere in the project references.
     *
     * <p>Skipped when the shared directory lies outside the project, because the pointers that
     * reference it cannot be enumerated and no deletion could be shown to be safe.
     */
    private int collectGarbage(Path sharedDirPath, Set<String> canonicalsUsedThisRun) throws IOException {
        if (!Files.exists(sharedDirPath) || isGarbageCollectionSkipped()) {
            return 0;
        }

        Set<String> referenced = new HashSet<>(canonicalsUsedThisRun);
        referenced.addAll(DedupPaths.collectReferencedCanonicals(scanner, workingDirectory, sharedDirPath));

        int orphansRemoved = 0;
        for (Path canonical : scanner.collectApprovedFiles(sharedDirPath, null)) {
            // Only canonicals of the selected types are candidates; a json-only run must leave
            // content canonicals alone even when nothing currently points at them. This and the
            // unfiltered reference lookup in DedupPaths are independently sufficient - verified by
            // mutation: removing either alone still protects the other type, removing both deletes
            // it. DedupTypeSelectionTest.jsonOnlyRunDoesNotCollectContentCanonicals covers that.
            if (!scanner.isSelectedType(canonical)) {
                continue;
            }
            if (!referenced.contains(DedupPaths.relativize(workingDirectory, canonical))) {
                if (!dryRun) {
                    Files.delete(canonical);
                }
                orphansRemoved++;
            }
        }
        return orphansRemoved;
    }

    /**
     * True when the shared directory sits outside the project, in which case garbage collection is
     * skipped because the pointers referencing it cannot be found.
     */
    public boolean isGarbageCollectionSkipped() {
        return !workingDirectory.resolve(sharedApprovalDir).normalize()
                .startsWith(workingDirectory.normalize());
    }

    static class ApprovedFileEntry {
        final Path path;
        final String key;
        final String content;
        final String comment;
        final ApprovedFileType extension;

        ApprovedFileEntry(Path path, String key, String content, String comment, ApprovedFileType extension) {
            this.path = path;
            this.key = key;
            this.content = content;
            this.comment = comment;
            this.extension = extension;
        }
    }

    public static class DeduplicatorResult {
        private final int pointersWritten;
        private final int canonicalsCreated;
        private final int orphansRemoved;
        private final boolean garbageCollectionSkipped;
        private final boolean dryRun;

        static final String DRY_RUN_PREFIX = "Dry run, no files changed. ";

        public DeduplicatorResult(int pointersWritten, int canonicalsCreated, int orphansRemoved) {
            this(pointersWritten, canonicalsCreated, orphansRemoved, false);
        }

        public DeduplicatorResult(int pointersWritten, int canonicalsCreated, int orphansRemoved,
                                  boolean garbageCollectionSkipped) {
            this(pointersWritten, canonicalsCreated, orphansRemoved, garbageCollectionSkipped, false);
        }

        public DeduplicatorResult(int pointersWritten, int canonicalsCreated, int orphansRemoved,
                                  boolean garbageCollectionSkipped, boolean dryRun) {
            this.pointersWritten = pointersWritten;
            this.canonicalsCreated = canonicalsCreated;
            this.orphansRemoved = orphansRemoved;
            this.garbageCollectionSkipped = garbageCollectionSkipped;
            this.dryRun = dryRun;
        }

        public int getPointersWritten() {
            return pointersWritten;
        }

        public int getCanonicalsCreated() {
            return canonicalsCreated;
        }

        public int getOrphansRemoved() {
            return orphansRemoved;
        }

        public boolean isGarbageCollectionSkipped() {
            return garbageCollectionSkipped;
        }

        @Override
        public String toString() {
            String result = (dryRun ? DRY_RUN_PREFIX : "")
                    + "Deduplication complete: " + canonicalsCreated + " canonical(s) created, "
                    + pointersWritten + " pointer(s) written, "
                    + orphansRemoved + " orphaned canonical(s) removed.";
            if (garbageCollectionSkipped) {
                result += " Skipped removing orphaned canonicals: the shared directory is outside the"
                        + " project, so the pointers referencing it could not be found.";
            }
            return result;
        }
    }
}
