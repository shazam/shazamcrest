package com.github.karsaig.approvalcrest;

import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameBeanAs;
import static com.github.karsaig.approvalcrest.testng.matcher.Matchers.sameJsonAsApproved;

import java.nio.file.Paths;

import org.opentest4j.AssertionFailedError;
import org.testng.annotations.Test;

import com.github.karsaig.approvalcrest.matcher.TestMetaInformation;
import com.github.karsaig.approvalcrest.testng.MatcherAssert;
import com.github.karsaig.approvalcrest.testng.TestNgTestMeta;

public class MatcherAssertExceptionMatchingTests {

    @Test
    public void assertThrowsWithSameBeanAsShouldPassWhenExpectationIsMet() {
        IllegalStateException expected = new IllegalStateException("Test excpetion message...");
        MatcherAssert.assertThrows(sameBeanAs(expected), () -> {
            throw new IllegalStateException("Test excpetion message...");
        });
    }

    @Test(expectedExceptions = AssertionFailedError.class)
    public void assertThrowsWithSameBeanAsShouldFailWhenMessageIsDifferent() {
        IllegalStateException expected = new IllegalStateException("Different excpetion message...");
        MatcherAssert.assertThrows(sameBeanAs(expected), () -> {
            throw new IllegalStateException("Test excpetion message...");
        });
    }

    @Test(expectedExceptions = AssertionFailedError.class)
    public void assertThrowsWithSameBeanAsShouldFailWhenNoExceptionThrown() {
        IllegalStateException expected = new IllegalStateException("Different excpetion message...");
        MatcherAssert.assertThrows(sameBeanAs(expected), () -> {
        });
    }

    @Test
    public void assertThrowsWithSameJsonAsShouldPassWhenExpectationIsMet() {
        TestMetaInformation testMeta = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest"),
                "com.github.karsaig.approvalcrest.MatcherAssertExceptionMatchingTests",
                "assertThrowsWithSameJsonAsShouldPassWhenExpectationIsMet");
        MatcherAssert.assertThrows(sameJsonAsApproved(testMeta), () -> {
            throw new IllegalStateException("Test excpetion message...");
        });
    }

    @Test(expectedExceptions = AssertionFailedError.class)
    public void assertThrowsWithSameJsonAsShouldFailWhenNoExceptionThrown() {
        TestMetaInformation testMeta = new TestNgTestMeta(
                Paths.get("src/test/java/com/github/karsaig/approvalcrest"),
                "com.github.karsaig.approvalcrest.MatcherAssertExceptionMatchingTests",
                "assertThrowsWithSameJsonAsShouldFailWhenNoExceptionThrown");
        MatcherAssert.assertThrows(sameJsonAsApproved(testMeta), () -> {
        });
    }
}
