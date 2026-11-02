import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("io.github.karsaig.approvalcrest")
}

val approvalcrestVersion = gradle.extra["approvalcrestVersion"] as String
// Runs the Maven module's tests where they are. The working directory makes approvalcrest read
// the same approved files as the Maven build.
val itModule = rootDir.resolve("../approvalcrest-junit-jupiter-kotlin-integration-tests")

sourceSets.test {
    kotlin.setSrcDirs(listOf(itModule.resolve("src/test/kotlin")))
    java.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
}

// Kotlin keeps one target, as in the Maven build. The Java task must match it, or Kotlin stops the build.
tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    testImplementation("com.github.karsaig:approvalcrest-junit-jupiter-kotlin:$approvalcrestVersion")
    testImplementation("com.github.karsaig:testing-common:$approvalcrestVersion")
    testImplementation(libs.kotlin.test)
    testImplementation(libs.hamcrest)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    workingDir = itModule
}
