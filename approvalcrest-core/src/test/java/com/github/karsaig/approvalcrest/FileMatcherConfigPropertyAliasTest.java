package com.github.karsaig.approvalcrest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that each property alias for {@link FileMatcherConfig} is resolved
 * identically to its canonical system property name, and that setting both to
 * conflicting values throws an {@link IllegalStateException}.
 */
public class FileMatcherConfigPropertyAliasTest {

    @BeforeEach
    void clearAllProperties() {
        System.clearProperty("jsonMatcherUpdateInPlace");
        System.clearProperty("fileMatcherUpdateInPlace");
        System.clearProperty("fMUInPlace");
        System.clearProperty("fileMatcherPassOnCreate");
        System.clearProperty("fMPOnCreate");
        System.clearProperty("sortInputFile");
        System.clearProperty("sIFile");
        System.clearProperty("fileMatcherSkipCustomMatchersOnUpdate");
        System.clearProperty("fMSCMOUpdate");
        System.clearProperty("fileMatcherStrictFileMatching");
        System.clearProperty("fMStrictMatching");
        System.clearProperty("fileMatcherSharedDir");
        System.clearProperty("fmSharedDir");
        System.clearProperty("fileMatcherSharedEnabled");
        System.clearProperty("fmSharedEnabled");
        System.clearProperty("fileMatcherSharedDirBucketDepth");
        System.clearProperty("fmSharedDirBucketDepth");
    }

    @AfterEach
    void restoreAllProperties() {
        clearAllProperties();
    }

    @Test
    public void fMUInPlaceAliasEnablesOverwriteInPlace() {
        System.setProperty("fMUInPlace", "true");
        assertTrue(new FileMatcherConfig().isOverwriteInPlaceEnabled());
    }

    @Test
    public void fMPOnCreateAliasEnablesPassOnCreate() {
        System.setProperty("fMPOnCreate", "true");
        assertTrue(new FileMatcherConfig().isPassOnCreateEnabled());
    }

    @Test
    public void sIFileAliasEnablesSortInputFile() {
        System.setProperty("sIFile", "true");
        assertTrue(new FileMatcherConfig().isSortInputFile());
    }

    @Test
    public void fMStrictMatchingAliasConfiguresStrictFileMatching() {
        System.setProperty("fMStrictMatching", "false");
        assertFalse(new FileMatcherConfig().isStrictFileMatching());
    }

    @Test
    public void fileMatcherSkipCustomMatchersOnUpdateCanonicalNameIsRead() {
        System.setProperty("fileMatcherSkipCustomMatchersOnUpdate", "true");
        assertTrue(new FileMatcherConfig().isSkipCustomMatchersOnUpdateEnabled());
    }

    @Test
    public void fMSCMOUpdateAliasIsRead() {
        System.setProperty("fMSCMOUpdate", "true");
        assertTrue(new FileMatcherConfig().isSkipCustomMatchersOnUpdateEnabled());
    }

    @Test
    public void skipCustomMatchersOnUpdateDefaultsToFalse() {
        assertFalse(new FileMatcherConfig().isSkipCustomMatchersOnUpdateEnabled());
    }

    @Test
    public void skipCustomMatchersOnUpdateAcceptsCanonicalAndAliasAgreeing() {
        System.setProperty("fileMatcherSkipCustomMatchersOnUpdate", "true");
        System.setProperty("fMSCMOUpdate", "true");
        assertTrue(new FileMatcherConfig().isSkipCustomMatchersOnUpdateEnabled());
    }

    @Test
    public void skipCustomMatchersOnUpdateRejectsCanonicalAndAliasDisagreeing() {
        System.setProperty("fileMatcherSkipCustomMatchersOnUpdate", "true");
        System.setProperty("fMSCMOUpdate", "false");
        assertThrows(IllegalStateException.class, FileMatcherConfig::new);
    }

    /**
     * The property on its own says nothing about whether custom matchers are skipped -- an update has to be
     * running as well. Keeping both halves in the configuration is what makes that testable without a filesystem.
     */
    @Test
    public void customMatcherEvaluationIsSkippedOnlyWhileUpdatingInPlace() {
        System.setProperty("fileMatcherSkipCustomMatchersOnUpdate", "true");
        assertFalse(new FileMatcherConfig().isCustomMatcherEvaluationSkipped());

        System.setProperty("fileMatcherUpdateInPlace", "true");
        assertTrue(new FileMatcherConfig().isCustomMatcherEvaluationSkipped());
    }

    @Test
    public void customMatcherEvaluationIsNotSkippedWhenUpdatingWithoutTheFlag() {
        System.setProperty("fileMatcherUpdateInPlace", "true");
        assertFalse(new FileMatcherConfig().isCustomMatcherEvaluationSkipped());
    }

