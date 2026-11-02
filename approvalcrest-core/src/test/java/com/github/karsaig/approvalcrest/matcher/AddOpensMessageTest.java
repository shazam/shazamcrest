package com.github.karsaig.approvalcrest.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

/**
 * When reflection fails for a missing {@code --add-opens}, the error gives the flag for each build tool.
 *
 * <p>The Gradle form must give the option and its value as two {@code jvmArgs} elements: Gradle passes
 * each element to the JVM as one argument, and the JVM does not start with {@code "--add-opens x"}.
 */
public class AddOpensMessageTest {

    private static final String JAVA_TIME_ERROR = "Unable to make field private final java.lang.String java.time.ZoneRegion.id"
            + " accessible: module java.base does not \"opens java.time\" to unnamed module @1b2c3d4e";

    @Test
    public void messageGivesTheFlagThatTheJdkNames() throws Exception {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> matcherThatThrows(inaccessibleObjectException(JAVA_TIME_ERROR)).matches("actual"));

        assertEquals("approvalcrest could not access a field via reflection.\n"
                + "This happens on Java 9+ when the required '--add-opens' JVM argument is missing.\n"
                + "\n"
                + "Add the following JVM argument:\n"
                + "  --add-opens java.base/java.time=ALL-UNNAMED\n"
                + "\n"
                + "For Maven (maven-surefire-plugin), add to pom.xml:\n"
                + "  <configuration>\n"
                + "    <argLine>--add-opens java.base/java.time=ALL-UNNAMED</argLine>\n"
                + "  </configuration>\n"
                + "\n"
                + "For Gradle, add to your test block:\n"
                + "  jvmArgs(\"--add-opens\", \"java.base/java.time=ALL-UNNAMED\")\n"
                + "\n"
                + "For IDEs (IntelliJ / Eclipse), add to 'VM options' in the run/test configuration.\n"
                + "\n"
                + "Original error: " + JAVA_TIME_ERROR, e.getMessage());
    }

    @Test
    public void messageGivesAPlaceholderWhenTheJdkNamesNoPackage() throws Exception {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> matcherThatThrows(inaccessibleObjectException("Unable to make field accessible")).matches("actual"));

        assertEquals("approvalcrest could not access a field via reflection.\n"
                + "This happens on Java 9+ when the required '--add-opens' JVM argument is missing.\n"
                + "\n"
                + "For Maven (maven-surefire-plugin), add to pom.xml:\n"
                + "  <configuration>\n"
                + "    <argLine>--add-opens <module>/<package>=ALL-UNNAMED</argLine>\n"
                + "  </configuration>\n"
                + "\n"
                + "For Gradle, add to your test block:\n"
                + "  jvmArgs(\"--add-opens\", \"<module>/<package>=ALL-UNNAMED\")\n"
                + "\n"
                + "For IDEs (IntelliJ / Eclipse), add to 'VM options' in the run/test configuration.\n"
                + "\n"
                + "Original error: Unable to make field accessible", e.getMessage());
    }

    /** The class is only on Java 9 and later, and the tests compile for Java 8. */
    private static RuntimeException inaccessibleObjectException(String message) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("java.lang.reflect.InaccessibleObjectException");
        } catch (ClassNotFoundException e) {
            assumeTrue(false, "Java 8 has no module system");
            throw e;
        }
        return (RuntimeException) type.getConstructor(String.class).newInstance(message);
    }

    private static AbstractDiagnosingMatcher<Object> matcherThatThrows(RuntimeException inaccessible) {
        return new AbstractDiagnosingMatcher<Object>() {
            @Override
            protected boolean doMatches(Object actual, Description mismatchDescription) {
                throw new RuntimeException("serialisation failed", inaccessible);
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }
}
