package com.github.karsaig.approvalcrest.matcher;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.karsaig.approvalcrest.EnvVarReader;

public abstract class AbstractTestMetaBase implements TestMetaInformation {

    protected static final String SRC_TEST_JAVA_PATH = "src" + File.separator + "test" + File.separator + "java" + File.separator;
    private static final String SOURCE_ROOT_NAME = "fileMatcherSourceRoot";
    private static final String SOURCE_ROOT_ALIAS = "fmSourceRoot";
    protected static final Pattern DOT_LITERAL_PATTERN = Pattern.compile(".", Pattern.LITERAL);

    private final Path testClassPath;
    private final String testClassName;
    private final String testMethodName;
    private final Path workingDirectory;

    protected AbstractTestMetaBase(String testClassName, String testMethodName) {
        this(buildClassPath(testClassName), testClassName, testMethodName);
    }

    /**
     * @param defaultSourceRoot the test source root to use when {@code fileMatcherSourceRoot} is not
     *                          set. Frameworks whose tests live somewhere other than
     *                          {@code src/test/java} — Kotlin, for instance — supply their own
     *                          default here; the property still overrides it when present.
     */
    protected AbstractTestMetaBase(String testClassName, String testMethodName, String defaultSourceRoot) {
        this(buildClassPath(testClassName, getSourceRoutePathString(defaultSourceRoot)), testClassName, testMethodName);
    }

    protected AbstractTestMetaBase(Path testClassPath, String testClassName, String testMethodName) {
        this(detectWorkingDirectory(), testClassPath, testClassName, testMethodName);
    }

    /**
     * The working directory comes first: a {@code (Path, String, String, Path)} call written for the removed
     * approved-directory constructor must fail to compile, not pass that path on as the working directory.
     */
    protected AbstractTestMetaBase(Path workingDirectory, Path testClassPath, String testClassName, String testMethodName) {
        this.testClassPath = testClassPath;
        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
        this.workingDirectory = workingDirectory;
    }

    protected static Path buildClassPath(String testClassName) {
        return buildClassPath(testClassName, getSourceRoutePathString());
    }

    protected static Path buildClassPath(String testClassName, String sourceRoutePathString) {
        return Paths.get(sourceRoutePathString + DOT_LITERAL_PATTERN.matcher(testClassName).replaceAll(Matcher.quoteReplacement(File.separator))).getParent();
    }

    protected static Path detectWorkingDirectory() {
        return Paths.get("").toAbsolutePath();
    }

    protected static String getSourceRoutePathString() {
        return getSourceRoutePathString(SRC_TEST_JAVA_PATH);
    }

    /**
     * Resolves the test source root, falling back to the given default when
     * {@code fileMatcherSourceRoot} (alias {@code fmSourceRoot}) is not set.
     *
     * <p>The property is shared by every framework; only the fallback differs, so a project that
     * does not set it keeps the layout its framework expects.
     */
    protected static String getSourceRoutePathString(String defaultSourceRoot) {
        String configured = EnvVarReader.getStringProperties(defaultSourceRoot, SOURCE_ROOT_NAME, SOURCE_ROOT_ALIAS);
        return normalizeSourceRoot(configured);
    }

    private static String normalizeSourceRoot(String sourceRoot) {
        String normalized = sourceRoot.replace('/', File.separatorChar).replace('\\', File.separatorChar);
        if (!normalized.endsWith(File.separator)) {
            normalized = normalized + File.separator;
        }
        return normalized;
    }

    protected static StackTraceElement findTestStackTraceElement(StackTraceElement[] stackTrace, Predicate<StackTraceElement> isTestMethod) {
        for (StackTraceElement s : stackTrace) {
            if (isTestMethod.test(s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns every method with the given name declared on the class or any of its superclasses.
     *
     * <p>Declared methods are used rather than {@link Class#getMethods()} because that returns only
     * public members, while JUnit Jupiter and TestNG both allow package-private test methods.
     *
     * <p>All matches are returned rather than the first one because a test method can share its
     * name with an overload, and the reflection order is unspecified — so callers must be able to
     * check every candidate for the test annotation instead of guessing which one is the test.
     */
    protected static List<Method> findMethods(Class<?> clazz, String methodName) {
        List<Method> result = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    /**
     * Rejects a test class name that cannot possibly be the class being run.
     *
     * <p>A test method inherited from a base class reports the <em>declaring</em> class, not the
     * subclass actually running it, because that is what a stack frame and
     * {@code Method#getDeclaringClass()} both describe. Every subclass therefore resolves to one
     * approved file and they silently share a golden master.
     *
     * <p>The running subclass cannot be recovered from either source, so the only honest check is
     * for the case where the reported class is provably wrong: an abstract class is never itself
     * the test class. A concrete base with subclasses has the same problem but is indistinguishable
     * from an ordinary test class, so it is documented rather than guessed at.
     *
     * @param testClassName the class name derived from the stack frame or declaring class
     * @param howToFix      framework-specific instruction for supplying the running test class
     */
    protected static void requireConcreteTestClass(String testClassName, String howToFix) {
        Class<?> clazz;
        try {
            clazz = Class.forName(testClassName);
        } catch (Throwable e) {
            // Cannot inspect it; leave the name as-is rather than failing on a resolution problem.
            return;
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalStateException("Cannot determine which test class is running: the test method is"
                    + " declared in the abstract class " + testClassName + ", so the subclass actually running it"
                    + " is not recoverable here. Every subclass would resolve to the same approved file and"
                    + " overwrite each other.\n" + howToFix);
        }
    }

    @Override
    public Path getTestClassPath() {
        return testClassPath;
    }

    @Override
    public String testClassName() {
        return testClassName;
    }

    @Override
    public String testMethodName() {
        return testMethodName;
    }

    @Override
    public Path workingDirectory() {
        return workingDirectory;
    }

    @Override
    public String toString() {
        return "TestMeta[cn=" + testClassName +
                ",mn=" + testMethodName +
                ",cp=" + testClassPath +
                ",wd=" + workingDirectory + "]";
    }
}
