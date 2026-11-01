package com.github.karsaig.approvalcrest;

import java.nio.file.Path;

import org.junit.runner.Description;

public class Junit4DescriptionBasedTestMeta extends Junit4TestMetaBase {

    public Junit4DescriptionBasedTestMeta(Description description) {
        super(description.getClassName(), description.getMethodName());
    }

    public Junit4DescriptionBasedTestMeta(Path testClassPath, String testClassName, String testMethodName, Path workingDirectory) {
        super(testClassPath, testClassName, testMethodName, workingDirectory);
    }

}
