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
