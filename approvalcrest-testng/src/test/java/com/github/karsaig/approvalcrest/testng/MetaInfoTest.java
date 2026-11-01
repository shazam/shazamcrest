package com.github.karsaig.approvalcrest.testng;

import static com.github.karsaig.approvalcrest.testng.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameBeanAs;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Paths;

import com.github.karsaig.approvalcrest.matcher.DiagnosingCustomisableMatcher;
import org.testng.annotations.Test;

public class MetaInfoTest {

    @Test
    public void testTestNgMetaWithSameBeanAsMatcher() {
        TestNgTestMeta underTest = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/testng"),
                "com.github.karsaig.approvalcrest.testng.MetaInfoTest",
                "testTestNgMetaWithSameBeanAsMatcher");

        TestNgTestMeta expected = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/testng"),
                "com.github.karsaig.approvalcrest.testng.MetaInfoTest",
                "testTestNgMetaWithSameBeanAsMatcher");

        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testMethodBasedMetaWithSameBeanAsMatcher() throws Exception {
        Method method = TestNgAnnotatedTestHelper.class.getMethod("annotatedTestMethod");
        TestNgMethodBasedTestMeta underTest = new TestNgMethodBasedTestMeta(method);

        TestNgMethodBasedTestMeta expected = new TestNgMethodBasedTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/testng"),
                TestNgAnnotatedTestHelper.class.getName(),
                "annotatedTestMethod");

        DiagnosingCustomisableMatcher<Object> matcher = sameBeanAs(expected);
        matcher.skipClassComparison();
        assertThat(underTest, matcher);
    }

    @Test
    public void testStackTraceBasedMetaAutoDetectsCurrentTest() {
        TestNgTestMeta underTest = new TestNgTestMeta();

        TestNgTestMeta expected = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/testng"),
                "com.github.karsaig.approvalcrest.testng.MetaInfoTest",
                "testStackTraceBasedMetaAutoDetectsCurrentTest");

        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testMethodBasedAndStackBasedMetaHaveSameResult(Method testMethod) {
        TestNgTestMeta stackBased = new TestNgTestMeta();
        TestNgMethodBasedTestMeta methodBased = new TestNgMethodBasedTestMeta(testMethod);

        DiagnosingCustomisableMatcher<Object> matcher = sameBeanAs(methodBased);
        matcher.skipClassComparison();
        assertThat(stackBased, matcher);
    }

    @Test
    public void toStringContainsAllFields() {
        TestNgTestMeta underTest = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest/testng"),
                "com.github.karsaig.approvalcrest.testng.MetaInfoTest",
                "toStringContainsAllFields");
        String result = underTest.toString();
        assertTrue(result.contains("cn=com.github.karsaig.approvalcrest.testng.MetaInfoTest"));
        assertTrue(result.contains("mn=toStringContainsAllFields"));
        assertTrue(result.contains("cp="));
        assertTrue(result.contains("wd="));
    }
}
