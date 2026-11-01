package com.github.karsaig.approvalcrest;

import java.nio.file.Path;
import java.util.Objects;

import org.junit.Test;

public class Junit4TestMeta extends Junit4TestMetaBase {

    private static final String INHERITED_TEST_METHOD_FIX = "Do either of the following to solve it:\n1. Use Junit4DesciptionWatcher as a @Rule and the matcher constructor with Description parameter; the Description knows the concrete test class\n2. Provide a custom implementation of TestMetaInformation, this is rarely needed.";

    public Junit4TestMeta() {
        this(Objects.requireNonNull(getTestStackTraceElement(Thread.currentThread().getStackTrace()), "Cannot determine test method for Junit4TestMeta do either of the following to solve it:\n1. Use Junit4DesciptionWatcher as a @Rule and the matcher constructor with Description parameter!\n2. Provide a custom implementation of TestMetaInformation, this is rarely needed."));
    }

    private Junit4TestMeta(StackTraceElement testStackTraceElement) {
        super(concreteClassName(testStackTraceElement), testStackTraceElement.getMethodName());
    }

    private static String concreteClassName(StackTraceElement testStackTraceElement) {
        String className = testStackTraceElement.getClassName();
        requireConcreteTestClass(className, INHERITED_TEST_METHOD_FIX);
        return className;
    }

    public Junit4TestMeta(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }

    static StackTraceElement getTestStackTraceElement(StackTraceElement[] stackTrace) {
        return findTestStackTraceElement(stackTrace, Junit4TestMeta::isTestMethod);
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
