package com.github.karsaig.approvalcrest;

import com.github.karsaig.approvalcrest.matcher.AbstractTestMetaBase;

import java.nio.file.Path;

abstract class Junit4TestMetaBase extends AbstractTestMetaBase {

    protected Junit4TestMetaBase(String testClassName, String testMethodName) {
        super(testClassName, testMethodName);
    }

    public Junit4TestMetaBase(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }

    public Junit4TestMetaBase(Path testClassPath, String testClassName, String testMethodName, Path workingDirectory) {
        super(workingDirectory, testClassPath, testClassName, testMethodName);
    }
}
