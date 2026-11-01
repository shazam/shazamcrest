package com.github.karsaig.approvalcrest.testng;

import com.github.karsaig.approvalcrest.matcher.AbstractTestMetaBase;

import java.lang.reflect.Method;
import java.nio.file.Path;

public class TestNgMethodBasedTestMeta extends AbstractTestMetaBase {

    public TestNgMethodBasedTestMeta(Method testMethod) {
        super(concreteClassName(testMethod), testMethod.getName());
    }

    /**
     * {@code Method#getDeclaringClass()} names the class the method is declared in, so an inherited
     * test method reports the base class rather than the subclass running it - the same ambiguity
     * the stack-trace route has, despite this route being handed the method directly.
     */
    private static String concreteClassName(Method testMethod) {
        String className = testMethod.getDeclaringClass().getName();
        requireConcreteTestClass(className, TestNgTestMeta.INHERITED_TEST_METHOD_FIX);
        return className;
    }

    public TestNgMethodBasedTestMeta(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }
}
