package com.github.karsaig.approvalcrest.jdk26;

import com.github.karsaig.approvalcrest.ReflectUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Runs with the sun-misc-unsafe-memory-access=deny flag and no agent. This is what a project that
 * has not opted in gets when the Unsafe memory-access methods are denied. Locks in the documented
 * degradation: getter-based output, and no exception.
 */
class UnsafeDeniedWithoutAgentTest {

    @Test
    void nothingCanOpenAModule() {
        assertThat(ReflectUtil.isUnsafeAvailable(), is(false));
        assertThat(ReflectUtil.isInstrumentationAvailable(), is(false));
        assertThat(ReflectUtil.isModuleOpeningAvailable(), is(false));
    }

    @Test
    void jdkExceptionFallsBackToGetters() {
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.jdkException("one"), Jdk26Fixtures.jdkException("two"));

        assertThat(mismatch, containsString("localizedMessage"));
        assertThat(mismatch, not(containsString("detailMessage")));
    }

    /**
     * Unless a type that merely inherits locked-module fields is treated as locked too, this throws
     * JsonIOException on java.lang.Throwable#detailMessage.
     */
    @Test
    void userDefinedExceptionFallsBackToGettersRatherThanThrowing() {
        // Vary the message, because a mismatch description names only the fields that differ — so
        // this is what makes the property carrying the message visible in the first place.
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("bang", "T-1"));

        assertThat(mismatch, containsString("localizedMessage"));
        assertThat(mismatch, not(containsString("detailMessage")));
    }

    @Test
    void userDefinedExceptionStillCoversItsOwnFields() {
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("boom", "T-2"));

        assertThat(mismatch, containsString("ticket"));
    }

    /**
     * The counterweight to the entry above. Only a superclass that actually contributes a field
     * takes a subclass off the field-based path; a locked superclass whose fields nobody reads
     * must not, or output changes for types that were never affected.
     */
    @Test
    void aSubclassOfALockedClassWithNoReadableFieldsKeepsFieldBasedOutput() {
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.eventSubclass("one"), Jdk26Fixtures.eventSubclass("two"));

        assertThat(mismatch, containsString("label"));
        assertThat("the transient superclass field is not serialised in any mode",
                mismatch, not(containsString("source")));
    }

    @Test
    void equalExceptionsStillCompareEqual() {
        assertThat(Jdk26Fixtures.matches(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("boom", "T-1")), is(true));
    }
}
