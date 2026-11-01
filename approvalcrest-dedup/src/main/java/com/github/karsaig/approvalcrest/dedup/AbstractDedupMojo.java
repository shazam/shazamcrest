package com.github.karsaig.approvalcrest.dedup;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The parameters that {@code approvalcrest:dedup} and {@code approvalcrest:reinstate} share. Each
 * system property has the same long name and short alias as in the tests, and the same rule: a long
 * name and its alias with different values stop the goal. The alias parameters are not read-only,
 * because Maven warns about a read-only parameter whenever its property is set.
 */
abstract class AbstractDedupMojo extends AbstractMojo {

    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";
    private static final String DEFAULT_SOURCE_ROOT = "src/test/java";

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File projectBaseDir;

    /**
     * Shared approvals directory, relative to the project base directory. Default:
     * {@code src/test/java/shared-approvals}.
     */
    @Parameter(property = "fileMatcherSharedDir")
    private String sharedDir;

    /** Short alias of {@code fileMatcherSharedDir}. */
    @Parameter(property = "fmSharedDir")
    private String fmSharedDir;

    /**
     * Directory to scan for approved files, relative to the project base directory. When not set,
     * the goal scans the directory that {@code fileMatcherSourceRoot} names, else {@code src/test/java}.
     */
    @Parameter
    private String dir;

    /** The test source root of the approved files, as the tests read it. Used when {@code dir} is not set. */
    @Parameter(property = "fileMatcherSourceRoot")
    private String fileMatcherSourceRoot;

    /** Short alias of {@code fileMatcherSourceRoot}. */
    @Parameter(property = "fmSourceRoot")
    private String fmSourceRoot;

    /** When true, print what the goal would do, and write and delete nothing. */
    @Parameter(property = "dryRun", defaultValue = "false")
    private boolean dryRun;

    /**
     * Approved file types to process: {@code json}, {@code content}, {@code json,content} or
     * {@code all}. Files of any other type are left alone, and so are their canonicals.
     */
    @Parameter(property = "fileMatcherSharedTypes", defaultValue = "all")
    private String types;

    protected Path workingDirectory() {
        return projectBaseDir.toPath();
    }

    protected String sharedDir() throws MojoExecutionException {
        return resolve("fileMatcherSharedDir", sharedDir, "fmSharedDir", fmSharedDir, DEFAULT_SHARED_DIR);
    }

    protected Path scanDir() throws MojoExecutionException {
        if (dir != null) {
            return workingDirectory().resolve(dir);
        }
        // Backslashes become separators, as in the tests, so that both find the same directory on every OS.
        String sourceRoot = resolve("fileMatcherSourceRoot", fileMatcherSourceRoot, "fmSourceRoot", fmSourceRoot, DEFAULT_SOURCE_ROOT);
        return workingDirectory().resolve(sourceRoot.replace('\\', '/'));
    }

    protected boolean dryRun() {
        return dryRun;
    }

    protected String types() {
        return types;
    }

    static <T> T resolve(String name, T value, String alias, T aliasValue, T defaultValue) throws MojoExecutionException {
        if (value != null && aliasValue != null && !value.equals(aliasValue)) {
            throw new MojoExecutionException(
                    "Ambiguous configuration: conflicting values set for properties: [" + name + ", " + alias + "]");
        }
        if (value != null) {
            return value;
        }
        return aliasValue != null ? aliasValue : defaultValue;
    }
}
