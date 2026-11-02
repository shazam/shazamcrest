// The release level of the integration tests, as in the Maven build: -Pjava.version.to.run=<v>, 11 by default.
val javaVersionToRun = providers.gradleProperty("java.version.to.run").orElse("11").map(String::toInt)

subprojects {
    pluginManager.withPlugin("java") {
        tasks.withType<JavaCompile>().configureEach {
            options.release = if (project.parent?.name == "integration-tests") javaVersionToRun else provider { 11 }
            // UTF-8 whatever the locale, as project.build.sourceEncoding and the Surefire argLine give in Maven.
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-Xlint:-options", "-Werror"))
        }
        tasks.withType<Test>().configureEach {
            jvmArgs("-Dfile.encoding=UTF-8")
            // Surefire's default includes, so that Gradle runs the same test classes as Maven.
            include("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class")
        }
    }
}

tasks.register("check") {
    dependsOn(gradle.includedBuild("approvalcrest-gradle-plugin").task(":check"))
}

// CI runs the integration tests again at each lower release level, as the Maven matrix does.
tasks.register("integrationTest") {
    dependsOn(subprojects.filter { it.parent?.name == "integration-tests" }.map { "${it.path}:test" })
}
