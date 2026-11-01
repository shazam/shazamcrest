package com.github.karsaig.approvalcrest;

import static com.github.karsaig.approvalcrest.EnvVarReader.getBooleanProperties;
import static com.github.karsaig.approvalcrest.EnvVarReader.getIntProperties;
import static com.github.karsaig.approvalcrest.EnvVarReader.getStringProperties;

import java.util.Set;

public class FileMatcherConfig {

    private static final String UPDATE_IN_PLACE_OLD_NAME = "jsonMatcherUpdateInPlace";
    private static final String UPDATE_IN_PLACE_NAME = "fileMatcherUpdateInPlace";
    private static final String UPDATE_IN_PLACE_ALIAS = "fMUInPlace";
    private static final String PASS_ON_CREATE = "fileMatcherPassOnCreate";
    private static final String PASS_ON_CREATE_ALIAS = "fMPOnCreate";
    private static final String SORT_INPUT_FILE = "sortInputFile";
    private static final String SORT_INPUT_FILE_ALIAS = "sIFile";
    private static final String SKIP_CUSTOM_MATCHERS_ON_UPDATE = "fileMatcherSkipCustomMatchersOnUpdate";
    private static final String SKIP_CUSTOM_MATCHERS_ON_UPDATE_ALIAS = "fMSCMOUpdate";
    private static final String STRICT_FILE_MATCHING = "fileMatcherStrictFileMatching";
    private static final String STRICT_FILE_MATCHING_ALIAS = "fMStrictMatching";
    private static final String SHARED_DIR_NAME = "fileMatcherSharedDir";
    private static final String SHARED_DIR_ALIAS = "fmSharedDir";
    private static final String SHARED_ENABLED_NAME = "fileMatcherSharedEnabled";
    private static final String SHARED_ENABLED_ALIAS = "fmSharedEnabled";
    private static final String SHARED_BUCKET_DEPTH_NAME = "fileMatcherSharedDirBucketDepth";
    private static final String SHARED_BUCKET_DEPTH_ALIAS = "fmSharedDirBucketDepth";

    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";
    private static final int DEFAULT_SHARED_BUCKET_DEPTH = 2;

    /**
     * Bucket depth is used as {@code contentKey.substring(0, depth)}, so it has to stay within the
     * documented range. Anything outside it would either fail with an opaque
     * {@link StringIndexOutOfBoundsException} deep inside the matcher, or fan the shared directory
     * out over an absurd number of buckets.
     */
    public static final int MIN_SHARED_BUCKET_DEPTH = 1;
    public static final int MAX_SHARED_BUCKET_DEPTH = 6;

    private final boolean overwriteInPlaceEnabled;
    private final boolean passOnCreateEnabled;
    private final boolean sortInputFile;
    private final boolean strictFileMatching;
    private final boolean skipCustomMatchersOnUpdate;
    private final String sharedApprovalDirectory;
    private final Set<ApprovedFileType> sharedTypes;
    private final int sharedBucketDepth;

    public FileMatcherConfig() {
        overwriteInPlaceEnabled = getBooleanProperties(null, UPDATE_IN_PLACE_OLD_NAME, UPDATE_IN_PLACE_NAME, UPDATE_IN_PLACE_ALIAS);
        passOnCreateEnabled = getBooleanProperties(null, PASS_ON_CREATE, PASS_ON_CREATE_ALIAS);
        sortInputFile = getBooleanProperties(null, SORT_INPUT_FILE, SORT_INPUT_FILE_ALIAS);
        strictFileMatching = getBooleanProperties("true", STRICT_FILE_MATCHING, STRICT_FILE_MATCHING_ALIAS);
        skipCustomMatchersOnUpdate = getBooleanProperties(null, SKIP_CUSTOM_MATCHERS_ON_UPDATE, SKIP_CUSTOM_MATCHERS_ON_UPDATE_ALIAS);
        sharedApprovalDirectory = getStringProperties(DEFAULT_SHARED_DIR, SHARED_DIR_NAME, SHARED_DIR_ALIAS);
        sharedTypes = ApprovedFileType.parse(getStringProperties("none", SHARED_ENABLED_NAME, SHARED_ENABLED_ALIAS));
        sharedBucketDepth = validateBucketDepth(getIntProperties(DEFAULT_SHARED_BUCKET_DEPTH, SHARED_BUCKET_DEPTH_NAME, SHARED_BUCKET_DEPTH_ALIAS));
    }

    private static int validateBucketDepth(int bucketDepth) {
        if (bucketDepth < MIN_SHARED_BUCKET_DEPTH || bucketDepth > MAX_SHARED_BUCKET_DEPTH) {
            throw new IllegalStateException("Invalid value for property " + SHARED_BUCKET_DEPTH_NAME
                    + " (alias " + SHARED_BUCKET_DEPTH_ALIAS + "): " + bucketDepth
                    + ". Must be between " + MIN_SHARED_BUCKET_DEPTH + " and " + MAX_SHARED_BUCKET_DEPTH + ".");
        }
        return bucketDepth;
    }

    public FileMatcherConfig(boolean overwriteInPlaceEnabled, boolean passOnCreateEnabled, boolean sortInputFile, boolean strictMatching) {
        this(overwriteInPlaceEnabled, passOnCreateEnabled, sortInputFile, strictMatching, DEFAULT_SHARED_DIR, false, DEFAULT_SHARED_BUCKET_DEPTH);
    }

