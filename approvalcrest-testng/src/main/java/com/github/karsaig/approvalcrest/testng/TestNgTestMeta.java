package com.github.karsaig.approvalcrest.testng;

import com.github.karsaig.approvalcrest.matcher.AbstractTestMetaBase;

import java.nio.file.Path;
import java.util.Objects;

import org.testng.annotations.Test;

public class TestNgTestMeta extends AbstractTestMetaBase {

    static final String INHERITED_TEST_METHOD_FIX = "Do either of the following to solve it:\n1. Provide a custom implementation of TestMetaInformation that supplies the concrete test class\n2. Give each subclass its own test method rather than inheriting one.";
    private static final String CANNOT_DETERMINE_TEST_METHOD_ERROR = "Cannot determine test method for TestNgTestMeta, do either of the following to solve it:\n1. Pass java.lang.reflect.Method as constructor parameter to matcher (TestNG injects it into test methods)\n2. Provide a custom implementation of TestMetaInformation, this is rarely needed.";

    public TestNgTestMeta() {
        this(Objects.requireNonNull(getTestStackTraceElement(Thread.currentThread().getStackTrace()), CANNOT_DETERMINE_TEST_METHOD_ERROR));
    }

    private TestNgTestMeta(StackTraceElement testStackTraceElement) {
        super(concreteClassName(testStackTraceElement), testStackTraceElement.getMethodName());
    }

    private static String concreteClassName(StackTraceElement testStackTraceElement) {
        String className = testStackTraceElement.getClassName();
        requireConcreteTestClass(className, INHERITED_TEST_METHOD_FIX);
        return className;
    }

    public TestNgTestMeta(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }

    static StackTraceElement getTestStackTraceElement(StackTraceElement[] stackTrace) {
        return findTestStackTraceElement(stackTrace, TestNgTestMeta::isTestMethod);
    }

    private static boolean isTestMethod(StackTraceElement element) {
        try {
            Class<?> clazz = Class.forName(element.getClassName());
            return findMethods(clazz, element.getMethodName()).stream()
                    .anyMatch(method -> method.isAnnotationPresent(Test.class));
        } catch (Throwable e) {
            return false;
        }
    }
}
