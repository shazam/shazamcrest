package com.github.karsaig.approvalcrest.dedup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;

/**
 * Locates approved files on disk for the dedup tooling.
 *
 * <p>Both {@link ApprovalDeduplicator} and {@link ApprovalReinstate} need the same two scans, and
 * they need to agree about them exactly: a canonical file may only be deleted when nothing points
 * at it any more, so the scan that finds pointers must not be narrower than the scan that finds
 * canonicals.
 */
class ApprovedFileScanner {

    /**
     * Directories never worth walking when looking for pointer references. Without this the
     * project-wide reference scan would descend into build output and version control metadata.
     */
    private static final Set<String> PRUNED_DIRECTORIES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    ".git", ".hg", ".svn", "target", "build", "out", "node_modules", ".gradle", ".idea")));

    private final Map<ApprovedFileType, FileStoreMatcherUtils> utilsByType = new EnumMap<>(ApprovedFileType.class);
    private final Set<ApprovedFileType> selectedTypes;

    ApprovedFileScanner(String sharedApprovalDir, int bucketDepth, Set<ApprovedFileType> selectedTypes) {
        FileMatcherConfig config =
                new FileMatcherConfig(false, false, false, true, sharedApprovalDir, true, bucketDepth);
        for (ApprovedFileType type : ApprovedFileType.values()) {
            utilsByType.put(type, new FileStoreMatcherUtils(type.extension(), config));
        }
        this.selectedTypes = selectedTypes;
    }

    /**
     * Whether this file is one of the types the caller asked to process.
     *
     * <p>Deliberately a separate check rather than something folded into {@link #isApprovedFileName}:
     * the reference lookup must see pointers of <em>every</em> type, or garbage collection would
     * treat the unselected type's canonicals as unreferenced and delete them. Only conversion and
     * deletion filter; callers say so explicitly by calling this.
     *
     * @param file an approved file
     * @return true when its type was selected
     */
    boolean isSelectedType(Path file) {
        ApprovedFileType type = getType(file);
        return type != null && selectedTypes.contains(type);
    }

    /**
     * Returns every approved file under {@code dir}, optionally excluding a subtree.
     */
    List<Path> collectApprovedFiles(Path dir, Path excludeDir) throws IOException {
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        Path normalizedExclude = excludeDir == null ? null : excludeDir.normalize();
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> {
                if (!Files.isRegularFile(p)) {
                    return false;
                }
                if (normalizedExclude != null && p.normalize().startsWith(normalizedExclude)) {
                    return false;
                }
                return isApprovedFileName(p.getFileName().toString());
            }).forEach(result::add);
        }
        return result;
    }

    /**
     * Returns every approved file in the project, skipping build output and VCS metadata. Used to
     * find the pointers that reference canonicals, which is deliberately independent of the scan
     * directory: narrowing the scan must never make a live canonical look unreferenced.
     */
    List<Path> collectApprovedFilesForReferenceLookup(Path workingDirectory) throws IOException {
        if (!Files.exists(workingDirectory)) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(workingDirectory)) {
            stream.filter(p -> !isPruned(workingDirectory, p))
                    .filter(Files::isRegularFile)
                    .filter(p -> isApprovedFileName(p.getFileName().toString()))
                    .forEach(result::add);
        }
        return result;
    }

    private boolean isPruned(Path root, Path candidate) {
        Path relative = root.relativize(candidate);
        for (Path segment : relative) {
            if (PRUNED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    boolean isApprovedFileName(String name) {
        return (name.endsWith("-approved.json") || name.endsWith("-approved.content"))
                && !name.contains("-not-approved.");
    }

    /**
     * Returns the approved file's type, or null if it is neither json nor content.
     *
     * @param file the file to classify
     * @return the type, or null
     */
    ApprovedFileType getType(Path file) {
        return ApprovedFileType.fromFileName(file.getFileName().toString());
    }

    /**
     * @param type the approved file type
     * @return the reader/writer configured for that type's extension
     */
    FileStoreMatcherUtils utilsFor(ApprovedFileType type) {
        return utilsByType.get(type);
    }
}
