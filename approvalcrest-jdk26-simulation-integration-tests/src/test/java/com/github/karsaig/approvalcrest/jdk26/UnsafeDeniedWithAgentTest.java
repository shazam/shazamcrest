package com.github.karsaig.approvalcrest.jdk26;

import com.github.karsaig.approvalcrest.ReflectUtil;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Runs with the sun-misc-unsafe-memory-access=deny flag and the approvalcrest agent attached.
 * Asserts that field-based output survives, which is the whole point of the agent.
 */
class UnsafeDeniedWithAgentTest {

    @Test
    void unsafeIsDeadButTheAgentStillOpensModules() {
        assertThat("the deny flag must be in force, or this class proves nothing",
                ReflectUtil.isUnsafeAvailable(), is(false));
        assertThat("the agent must be attached", ReflectUtil.isInstrumentationAvailable(), is(true));
        assertThat(ReflectUtil.isModuleOpeningAvailable(), is(true));
    }

    @Test
    void openingTargetsTheModuleThatActuallyReflects() {
        // Gson performs the field read, so an open naming only approvalcrest's module would leave
        // it unable to proceed. These are the same module in every ordinary arrangement.
        assertThat(Gson.class.getModule(), is(ReflectUtil.class.getModule()));
    }

    @Test
    void jdkExceptionKeepsItsFieldBasedShape() {
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.jdkException("one"), Jdk26Fixtures.jdkException("two"));

        assertThat(mismatch, containsString("detailMessage"));
        assertThat(mismatch, not(containsString("localizedMessage")));
    }

    /**
     * The case a JDK type cannot cover: this class is in the unnamed module, so nothing triggers an
     * on-demand open for it, and only the proactive opening of java.lang makes the fields it
     * inherits from Throwable readable.
     */
    @Test
    void userDefinedExceptionKeepsItsFieldBasedShape() {
        // Vary the message, because a mismatch description names only the fields that differ — so
        // this is what makes the property carrying the message visible in the first place.
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("bang", "T-1"));

        assertThat(mismatch, containsString("detailMessage"));
        assertThat(mismatch, not(containsString("localizedMessage")));
    }

    @Test
    void userDefinedExceptionStillCoversItsOwnFields() {
        String mismatch = Jdk26Fixtures.describeMismatch(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("boom", "T-2"));

        assertThat(mismatch, containsString("ticket"));
    }

    @Test
    void equalExceptionsStillCompareEqual() {
        assertThat(Jdk26Fixtures.matches(
                Jdk26Fixtures.userException("boom", "T-1"), Jdk26Fixtures.userException("boom", "T-1")), is(true));
    }
}
