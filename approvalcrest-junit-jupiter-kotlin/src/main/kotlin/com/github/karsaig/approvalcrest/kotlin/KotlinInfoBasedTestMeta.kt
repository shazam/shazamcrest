package com.github.karsaig.approvalcrest.kotlin

import com.github.karsaig.approvalcrest.jupiter.Junit5InfoBasedTestMeta
import com.github.karsaig.approvalcrest.kotlin.KotlinTestMeta.Companion.SRC_TEST_KOTLIN_PATH
import org.junit.jupiter.api.TestInfo
import java.nio.file.Path

class KotlinInfoBasedTestMeta: Junit5InfoBasedTestMeta {

    constructor(testInfo: TestInfo) : super(testInfo, SRC_TEST_KOTLIN_PATH)

    internal constructor(testClassPath: Path, testClassName: String, testMethodName: String) :
            super(testClassPath, testClassName, testMethodName)

}