    /**
     * @param sharedEnabled true enables the shared-approval integration for every file type; use
     *                      {@link #FileMatcherConfig(boolean, boolean, boolean, boolean, String, Set, int)}
     *                      to enable it for a subset
     */
    public FileMatcherConfig(boolean overwriteInPlaceEnabled, boolean passOnCreateEnabled, boolean sortInputFile, boolean strictMatching, String sharedApprovalDirectory, boolean sharedEnabled, int sharedBucketDepth) {
        this(overwriteInPlaceEnabled, passOnCreateEnabled, sortInputFile, strictMatching,
                sharedApprovalDirectory, sharedEnabled ? ApprovedFileType.all() : ApprovedFileType.none(), sharedBucketDepth, false);
    }

    /**
     * @param sharedTypes the file types the shared-approval integration applies to; empty disables it
     */
    public FileMatcherConfig(boolean overwriteInPlaceEnabled, boolean passOnCreateEnabled, boolean sortInputFile, boolean strictMatching, String sharedApprovalDirectory, Set<ApprovedFileType> sharedTypes, int sharedBucketDepth) {
        this(overwriteInPlaceEnabled, passOnCreateEnabled, sortInputFile, strictMatching,
                sharedApprovalDirectory, sharedTypes, sharedBucketDepth, false);
    }

    /**
     * Private, so that the only public route to the new flag is {@link #withSkipCustomMatchersOnUpdate(boolean)}.
     * Every constructor here is positional and already four booleans wide; adding a fifth to the published set
     * would make the next one worse again.
     *
     * @param skipCustomMatchersOnUpdate true defers custom matchers while an in-place update is rewriting
     *                                   approved files; inert unless {@code overwriteInPlaceEnabled}
     */
    private FileMatcherConfig(boolean overwriteInPlaceEnabled, boolean passOnCreateEnabled, boolean sortInputFile, boolean strictMatching, String sharedApprovalDirectory, Set<ApprovedFileType> sharedTypes, int sharedBucketDepth, boolean skipCustomMatchersOnUpdate) {
        this.overwriteInPlaceEnabled = overwriteInPlaceEnabled;
        this.passOnCreateEnabled = passOnCreateEnabled;
        this.sortInputFile = sortInputFile;
        this.strictFileMatching = strictMatching;
        this.sharedApprovalDirectory = sharedApprovalDirectory;
        this.sharedTypes = sharedTypes;
        this.sharedBucketDepth = validateBucketDepth(sharedBucketDepth);
        this.skipCustomMatchersOnUpdate = skipCustomMatchersOnUpdate;
    }

    /**
     * A copy of this configuration with the custom-matcher skip set as given. Everything else is carried over.
     */
    public FileMatcherConfig withSkipCustomMatchersOnUpdate(boolean skipCustomMatchersOnUpdate) {
        return new FileMatcherConfig(overwriteInPlaceEnabled, passOnCreateEnabled, sortInputFile,
                strictFileMatching, sharedApprovalDirectory, sharedTypes, sharedBucketDepth, skipCustomMatchersOnUpdate);
    }

    public boolean isOverwriteInPlaceEnabled() {
        return overwriteInPlaceEnabled;
    }

    public boolean isPassOnCreateEnabled() {
        return passOnCreateEnabled;
    }

    public boolean isSortInputFile() {
        return sortInputFile;
    }

    public boolean isStrictFileMatching() {
        return strictFileMatching;
    }

    /**
     * The raw {@code fileMatcherSkipCustomMatchersOnUpdate} setting. Callers deciding whether to evaluate custom
     * matchers want {@link #isCustomMatcherEvaluationSkipped()} instead -- this one says nothing about whether an
     * update is in progress.
     */
    public boolean isSkipCustomMatchersOnUpdateEnabled() {
        return skipCustomMatchersOnUpdate;
    }

    /**
     * Whether custom matchers should be left unevaluated for this run.
     *
     * <p>True only while an in-place update is rewriting approved files <em>and</em> the skip is asked for. A
     * regeneration run exists to produce files the next verification run will match, and a custom matcher that
     * fails stops it before most files are rewritten -- while a brand new approved file is already written
     * without consulting them at all. Both halves of the condition live here so that "inert unless updating"
     * is a property of the configuration rather than something each caller has to remember.
     *
     * <p>It applies to the whole run, not to the files that turn out to need rewriting: a test whose approved
     * file is already current has its custom matchers skipped too, and so passes even where one would have
     * failed. That is the cost of asking for it, and the reason it is off by default -- an update run is for
     * producing files, and a verification run is what decides whether they are right.
     *
     * <p>It suppresses the assertion a registration carries, never the field removal it drives: the file a
     * regeneration writes has to be the one a verification run would compare against.
     */
    public boolean isCustomMatcherEvaluationSkipped() {
        return overwriteInPlaceEnabled && skipCustomMatchersOnUpdate;
    }

    public String getSharedApprovalDirectory() {
        return sharedApprovalDirectory;
    }

    /**
     * @return true when the shared-approval integration is active for at least one file type
     */
    public boolean isSharedEnabled() {
        return !sharedTypes.isEmpty();
    }

    /**
     * Whether new and updated approved files of this type should point at a matching canonical
     * instead of carrying their own copy of the content.
     *
     * @param type the approved file type the matcher writes
     * @return true when the shared-approval integration is active for that type
     */
    public boolean isSharedEnabledFor(ApprovedFileType type) {
        return sharedTypes.contains(type);
    }

    /**
     * @return the file types the shared-approval integration is active for
     */
    public Set<ApprovedFileType> getSharedTypes() {
        return sharedTypes;
    }

    public int getSharedBucketDepth() {
        return sharedBucketDepth;
    }
}
