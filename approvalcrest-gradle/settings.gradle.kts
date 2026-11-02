import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

rootProject.name = "approvalcrest-gradle"

// The version under test is the version of the Maven reactor, so a release bumps one place only.
val approvalcrestVersion: String = providers.fileContents(layout.rootDirectory.file("../pom.xml")).asBytes.map { pom ->
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.inputStream())
    XPathFactory.newInstance().newXPath().evaluate("/project/version", document)
}.get()
gradle.extra["approvalcrestVersion"] = approvalcrestVersion

pluginManagement {
    includeBuild("approvalcrest-gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        // Only the reactor artifacts come from ~/.m2, so mvn install must run first.
        mavenLocal {
            content {
                includeGroup("com.github.karsaig")
            }
        }
        mavenCentral()
    }
}

include(
    "integration-tests:junit4",
    "integration-tests:jupiter",
    "integration-tests:testng",
    "integration-tests:vintage",
    "integration-tests:kotlin",
    "doc-snippets",
)

// JUnit 6 needs Java 17, so its projects join at release 17 and later, as the junit6 profiles do in Maven.
if (providers.gradleProperty("java.version.to.run").orElse("11").get().toInt() >= 17) {
    include("integration-tests:jupiter-junit6", "integration-tests:vintage-junit6")
}
