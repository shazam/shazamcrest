package com.github.karsaig.approvalcrest.unsafedenied;

import com.github.karsaig.approvalcrest.ReflectUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Runs in fallback mode with the agent on the command line.
 * <p>
 * Fallback mode exists for suites that want no module bypass at all, so it must ignore the agent
 * the same way it already ignores Unsafe. Opening a package through {@code Instrumentation} is
 * still a bypass, even though it needs no Unsafe to do it.
 * </p>
 */
class FallbackModeWithAgentTest {

    @Test
    void fallbackModeIgnoresTheAgent() {
        assertThat("this execution must actually be in fallback mode",
                ReflectUtil.getMode(), is("fallback"));
        assertThat(ReflectUtil.isInstrumentationAvailable(), is(false));
        assertThat(ReflectUtil.isModuleOpeningAvailable(), is(false));
    }

    @Test
    void noPackageIsOpenedDespiteTheAgentBeingAttached() {
        assertThat(Object.class.getModule().isOpen("java.lang", FallbackModeWithAgentTest.class.getModule()),
                is(false));
    }
}
