package com.github.karsaig.approvalcrest.jupiter;

import com.github.karsaig.approvalcrest.matcher.AbstractTestMetaBase;

import java.nio.file.Path;

abstract class Junit5TestMetaBase extends AbstractTestMetaBase {

    protected Junit5TestMetaBase(String testClassName, String testMethodName) {
        super(testClassName, testMethodName);
    }

    protected Junit5TestMetaBase(String testClassName, String testMethodName, String defaultSourceRoot) {
        super(testClassName, testMethodName, defaultSourceRoot);
    }

    protected Junit5TestMetaBase(Path testClassPath, String testClassName, String testMethodName) {
        super(testClassPath, testClassName, testMethodName);
    }

    protected Junit5TestMetaBase(Path testClassPath, String testClassName, String testMethodName, Path workingDirectory) {
        super(workingDirectory, testClassPath, testClassName, testMethodName);
    }
}
