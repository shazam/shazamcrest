package com.github.karsaig.approvalcrest.dedup;

import com.github.karsaig.approvalcrest.ApprovedFileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Scans the project's approved files and replaces duplicates with pointer files referencing
 * a single canonical file in the shared approvals directory.
 *
 * <p>Run with: {@code mvn approvalcrest:dedup}
 */
@Mojo(name = "dedup")
public class DedupMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File projectBaseDir;

    /** Shared approvals directory, relative to the project base directory. */
    @Parameter(property = "fileMatcherSharedDir", defaultValue = "src/test/java/shared-approvals")
    private String sharedDir;

    /** Number of characters of the hash prefix used as bucket directory name (1-6). */
    @Parameter(property = "fileMatcherSharedDirBucketDepth", defaultValue = "2")
    private int bucketDepth;

    /** Directory to scan for approved files, relative to the project base directory. */
    @Parameter(defaultValue = "src/test/java")
    private String dir;

    /** When true, print what would be done without writing any files. */
    @Parameter(property = "dryRun", defaultValue = "false")
    private boolean dryRun;

    /**
     * Approved file types to process: {@code json}, {@code content}, {@code json,content} or
     * {@code all}. Files of any other type are left alone, and so are their canonicals.
     */
    @Parameter(property = "fileMatcherSharedTypes", defaultValue = "all")
    private String types;

    @Override
    public void execute() throws MojoExecutionException {
        Path workingDirectory = projectBaseDir.toPath();
        Path scanDirPath = workingDirectory.resolve(dir);
        try {
            ApprovalDeduplicator deduplicator = new ApprovalDeduplicator(
                    workingDirectory, scanDirPath, sharedDir, bucketDepth, dryRun,
                    ApprovedFileType.parse(types));
            ApprovalDeduplicator.DeduplicatorResult result = deduplicator.deduplicate();
            getLog().info(result.toString());
        } catch (IOException e) {
            throw new MojoExecutionException("Deduplication failed", e);
        }
    }
}