    @Test
    public void fmSharedDirAliasConfiguresSharedApprovalDirectory() {
        System.setProperty("fmSharedDir", "custom/shared/dir");
        assertEquals("custom/shared/dir", new FileMatcherConfig().getSharedApprovalDirectory());
    }

    @Test
    public void fileMatcherSharedDirCanonicalNameConfiguresSharedApprovalDirectory() {
        System.setProperty("fileMatcherSharedDir", "another/path");
        assertEquals("another/path", new FileMatcherConfig().getSharedApprovalDirectory());
    }

    @Test
    public void sharedApprovalDirectoryDefaultsToStandardPath() {
        assertEquals("src/test/java/shared-approvals", new FileMatcherConfig().getSharedApprovalDirectory());
    }

    @Test
    public void fmSharedEnabledAliasEnablesShared() {
        System.setProperty("fmSharedEnabled", "true");
        assertTrue(new FileMatcherConfig().isSharedEnabled());
    }

    @Test
    public void sharedEnabledDefaultsFalse() {
        assertFalse(new FileMatcherConfig().isSharedEnabled());
    }

    /** true and false keep their meaning, so an existing configuration behaves as before. */
    @Test
    public void sharedEnabledTrueAppliesToEveryType() {
        System.setProperty("fileMatcherSharedEnabled", "true");
        FileMatcherConfig config = new FileMatcherConfig();

        assertTrue(config.isSharedEnabled());
        assertTrue(config.isSharedEnabledFor(ApprovedFileType.JSON));
        assertTrue(config.isSharedEnabledFor(ApprovedFileType.CONTENT));
    }

    @Test
    public void sharedEnabledFalseAppliesToNoType() {
        System.setProperty("fileMatcherSharedEnabled", "false");
        FileMatcherConfig config = new FileMatcherConfig();

        assertFalse(config.isSharedEnabled());
        assertFalse(config.isSharedEnabledFor(ApprovedFileType.JSON));
        assertFalse(config.isSharedEnabledFor(ApprovedFileType.CONTENT));
    }

    @Test
    public void sharedEnabledCanSelectJsonOnly() {
        System.setProperty("fileMatcherSharedEnabled", "json");
        FileMatcherConfig config = new FileMatcherConfig();

        assertTrue(config.isSharedEnabled(), "enabled for at least one type");
        assertTrue(config.isSharedEnabledFor(ApprovedFileType.JSON));
        assertFalse(config.isSharedEnabledFor(ApprovedFileType.CONTENT));
    }

    @Test
    public void sharedEnabledCanSelectContentOnly() {
        System.setProperty("fmSharedEnabled", "content");
        FileMatcherConfig config = new FileMatcherConfig();

        assertFalse(config.isSharedEnabledFor(ApprovedFileType.JSON));
        assertTrue(config.isSharedEnabledFor(ApprovedFileType.CONTENT));
    }

    @Test
    public void sharedEnabledAcceptsAListAndTheAllKeyword() {
        System.setProperty("fileMatcherSharedEnabled", "json,content");
        assertEquals(ApprovedFileType.all(), new FileMatcherConfig().getSharedTypes());

        System.setProperty("fileMatcherSharedEnabled", "all");
        assertEquals(ApprovedFileType.all(), new FileMatcherConfig().getSharedTypes());
    }

    @Test
    public void sharedEnabledRejectsAnUnknownValue() {
        System.setProperty("fileMatcherSharedEnabled", "xml");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, FileMatcherConfig::new);
        assertTrue(e.getMessage().contains("xml"), "names the offending value: " + e.getMessage());
        assertTrue(e.getMessage().contains("json"), "lists the valid values: " + e.getMessage());
    }

    @Test
    public void fmSharedDirBucketDepthAliasConfiguresBucketDepth() {
        System.setProperty("fmSharedDirBucketDepth", "3");
        assertEquals(3, new FileMatcherConfig().getSharedBucketDepth());
    }

    @Test
    public void sharedBucketDepthDefaultsToTwo() {
        assertEquals(2, new FileMatcherConfig().getSharedBucketDepth());
    }

    @Test
    public void conflictingSharedDirThrowsIllegalStateException() {
        System.setProperty("fileMatcherSharedDir", "path/one");
        System.setProperty("fmSharedDir", "path/two");
        assertThrows(IllegalStateException.class, FileMatcherConfig::new);
    }

    @Test
    public void conflictingCanonicalAndAliasThrowsIllegalStateException() {
        System.setProperty("fileMatcherUpdateInPlace", "true");
        System.setProperty("fMUInPlace", "false");
        assertThrows(IllegalStateException.class, FileMatcherConfig::new);
    }

    @Test
    public void canonicalAndAliasAgreementDoesNotThrow() {
        System.setProperty("fileMatcherUpdateInPlace", "true");
        System.setProperty("fMUInPlace", "true");
        assertTrue(new FileMatcherConfig().isOverwriteInPlaceEnabled());
    }
}
