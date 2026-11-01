package com.github.karsaig.approvalcrest.dedup;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.github.karsaig.approvalcrest.ApprovedFileType;
import com.github.karsaig.approvalcrest.EnvVarReader;
import com.github.karsaig.approvalcrest.FileMatcherConfig;

/**
 * CLI entry point for the approvalcrest deduplication tool.
 *
 * <p>Usage:
 * <pre>
 *   java -jar approvalcrest-dedup-VERSION-standalone.jar [options]
 *
 *   Options:
 *     --dir &lt;path&gt;          Scan directory (default: fileMatcherSourceRoot, else src/test/java)
 *     --shared-dir &lt;path&gt;   Shared approvals directory (default: src/test/java/shared-approvals)
 *     --bucket-depth &lt;n&gt;    Bucket prefix depth 1-6 (default: 2)
 *     --types &lt;list&gt;        Approved file types to process: json, content, json,content or all
 *                           (default: all)
 *     --dry-run             Print what would be done without writing files (applies to --reinstate too)
 *     --reinstate           Replace all pointers with standalone content and clear the shared dir
 * </pre>
 *
 * <p>Without the option, {@code --dir}, {@code --shared-dir} and {@code --bucket-depth} read the system
 * properties that the tests read: {@code fileMatcherSourceRoot}, {@code fileMatcherSharedDir} and
 * {@code fileMatcherSharedDirBucketDepth}, or their aliases {@code fmSourceRoot}, {@code fmSharedDir} and
 * {@code fmSharedDirBucketDepth}. The other options read no system property.
 */
public class DedupCli {

    public static void main(String[] args) throws IOException {
        try {
            run(args, Paths.get("").toAbsolutePath(), System.out);
        } catch (IllegalArgumentException e) {
            // Misconfiguration, not a defect: report it plainly and exit non-zero rather than
            // dumping a stack trace at someone who mistyped a flag.
            System.err.println("approvalcrest-dedup: " + e.getMessage());
            System.exit(2);
        }
    }

    static void run(String[] args, Path workingDirectory, PrintStream out) throws IOException {
        String dir = null;
        String sharedDir = null;
        Integer bucketDepth = null;
        boolean dryRun = false;
        boolean reinstate = false;
        String types = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--dir".equals(arg) && i + 1 < args.length) {
                dir = args[++i];
            } else if ("--shared-dir".equals(arg) && i + 1 < args.length) {
                sharedDir = args[++i];
            } else if ("--bucket-depth".equals(arg) && i + 1 < args.length) {
                bucketDepth = Integer.parseInt(args[++i]);
            } else if ("--types".equals(arg) && i + 1 < args.length) {
                types = args[++i];
            } else if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if ("--reinstate".equals(arg)) {
                reinstate = true;
            }
        }

        FileMatcherConfig config;
        String sourceRoot;
        try {
            config = new FileMatcherConfig();
            sourceRoot = EnvVarReader.getStringProperties("src/test/java", "fileMatcherSourceRoot", "fmSourceRoot");
        } catch (IllegalStateException e) {
            // A conflicting or invalid system property is misconfiguration too, so main reports it plainly.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (sharedDir == null) {
            sharedDir = config.getSharedApprovalDirectory();
        }
        if (bucketDepth == null) {
            bucketDepth = config.getSharedBucketDepth();
        }

        // Backslashes in the property become separators, as in the tests, so that both find the same directory.
        Path scanDirPath = workingDirectory.resolve(dir != null ? dir : sourceRoot.replace('\\', '/'));
        Set<ApprovedFileType> selectedTypes = ApprovedFileType.parse(types);

        if (reinstate) {
            ApprovalReinstate reinstater =
                    new ApprovalReinstate(workingDirectory, scanDirPath, sharedDir, dryRun, selectedTypes);
            ApprovalReinstate.ReinstateResult result = reinstater.reinstate();
            out.println(result);
        } else {
            ApprovalDeduplicator deduplicator = new ApprovalDeduplicator(
                    workingDirectory, scanDirPath, sharedDir, bucketDepth, dryRun, selectedTypes);
            ApprovalDeduplicator.DeduplicatorResult result = deduplicator.deduplicate();
            out.println(result);
        }
    }
}
