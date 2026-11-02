plugins {
    java
    id("io.github.karsaig.approvalcrest")
}

val approvalcrestVersion = gradle.extra["approvalcrestVersion"] as String
// Runs the Maven module's tests where they are. The working directory makes approvalcrest read
// the same approved files as the Maven build.
val itModule = rootDir.resolve("../approvalcrest-testng-integration-tests")

sourceSets.test {
    java.setSrcDirs(listOf(itModule.resolve("src/test/java")))
    resources.setSrcDirs(emptyList<File>())
}

// TestNG 7.10 declares Java 11, so Gradle refuses it at release 8. Maven does not check, and the tests run on
// the build JDK in both builds, so only the class-file version of the tests changes.
java {
    disableAutoTargetJvm()
}

dependencies {
    testImplementation("com.github.karsaig:approvalcrest-testng:$approvalcrestVersion")
    testImplementation("com.github.karsaig:testing-common:$approvalcrestVersion")
    testImplementation(libs.hamcrest)
    testImplementation(libs.testng)
}

tasks.test {
    useTestNG()
    workingDir = itModule
}
