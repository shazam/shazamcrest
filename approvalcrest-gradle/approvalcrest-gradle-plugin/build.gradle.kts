import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
}

group = "io.github.karsaig"
// The plugin version is the version of the Maven reactor, so a release bumps one place only.
version = providers.fileContents(layout.projectDirectory.file("../../pom.xml")).asBytes.map { pom ->
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.inputStream())
    XPathFactory.newInstance().newXPath().evaluate("/project/version", document)
}.get()

tasks.withType<JavaCompile>().configureEach {
    // Gradle 8 can run on Java 8, so the plugin must load there too.
    options.release = 8
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Xlint:-classfile", "-Werror"))
}

val generatePluginProperties = tasks.register<WriteProperties>("generatePluginProperties") {
    destinationFile = layout.buildDirectory.file("generated/approvalcrest-gradle-plugin.properties")
    property("version", project.version)
}

tasks.processResources {
    from(generatePluginProperties) {
        into("io/github/karsaig/approvalcrest/gradle")
    }
}

gradlePlugin {
    website = "https://github.com/karsaig/approvalcrest"
    vcsUrl = "https://github.com/karsaig/approvalcrest"
    plugins {
        create("approvalcrest") {
            id = "io.github.karsaig.approvalcrest"
            implementationClass = "io.github.karsaig.approvalcrest.gradle.ApprovalcrestPlugin"
            displayName = "Approvalcrest"
            description = "Configures the Gradle test tasks for approvalcrest: passes -D properties from the command line to the " +
                    "test JVM, makes approved files test inputs, and adds the approvalcrestDedup and approvalcrestReinstate tasks."
            tags = listOf("testing", "approval-testing", "snapshot-testing", "hamcrest")
        }
    }
}

// The doc-snippet tests request the plugin with a version, as a reader does, so they resolve it from this repository.
publishing {
    repositories {
        maven {
            name = "testRepo"
            url = uri(layout.buildDirectory.dir("test-repo"))
        }
    }
}

val functionalTest = sourceSets.create("functionalTest")
gradlePlugin.testSourceSets(functionalTest)

dependencies {
    "functionalTestImplementation"(platform(libs.junit.bom))
    "functionalTestImplementation"(libs.junit.jupiter.api)
    "functionalTestImplementation"(libs.junit.jupiter.params)
    "functionalTestImplementation"(gradleTestKit())
    "functionalTestRuntimeOnly"(libs.junit.jupiter.engine)
    "functionalTestRuntimeOnly"(libs.junit.platform.launcher)
}

// Gradle 8.14 does not run on JDK 25, so its fixture builds use a JDK 17 daemon.
val jdk17Home = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(17)
}.map { it.metadata.installationPath.asFile.absolutePath }

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
    systemProperty("approvalcrest.version", project.version.toString())
    jvmArgumentProviders.add(Jdk17HomeArgument(jdk17Home))
    // Each case runs Gradle builds; the default timeout of the test JVM is enough, but the output helps on CI.
    testLogging {
        events("passed", "failed", "skipped")
    }
}

tasks.check {
    dependsOn(functionalTestTask)
}

class Jdk17HomeArgument(@get:Input val home: Provider<String>) : CommandLineArgumentProvider {
    override fun asArguments() = listOf("-Dapprovalcrest.jdk17Home=${home.get()}")
}
