package io.github.karsaig.approvalcrest.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Configures every {@code Test} task of a Java project for approvalcrest, and adds the
 * {@code approvalcrestDedup} and {@code approvalcrestReinstate} tasks.
 */
public class ApprovalcrestPlugin implements Plugin<Project> {

    private static final String SOURCE_ROOT = "fileMatcherSourceRoot";
    private static final String SOURCE_ROOT_ALIAS = "fmSourceRoot";
    private static final String SHARED_DIR = "fileMatcherSharedDir";
    private static final String SHARED_DIR_ALIAS = "fmSharedDir";
    private static final String DEFAULT_SHARED_DIR = "src/test/java/shared-approvals";
    private static final List<String> TOOL_PROPERTIES = Arrays.asList(SOURCE_ROOT, SOURCE_ROOT_ALIAS, SHARED_DIR,
            SHARED_DIR_ALIAS, "fileMatcherSharedDirBucketDepth", "fmSharedDirBucketDepth");
    // A run with one of these flags writes approved files. Its result depends on files that the run itself changes.
    private static final List<String> WRITE_FLAGS = Arrays.asList("fileMatcherUpdateInPlace", "fMUInPlace",
            "jsonMatcherUpdateInPlace", "fileMatcherPassOnCreate", "fMPOnCreate");
    // The values that approvalcrest reads as true.
    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays.asList("true", "t", "1", "yes", "y"));

    @Override
    public void apply(Project project) {
        ApprovalcrestExtension extension = project.getExtensions().create("approvalcrest", ApprovalcrestExtension.class);
        extension.getAttachAgent().convention(false);
        String version = pluginVersion();
        Map<String, String> commandLine = commandLineProperties(project);

        project.getPlugins().withType(JavaBasePlugin.class, javaBase -> {
            Configuration agent = toolClasspath(project, "approvalcrestAgentClasspath",
                    "com.github.karsaig:approvalcrest-agent:" + version, false);
            Configuration dedup = toolClasspath(project, "approvalcrestDedupClasspath",
                    "com.github.karsaig:approvalcrest-dedup:" + version, true);
            TaskProvider<ApprovalcrestDedupTask> dedupTask = registerDedupTask(project, "approvalcrestDedup", false, dedup,
                    "Replaces approved files that have the same content with pointers to one shared canonical file.");
            TaskProvider<ApprovalcrestDedupTask> reinstateTask = registerDedupTask(project, "approvalcrestReinstate", true, dedup,
                    "Replaces pointer files with the content of their canonical file again.");

            // After the build script, so that the values that it sets are known, and the command line can win over them.
            project.afterEvaluate(evaluated -> {
                evaluated.getTasks().withType(Test.class)
                        .configureEach(test -> configureTest(evaluated, test, commandLine, extension, agent));
                Map<String, Object> toolProperties = toolProperties(evaluated, commandLine);
                dedupTask.configure(task -> configureDedupTask(evaluated, task, toolProperties));
                reinstateTask.configure(task -> configureDedupTask(evaluated, task, toolProperties));
            });
        });
    }

    /**
     * The {@code -D} properties of the command line. The values come through the providers API, which makes every
     * system property a configuration cache input: the start parameter alone is not one, and a cached configuration
     * from a run without {@code -D} would otherwise run again with the flag missing.
     */
    private static Map<String, String> commandLineProperties(Project project) {
        Map<String, String> systemProperties = project.getProviders().systemPropertiesPrefixedBy("").get();
        Map<String, String> commandLine = new TreeMap<>();
        for (String name : project.getGradle().getStartParameter().getSystemPropertiesArgs().keySet()) {
            // Gradle's own options configure the build, not the tests; as test inputs they would only split the cache.
            if (name.startsWith("org.gradle.")) {
                continue;
            }
            String value = systemProperties.get(name);
            if (value != null) {
                commandLine.put(name, value);
            }
        }
        return commandLine;
    }

    private static void configureTest(Project project, Test test, Map<String, String> commandLine,
                                      ApprovalcrestExtension extension, Configuration agent) {
        test.systemProperties(commandLine);
        if (!"test".equals(test.getName()) && first(test.getSystemProperties(), SOURCE_ROOT, SOURCE_ROOT_ALIAS) == null) {
            String suiteRoot = suiteRoot(project, test.getName());
            if (suiteRoot != null) {
                test.systemProperty(SOURCE_ROOT, suiteRoot);
            }
        }
        // Lazy, so that configuration in a later afterEvaluate still counts.
        test.getInputs().files(project.files((Callable<FileCollection>) () -> approvedFiles(project, test)))
                .withPropertyName("approvalcrestApprovedFiles")
                .withPathSensitivity(PathSensitivity.RELATIVE);
        test.getOutputs().upToDateWhen(new NotWriteRun());
        test.getOutputs().doNotCacheIf("approvalcrest writes approved files in this run", new WriteRun());
        test.getJvmArgumentProviders().add(new AgentArgument(extension.getAttachAgent(), agent, project.files()));
        test.doFirst(new ApplyCommandLine(commandLine));
    }

    /**
     * The source root for the test task of a source set other than {@code test}: {@code src/<name>/java}, or
     * {@code src/<name>/kotlin} when only that exists. Null when the task has no source set of its name, or when
     * both directories exist, because one property cannot name two roots.
     */
    private static String suiteRoot(Project project, String name) {
        SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
        if (sourceSets == null || sourceSets.findByName(name) == null) {
            return null;
        }
        boolean java = project.file("src/" + name + "/java").isDirectory();
        boolean kotlin = project.file("src/" + name + "/kotlin").isDirectory();
        if (java == kotlin) {
            return java ? null : "src/" + name + "/java";
        }
        return java ? "src/" + name + "/java" : "src/" + name + "/kotlin";
    }

    private static FileCollection approvedFiles(Project project, Test test) {
        Map<String, Object> properties = test.getSystemProperties();
        List<String> roots = new ArrayList<>();
        String sourceRoot = first(properties, SOURCE_ROOT, SOURCE_ROOT_ALIAS);
        if (sourceRoot != null) {
            roots.add(sourceRoot);
        } else {
            roots.add("src/test/java");
            roots.add("src/test/kotlin");
        }
        String sharedDir = first(properties, SHARED_DIR, SHARED_DIR_ALIAS);
        roots.add(sharedDir != null ? sharedDir : DEFAULT_SHARED_DIR);

        ConfigurableFileCollection files = project.files();
        for (String root : roots) {
            File dir = test.getWorkingDir().toPath().resolve(root.replace('\\', '/')).toFile();
            files.from(project.fileTree(dir, tree -> tree.include("**/*-approved.*")));
        }
        return files;
    }

    private static boolean isWriteRun(Map<String, Object> properties) {
        for (String flag : WRITE_FLAGS) {
            Object value = properties.get(flag);
            if (value != null && TRUE_VALUES.contains(value.toString().trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> toolProperties(Project project, Map<String, String> commandLine) {
        Map<String, Object> properties = new TreeMap<>();
        Task test = project.getTasks().findByName("test");
        if (test instanceof Test) {
            Map<String, Object> testProperties = ((Test) test).getSystemProperties();
            for (String name : TOOL_PROPERTIES) {
                if (testProperties.containsKey(name)) {
                    properties.put(name, testProperties.get(name));
                }
            }
        }
        properties.putAll(commandLine);
        return properties;
    }

    private static void configureDedupTask(Project project, ApprovalcrestDedupTask task, Map<String, Object> toolProperties) {
        task.systemProperties(toolProperties);
        if (first(toolProperties, SOURCE_ROOT, SOURCE_ROOT_ALIAS) == null
                && !project.file("src/test/java").isDirectory() && project.file("src/test/kotlin").isDirectory()) {
            task.args("--dir", "src/test/kotlin");
        }
    }

    private static TaskProvider<ApprovalcrestDedupTask> registerDedupTask(Project project, String name, boolean reinstate,
                                                                         Configuration classpath, String description) {
        return project.getTasks().register(name, ApprovalcrestDedupTask.class, task -> {
            task.setGroup("verification");
            task.setDescription(description);
            task.setReinstate(reinstate);
            task.setClasspath(classpath);
            task.getMainClass().set("com.github.karsaig.approvalcrest.dedup.DedupCli");
            task.setWorkingDir(project.getProjectDir());
        });
    }

    private static Configuration toolClasspath(Project project, String name, String dependency, boolean transitive) {
        Configuration configuration = project.getConfigurations().create(name, c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.setTransitive(transitive);
            c.setDescription("The approvalcrest tool that the approvalcrest plugin runs.");
        });
        project.getDependencies().add(name, dependency);
        return configuration;
    }

    private static String first(Map<String, ?> properties, String name, String alias) {
        Object value = properties.get(name);
        if (value == null) {
            value = properties.get(alias);
        }
        return value == null ? null : value.toString();
    }

    private static String pluginVersion() {
        Properties properties = new Properties();
        try (InputStream in = ApprovalcrestPlugin.class.getResourceAsStream("approvalcrest-gradle-plugin.properties")) {
            if (in == null) {
                throw new IllegalStateException("approvalcrest-gradle-plugin.properties is missing from the plugin jar");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties.getProperty("version");
    }

    /** Adds {@code -javaagent} with the approvalcrest agent jar, when {@code attachAgent} is true at execution. */
    public static class AgentArgument implements CommandLineArgumentProvider {

        private final Property<Boolean> enabled;
        private final FileCollection agent;
        private final FileCollection none;

        public AgentArgument(Property<Boolean> enabled, FileCollection agent, FileCollection none) {
            this.enabled = enabled;
            this.agent = agent;
            this.none = none;
        }

        @Input
        public Property<Boolean> getEnabled() {
            return enabled;
        }

        @Classpath
        public FileCollection getAgent() {
            // Not resolved when the agent is off, so a build without access to the agent artifact still works.
            return enabled.get() ? agent : none;
        }

        @Override
        public Iterable<String> asArguments() {
            if (!enabled.get()) {
                return Collections.emptyList();
            }
            return Collections.singletonList("-javaagent:" + agent.getSingleFile().getAbsolutePath());
        }
    }

    /**
     * Puts the command-line properties back before the tests run: a later afterEvaluate may have set the same names,
     * and the command line must win.
     */
    public static class ApplyCommandLine implements Action<Task> {

        private final Map<String, String> commandLine;

        public ApplyCommandLine(Map<String, String> commandLine) {
            this.commandLine = new TreeMap<>(commandLine);
        }

        @Override
        public void execute(Task task) {
            ((Test) task).systemProperties(commandLine);
        }
    }

    /** Decides at execution, so that a flag set late in configuration counts. A class, so that the cache can store it. */
    public static class NotWriteRun implements Spec<Task> {
        @Override
        public boolean isSatisfiedBy(Task task) {
            return !isWriteRun(((Test) task).getSystemProperties());
        }
    }

    /** The opposite of {@link NotWriteRun}. */
    public static class WriteRun implements Spec<Task> {
        @Override
        public boolean isSatisfiedBy(Task task) {
            return isWriteRun(((Test) task).getSystemProperties());
        }
    }
}
