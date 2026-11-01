package com.github.karsaig.approvalcrest.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * The stack-trace route and the TestInfo route must describe the running test identically, because
 * that description determines the approved file's path. If they disagreed, adding a [TestInfo]
 * parameter to an existing test would silently orphan its approved file.
 */
class RouteEquivalenceTest {

    private var capturedInBeforeEach: TestInfo? = null

    @BeforeEach
    fun captureTestInfo(testInfo: TestInfo) {
        capturedInBeforeEach = testInfo
    }

    private fun assertSameIdentity(stackRoute: KotlinTestMeta, infoRoute: KotlinInfoBasedTestMeta) {
        assertEquals(stackRoute.testClassName(), infoRoute.testClassName(), "testClassName")
        assertEquals(stackRoute.testMethodName(), infoRoute.testMethodName(), "testMethodName")
        assertEquals(stackRoute.testClassPath, infoRoute.testClassPath, "testClassPath")
        assertEquals(stackRoute.workingDirectory(), infoRoute.workingDirectory(), "workingDirectory")
    }

    @Test
    fun publicFunctionResolvesIdenticallyByBothRoutes(testInfo: TestInfo) {
        assertSameIdentity(KotlinTestMeta(), KotlinInfoBasedTestMeta(testInfo))
    }

    @Test
    internal fun internalFunctionResolvesIdenticallyByBothRoutes(testInfo: TestInfo) {
        assertSameIdentity(KotlinTestMeta(), KotlinInfoBasedTestMeta(testInfo))
    }

    @Test
    @DisplayName("a display name that is nothing like the function name")
    fun displayNameDoesNotAffectIdentity(testInfo: TestInfo) {
        val infoRoute = KotlinInfoBasedTestMeta(testInfo)

        assertSameIdentity(KotlinTestMeta(), infoRoute)
        assertEquals("displayNameDoesNotAffectIdentity", infoRoute.testMethodName(),
                "the function name, not the display name, identifies the test")
    }

    @Test
    fun testInfoCapturedInBeforeEachDescribesTheSameTest(testInfo: TestInfo) {
        assertEquals(
                KotlinInfoBasedTestMeta(testInfo).testMethodName(),
                KotlinInfoBasedTestMeta(capturedInBeforeEach!!).testMethodName()
        )
        assertSameIdentity(KotlinTestMeta(), KotlinInfoBasedTestMeta(capturedInBeforeEach!!))
    }

    /**
     * With the property unset, both routes must still resolve `src/test/kotlin`. This is the
     * regression that would relocate every Kotlin approved file in existence, so it is asserted
     * directly rather than left to the integration tests to notice.
     */
    @Test
    fun bothKotlinRoutesDefaultToSrcTestKotlin(testInfo: TestInfo) {
        val expected = java.nio.file.Paths.get("src/test/kotlin/com/github/karsaig/approvalcrest/kotlin")

        assertEquals(expected, KotlinTestMeta().testClassPath)
        assertEquals(expected, KotlinInfoBasedTestMeta(testInfo).testClassPath)
    }

    /**
     * `fileMatcherSourceRoot` is shared by every framework; only the fallback differs. Kotlin
     * projects whose tests live outside `src/test/kotlin` can therefore configure it like everyone
     * else.
     */
    @Test
    fun bothKotlinRoutesHonourTheSourceRootProperty(testInfo: TestInfo) {
        System.setProperty("fileMatcherSourceRoot", "src/it/kotlin")
        try {
            val expected = java.nio.file.Paths.get("src/it/kotlin/com/github/karsaig/approvalcrest/kotlin")

            assertEquals(expected, KotlinTestMeta().testClassPath,
                    "the stack route must honour the property")
            assertEquals(expected, KotlinInfoBasedTestMeta(testInfo).testClassPath,
                    "and the TestInfo route must honour it identically")
        } finally {
            System.clearProperty("fileMatcherSourceRoot")
        }
    }

    @Test
    fun bothKotlinRoutesHonourTheAlias(testInfo: TestInfo) {
        System.setProperty("fmSourceRoot", "src/it/kotlin")
        try {
            val expected = java.nio.file.Paths.get("src/it/kotlin/com/github/karsaig/approvalcrest/kotlin")

            assertEquals(expected, KotlinTestMeta().testClassPath)
            assertEquals(expected, KotlinInfoBasedTestMeta(testInfo).testClassPath)
        } finally {
            System.clearProperty("fmSourceRoot")
        }
    }

    @Nested
    inner class NestedTests {

        @Test
        fun nestedClassResolvesIdenticallyByBothRoutes(testInfo: TestInfo) {
            assertSameIdentity(KotlinTestMeta(), KotlinInfoBasedTestMeta(testInfo))
        }

        @Test
        fun nestedClassIsTheTestClass(testInfo: TestInfo) {
            assertEquals(NestedTests::class.java.name, KotlinInfoBasedTestMeta(testInfo).testClassName())
            assertEquals(NestedTests::class.java.name, KotlinTestMeta().testClassName())
        }
    }
}
