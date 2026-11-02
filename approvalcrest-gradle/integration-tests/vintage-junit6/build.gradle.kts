plugins {
    java
    id("io.github.karsaig.approvalcrest")
}

val approvalcrestVersion = gradle.extra["approvalcrestVersion"] as String
// Runs the Vintage module's tests and approved files on JUnit 6, as the Maven module
// approvalcrest-junit-vintage-integration-tests-junit6 does.
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
    testImplementation(platform(libs.junit6.bom))
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
