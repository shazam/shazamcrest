subprojects {
    pluginManager.withPlugin("java") {
        tasks.withType<JavaCompile>().configureEach {
            // The Maven integration-test modules compile at java.version.to.run, 11 by default.
            options.release = 11
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-path", "-Xlint:-options", "-Werror"))
        }
        tasks.withType<Test>().configureEach {
            // Surefire's default includes, so that Gradle runs the same test classes as Maven.
            include("**/Test*.class", "**/*Test.class", "**/*Tests.class", "**/*TestCase.class")
        }
    }
}
