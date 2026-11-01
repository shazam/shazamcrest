package com.github.karsaig.approvalcrest.jupiter;

import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.nio.file.Path;

public class Junit5InfoBasedTestMeta extends Junit5TestMetaBase {


    public Junit5InfoBasedTestMeta(TestInfo testInfo) {
        super(getRequiredClassName(testInfo), getTestMethodName(testInfo));
    }

    public Junit5InfoBasedTestMeta(TestInfo testInfo, String defaultSourceRoot) {
        super(getRequiredClassName(testInfo), getTestMethodName(testInfo), defaultSourceRoot);
    }

    public Junit5InfoBasedTestMeta(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }

    private static String getTestMethodName(TestInfo testInfo) {
        return testInfo.getTestMethod().map(Method::getName).orElseGet(testInfo::getDisplayName);
    }

    private static String getRequiredClassName(TestInfo testInfo) {
        return testInfo.getTestClass().orElseThrow(() -> new IllegalStateException("Cannot get class from Testinfo, custom implementation of TestMetaInformation required!")).getName();
    }

}
