plugins {
    java
    id("io.github.karsaig.approvalcrest")
}

val approvalcrestVersion = gradle.extra["approvalcrestVersion"] as String
// Runs the Maven module's tests where they are. The working directory makes approvalcrest read
// the same approved files as the Maven build.
val itModule = rootDir.resolve("../approvalcrest-junit-vintage-integration-tests")

sourceSets.test {
    java.setSrcDirs(listOf(itModule.resolve("src/test/java")))
    resources.setSrcDirs(emptyList<File>())
}

dependencies {
    testImplementation("com.github.karsaig:approvalcrest:$approvalcrestVersion") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("com.github.karsaig:approvalcrest-junit-jupiter:$approvalcrestVersion")
    testImplementation("com.github.karsaig:testing-common:$approvalcrestVersion")
    testImplementation(libs.hamcrest)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    workingDir = itModule
}
