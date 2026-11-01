package com.github.karsaig.approvalcrest;

import static com.github.karsaig.approvalcrest.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.matcher.Matchers.sameBeanAs;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.karsaig.approvalcrest.matcher.DiagnosingCustomisableMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;

import static org.junit.Assert.assertTrue;

public class MetaInfoTest {

    @Rule
    public Junit4DesciptionWatcher testWatcher = new Junit4DesciptionWatcher();

    @Test
    public void testJunit4MetaWithSameBeanAsMatcher() {
        Junit4TestMeta underTest = new Junit4TestMeta();

        Junit4TestMeta expected = new Junit4TestMeta(Paths.get("src/test/java/com/github/karsaig/approvalcrest"), "com.github.karsaig.approvalcrest.MetaInfoTest", "testJunit4MetaWithSameBeanAsMatcher");
        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testJunit4DescriptionMetaWithSameBeanAsMatcher() {
        Junit4DescriptionBasedTestMeta underTest = new Junit4DescriptionBasedTestMeta(testWatcher.getDescription());

        Junit4DescriptionBasedTestMeta expected = new Junit4DescriptionBasedTestMeta(Paths.get("src/test/java/com/github/karsaig/approvalcrest"), "com.github.karsaig.approvalcrest.MetaInfoTest", "testJunit4DescriptionMetaWithSameBeanAsMatcher", underTest.workingDirectory());
        assertThat(underTest, sameBeanAs(expected));
    }

    @Test
    public void testStacktraceAndDescriptionBasedMetaHasSameResult() {
        Junit4TestMeta input1 = new Junit4TestMeta();
        Junit4DescriptionBasedTestMeta input2 = new Junit4DescriptionBasedTestMeta(testWatcher.getDescription());

        DiagnosingCustomisableMatcher<Object> matcher = sameBeanAs(input2);
        matcher.skipClassComparison();
        assertThat(input1, matcher);
    }

    @Test
    public void toStringContainsAllFields() {
        Junit4TestMeta underTest = new Junit4TestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest"),
                "com.github.karsaig.approvalcrest.MetaInfoTest",
                "toStringContainsAllFields");
        String result = underTest.toString();
        assertTrue(result.contains("cn=com.github.karsaig.approvalcrest.MetaInfoTest"));
        assertTrue(result.contains("mn=toStringContainsAllFields"));
        assertTrue(result.contains("cp="));
        assertTrue(result.contains("wd="));
    }

}
