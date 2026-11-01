package com.github.karsaig.approvalcrest.dedup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;

/**
 * Path rules shared by the dedup and reinstate tools.
 *
 * <p>Both tools delete files, so both need the same answers to "is this configuration safe?" and
 * "what is still referenced?". Keeping those answers in one place stops the two from drifting.
 */
final class DedupPaths {

    private DedupPaths() {
    }

    /**
     * Renders a path as the project-relative, forward-slash form used inside pointer files.
     */
    static String relativize(Path workingDirectory, Path file) {
        return workingDirectory.relativize(file).toString().replace('\\', '/');
    }

    /**
     * Rejects a configuration where the shared directory contains the scan directory.
     *
     * <p>In that arrangement every approved file in the scan tree is also treated as a canonical,
     * so a run would delete the entire golden-master corpus while reinstating nothing. It is easy
     * to hit by accident because the default shared directory already sits inside the default scan
     * directory, so a single mistyped {@code --shared-dir} is enough.
     */
    static void requireSharedDirDoesNotContainScanDir(Path workingDirectory, Path scanDir, String sharedApprovalDir) {
        Path sharedDirPath = workingDirectory.resolve(sharedApprovalDir).normalize();
        Path normalizedScanDir = scanDir.normalize();
        if (normalizedScanDir.startsWith(sharedDirPath)) {
            throw new IllegalArgumentException(
                    "Refusing to run: the shared approvals directory '" + sharedDirPath
                            + "' contains the scan directory '" + normalizedScanDir
                            + "'. Every approved file in the scan directory would be treated as a canonical"
                            + " and deleted. Point --shared-dir at a directory that does not contain --dir.");
        }
    }

    /**
     * Returns the canonicals referenced by pointer files anywhere in the project.
     *
     * <p>Deliberately independent of the scan directory: a pointer outside the scanned subtree
     * keeps its canonical alive just as much as one inside it, and deleting on the narrower view
     * would leave those pointers resolving to a file that no longer exists.
     */
    static Set<String> collectReferencedCanonicals(ApprovedFileScanner scanner, Path workingDirectory, Path sharedDirPath)
            throws IOException {
        return collectReferencedCanonicals(scanner, workingDirectory, sharedDirPath, Collections.emptySet());
    }

    /**
     * Same as {@link #collectReferencedCanonicals(ApprovedFileScanner, Path, Path)}, but ignores the
     * pointer files in {@code excludedFiles} (absolute, normalised paths).
     *
     * <p>Used by reinstate: the pointers it rewrites no longer keep a canonical alive, but in a dry run
     * they are still on disk. Excluding them by name, rather than subtracting their targets, keeps a
     * canonical that another pointer still uses.
     */
    static Set<String> collectReferencedCanonicals(ApprovedFileScanner scanner, Path workingDirectory, Path sharedDirPath,
                                                   Set<Path> excludedFiles) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path file : scanner.collectApprovedFilesForReferenceLookup(workingDirectory)) {
            if (!excludedFiles.contains(file.toAbsolutePath().normalize())) {
                files.add(file);
            }
        }
        return collectReferencesIn(scanner, workingDirectory, files, sharedDirPath);
    }

    private static Set<String> collectReferencesIn(ApprovedFileScanner scanner, Path workingDirectory,
                                                   Iterable<Path> files, Path sharedDirPath) throws IOException {
        Set<String> referenced = new HashSet<>();
        for (Path file : files) {
            if (file.normalize().startsWith(sharedDirPath)) {
                continue;
            }
            // Deliberately NOT filtered by selected type: a canonical of an unselected type is
            // still kept alive by its pointer, and missing that would delete a live file. The
            // callers also skip unselected canonicals when choosing deletion candidates, so the two
            // guards are redundant on purpose - either alone is sufficient.
            ApprovedFileType ext = scanner.getType(file);
            if (ext == null) {
                continue;
            }
            FileStoreMatcherUtils utils = scanner.utilsFor(ext);
            if (!utils.isPointerFile(file)) {
                continue;
            }
            Optional<String> target = utils.readPointerTarget(file);
            target.ifPresent(t -> referenced.add(t.replace('\\', '/')));
        }
        return referenced;
    }
}
