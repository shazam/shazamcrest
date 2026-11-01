package com.github.karsaig.approvalcrest.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * The stack-trace route and the TestInfo route must describe the running test identically, because
 * that description is what an approved file's path is derived from. If they disagree, adding a
 * {@code TestInfo} parameter to an existing test - an unremarkable refactor - silently orphans its
 * approved file.
 *
 * <p>Equality is asserted field by field rather than with {@code sameBeanAs} so that a failure names
 * the field that diverged.
 */
public class RouteEquivalenceTest {

    private TestInfo capturedInBeforeEach;

    @BeforeEach
    void captureTestInfo(TestInfo testInfo) {
        this.capturedInBeforeEach = testInfo;
    }

    private static void assertSameIdentity(JunitJupiterTestMeta stackRoute, Junit5InfoBasedTestMeta infoRoute) {
        assertEquals(stackRoute.testClassName(), infoRoute.testClassName(), "testClassName");
        assertEquals(stackRoute.testMethodName(), infoRoute.testMethodName(), "testMethodName");
        assertEquals(stackRoute.getTestClassPath(), infoRoute.getTestClassPath(), "testClassPath");
        assertEquals(stackRoute.workingDirectory(), infoRoute.workingDirectory(), "workingDirectory");
    }

    @Test
    public void publicMethodResolvesIdenticallyByBothRoutes(TestInfo testInfo) {
        assertSameIdentity(new JunitJupiterTestMeta(), new Junit5InfoBasedTestMeta(testInfo));
    }

    /** Package-private test methods are legal in Jupiter and must behave like public ones. */
    @Test
    void packagePrivateMethodResolvesIdenticallyByBothRoutes(TestInfo testInfo) {
        assertSameIdentity(new JunitJupiterTestMeta(), new Junit5InfoBasedTestMeta(testInfo));
    }

    /**
     * A display name is presentation only. If it leaked into the identity, adding one would move the
     * approved file.
     */
    @Test
    @DisplayName("a display name that is nothing like the method name")
    void displayNameDoesNotAffectIdentity(TestInfo testInfo) {
        Junit5InfoBasedTestMeta infoRoute = new Junit5InfoBasedTestMeta(testInfo);

        assertSameIdentity(new JunitJupiterTestMeta(), infoRoute);
        assertEquals("displayNameDoesNotAffectIdentity", infoRoute.testMethodName(),
                "the method name, not the display name, identifies the test");
    }

    /**
     * TestInfo captured in a @BeforeEach describes the same test as one injected into the method, so
     * a shared setup field is a valid way to supply it.
     */
    @Test
    void testInfoCapturedInBeforeEachDescribesTheSameTest(TestInfo testInfo) {
        assertEquals(new Junit5InfoBasedTestMeta(testInfo).testMethodName(),
                new Junit5InfoBasedTestMeta(capturedInBeforeEach).testMethodName());
        assertSameIdentity(new JunitJupiterTestMeta(), new Junit5InfoBasedTestMeta(capturedInBeforeEach));
    }

    @Nested
    class NestedTests {

        @Test
        void nestedClassResolvesIdenticallyByBothRoutes(TestInfo testInfo) {
            assertSameIdentity(new JunitJupiterTestMeta(), new Junit5InfoBasedTestMeta(testInfo));
        }

        /** The nested class, not the enclosing one, identifies the test. */
        @Test
        void nestedClassIsTheTestClass(TestInfo testInfo) {
            assertEquals(NestedTests.class.getName(), new Junit5InfoBasedTestMeta(testInfo).testClassName());
            assertEquals(NestedTests.class.getName(), new JunitJupiterTestMeta().testClassName());
        }
    }
}
