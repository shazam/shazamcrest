package com.github.karsaig.approvalcrest.kotlin

import com.github.karsaig.approvalcrest.jupiter.MatcherAssert
import com.github.karsaig.approvalcrest.kotlin.matcher.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.nio.file.Paths

class MetaInfoTest {
    @Test
    fun testKotlinMetaWithSameBeanAsMatcher() {
        val underTest = KotlinTestMeta()
        val expected = KotlinTestMeta(
                Paths.get("src/test/kotlin/com/github/karsaig/approvalcrest/kotlin"),
                "com.github.karsaig.approvalcrest.kotlin.MetaInfoTest",
                "testKotlinMetaWithSameBeanAsMatcher"
        )
        MatcherAssert.assertThat(underTest, Matchers.sameBeanAs(expected))
    }

    @Test
    fun testKotlinInfoBasedMetaWithSameBeanAsMatcher(testInfo: TestInfo) {
        val underTest = KotlinInfoBasedTestMeta(testInfo)
        val expected = KotlinInfoBasedTestMeta(
                Paths.get("src/test/kotlin/com/github/karsaig/approvalcrest/kotlin"),
                "com.github.karsaig.approvalcrest.kotlin.MetaInfoTest",
                "testKotlinInfoBasedMetaWithSameBeanAsMatcher"
        )
        MatcherAssert.assertThat(underTest, Matchers.sameBeanAs(expected))
    }

    @Test
    fun testStacktraceAndKotlinInfoBasedMetaHasSameResult(testInfo: TestInfo) {
        val input1 = KotlinTestMeta()
        val input2 = KotlinInfoBasedTestMeta(testInfo)

        val matcher = Matchers.sameBeanAs<Any>(input2)
        matcher.skipClassComparison()
        MatcherAssert.assertThat(input1, matcher)
    }
}
