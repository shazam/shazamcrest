plugins {
    java
}

val approvalcrestVersion = gradle.extra["approvalcrestVersion"] as String
val pluginBuild = gradle.includedBuild("approvalcrest-gradle-plugin")

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // The snippets request the plugin with a version, so the plugin under test must be in a repository.
    dependsOn(pluginBuild.task(":publishAllPublicationsToTestRepoRepository"))
    systemProperty("approvalcrest.version", approvalcrestVersion)
    systemProperty("approvalcrest.repoRoot", rootDir.resolve("..").canonicalPath)
    systemProperty("approvalcrest.pluginRepo", pluginBuild.projectDir.resolve("build/test-repo").canonicalPath)
    inputs.files(fileTree(rootDir.resolve("../docs")) { include("*.md") }, rootDir.resolve("../README.md"))
        .withPropertyName("docs")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    testLogging {
        events("passed", "failed", "skipped")
    }
}
