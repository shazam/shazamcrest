package com.github.karsaig.approvalcrest.dedup;

import com.github.karsaig.approvalcrest.ApprovedFileType;

import java.io.IOException;

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
public class DedupMojo extends AbstractDedupMojo {

    private static final int DEFAULT_BUCKET_DEPTH = 2;

    /** Number of characters of the hash prefix used as bucket directory name (1-6). Default: 2. */
    @Parameter(property = "fileMatcherSharedDirBucketDepth")
    private Integer bucketDepth;

    /** Short alias of {@code fileMatcherSharedDirBucketDepth}. */
    @Parameter(property = "fmSharedDirBucketDepth")
    private Integer fmSharedDirBucketDepth;

    @Override
    public void execute() throws MojoExecutionException {
        int depth = resolve("fileMatcherSharedDirBucketDepth", bucketDepth, "fmSharedDirBucketDepth", fmSharedDirBucketDepth,
                DEFAULT_BUCKET_DEPTH);
        try {
            ApprovalDeduplicator deduplicator = new ApprovalDeduplicator(
                    workingDirectory(), scanDir(), sharedDir(), depth, dryRun(),
                    ApprovedFileType.parse(types()));
            ApprovalDeduplicator.DeduplicatorResult result = deduplicator.deduplicate();
            getLog().info(result.toString());
        } catch (IOException e) {
            throw new MojoExecutionException("Deduplication failed", e);
        }
    }
}
