package com.github.karsaig.approvalcrest.jupiter;

import static com.github.karsaig.approvalcrest.jupiter.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.jupiter.matcher.Matchers.sameBeanAs;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import com.github.karsaig.approvalcrest.matcher.DiagnosingCustomisableMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class MetaInfoTest {

    @Test
    public void testJunitJupiterMetaWithSameBeanAsMatcher() {
        JunitJupiterTestMeta underTest = new JunitJupiterTestMeta();

        JunitJupiterTestMeta expected = new JunitJupiterTestMeta(Paths.get("src/test/java/com/github/karsaig/approvalcrest/jupiter"), "com.github.karsaig.approvalcrest.jupiter.MetaInfoTest", "testJunitJupiterMetaWithSameBeanAsMatcher");
        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testJunit5InfoMetaWithSameBeanAsMatcher(TestInfo testInfo) {
        Junit5InfoBasedTestMeta underTest = new Junit5InfoBasedTestMeta(testInfo);

        Junit5InfoBasedTestMeta expected = new Junit5InfoBasedTestMeta(Paths.get("src/test/java/com/github/karsaig/approvalcrest/jupiter"), "com.github.karsaig.approvalcrest.jupiter.MetaInfoTest", "testJunit5InfoMetaWithSameBeanAsMatcher");
        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testStacktraceAndTestInfoBasedMetaHasSameResult(TestInfo testInfo) {
        JunitJupiterTestMeta input1 = new JunitJupiterTestMeta();
        Junit5InfoBasedTestMeta input2 = new Junit5InfoBasedTestMeta(testInfo);

        DiagnosingCustomisableMatcher<Object> matcher = sameBeanAs(input2);
        matcher.skipClassComparison();
        assertThat(input1, matcher);
    }

    @Test
    public void toStringContainsAllFields() {
        JunitJupiterTestMeta underTest = new JunitJupiterTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/jupiter"),
                "com.github.karsaig.approvalcrest.jupiter.MetaInfoTest",
                "toStringContainsAllFields");
        String result = underTest.toString();
        assertTrue(result.contains("cn=com.github.karsaig.approvalcrest.jupiter.MetaInfoTest"));
        assertTrue(result.contains("mn=toStringContainsAllFields"));
        assertTrue(result.contains("cp="));
        assertTrue(result.contains("wd="));
    }
}
