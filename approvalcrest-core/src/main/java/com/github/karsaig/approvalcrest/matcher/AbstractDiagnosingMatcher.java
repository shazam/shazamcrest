package com.github.karsaig.approvalcrest.matcher;

import com.github.karsaig.approvalcrest.BeanFinder;
import com.github.karsaig.approvalcrest.ComparisonDescription;
import com.github.karsaig.approvalcrest.JsonElementUtil;
import com.github.karsaig.approvalcrest.Either;
import com.github.karsaig.approvalcrest.MatcherConfiguration;
import com.github.karsaig.approvalcrest.PathNullPointerException;
import com.github.karsaig.approvalcrest.matcher.machinereadable.AliasTracker;
import com.github.karsaig.approvalcrest.matcher.machinereadable.IgnoredFieldsTracker;
import com.github.karsaig.approvalcrest.matcher.machinereadable.SortedFieldsTracker;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.github.karsaig.approvalcrest.BeanFinder.findBeanAt;
import static com.github.karsaig.approvalcrest.JsonElementUtil.collectValuesByFieldNamePattern;
import static com.github.karsaig.approvalcrest.JsonElementUtil.findJsonValueAt;
import static com.github.karsaig.approvalcrest.JsonElementUtil.jsonElementToJavaValue;
import static com.github.karsaig.approvalcrest.EnvVarReader.getBooleanProperties;

public abstract class AbstractDiagnosingMatcher<T> extends DiagnosingMatcher<T> {

    // Class name string — keeps this file compilable on Java 8 since InaccessibleObjectException
    // was introduced in Java 9.
    private static final String INACCESSIBLE_OBJECT_EXCEPTION =
            "java.lang.reflect.InaccessibleObjectException";
    private static final Pattern ADD_OPENS_PATTERN =
            Pattern.compile("module (\\S+) does not \"opens (\\S+)\"");

    private static final String MACHINE_READABLE_ALIAS = "fMMReadable";
    private static final String MACHINE_READABLE_AI_ALIAS = "fmAI";

    private boolean comparisonDescriptionNeeded = false;
    protected boolean machineReadableOutput = getBooleanProperties(null, "fileMatcherMachineReadable", MACHINE_READABLE_ALIAS, MACHINE_READABLE_AI_ALIAS);

    /**
     * Template-method entry point. Subclasses implement their matching logic here.
     * This is called by {@link #matches(Object, Description)}, which provides the
     * single catch-point for {@code InaccessibleObjectException}.
     */
    protected abstract boolean doMatches(Object actual, Description mismatchDescription);

    /**
     * Dispatches to {@link #doMatches} and intercepts any {@code InaccessibleObjectException}
     * (checked by class name so this compiles on Java 8) buried anywhere in the cause chain.
     * When found it throws an {@link IllegalStateException} with a human-readable explanation
     * and the exact {@code --add-opens} JVM flag the developer needs to configure.
     */
    @Override
    protected final boolean matches(Object actual, Description mismatchDescription) {
        try {
            return doMatches(actual, mismatchDescription);
        } catch (RuntimeException e) {
            Throwable inaccessible = findCause(e, INACCESSIBLE_OBJECT_EXCEPTION);
            if (inaccessible != null) {
                throw new IllegalStateException(buildAddOpensMessage(inaccessible), e);
            }
            throw e;
        }
    }

    private static Throwable findCause(Throwable t, String className) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (className.equals(cause.getClass().getName())) {
                return cause;
            }
        }
        return null;
    }

    private static String buildAddOpensMessage(Throwable inaccessible) {
        String target = extractAddOpensTarget(inaccessible.getMessage());
        String example = target != null ? target : "<module>/<package>=ALL-UNNAMED";
        return "approvalcrest could not access a field via reflection.\n"
                + "This happens on Java 9+ when the required '--add-opens' JVM argument is missing.\n"
                + (target != null ? "\nAdd the following JVM argument:\n  --add-opens " + target + "\n" : "")
                + "\nFor Maven (maven-surefire-plugin), add to pom.xml:\n"
                + "  <configuration>\n"
                + "    <argLine>--add-opens " + example + "</argLine>\n"
                + "  </configuration>\n"
                + "\nFor Gradle, add to your test block:\n"
                // Gradle gives each jvmArgs element to the JVM as one argument, and the JVM rejects "--add-opens x".
                + "  jvmArgs(\"--add-opens\", \"" + example + "\")\n"
                + "\nFor IDEs (IntelliJ / Eclipse), add to 'VM options' in the run/test configuration.\n"
                + "\nOriginal error: " + inaccessible.getMessage();
    }

    private static String extractAddOpensTarget(String message) {
        if (message == null) {
            return null;
        }
        java.util.regex.Matcher m = ADD_OPENS_PATTERN.matcher(message);
        return m.find() ? m.group(1) + "/" + m.group(2) + "=ALL-UNNAMED" : null;
    }

    protected boolean assertJsonEquals(String expectedJson, String actualJson, Description mismatchDescription, Function<Throwable, String> messageExtractor) {
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, true);
        } catch (AssertionError | JSONException e) {
            return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, messageExtractor.apply(e));
        }
        return true;
    }

    protected boolean assertJsonEquals(String expectedJson, String actualJson, Description mismatchDescription,
                                        Function<Throwable, String> messageExtractor,
                                        IgnoredFieldsTracker ignoredTracker, AliasTracker aliasTracker,
                                        SortedFieldsTracker sortedTracker, String note) {
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, true);
        } catch (AssertionError | JSONException e) {
            return appendMismatchDescriptionWithNote(mismatchDescription, expectedJson, actualJson, messageExtractor.apply(e),
                    ignoredTracker, aliasTracker, sortedTracker, note);
        }
        return true;
    }

    protected boolean appendMismatchDescription(Description mismatchDescription, String expected, String actual, String message) {
        return appendMismatchDescription(mismatchDescription, expected, actual, message, null, null, false);
    }

    protected boolean appendMismatchDescription(Description mismatchDescription, String expected, String actual,
                                                 String message, IgnoredFieldsTracker ignoredFieldsTracker,
                                                 AliasTracker aliasTracker, boolean typesIgnoredConfigured) {
        return appendMismatchDescriptionWithNote(mismatchDescription, expected, actual, message,
                ignoredFieldsTracker, aliasTracker, null,
                typesIgnoredConfigured ? "Type-based ignoring is configured but field-level tracking is not available for it." : null);
    }

    protected boolean appendMismatchDescriptionWithNote(Description mismatchDescription, String expected, String actual,
                                                         String message, IgnoredFieldsTracker ignoredFieldsTracker,
                                                         AliasTracker aliasTracker, SortedFieldsTracker sortedFieldsTracker,
                                                         String note) {
        if (comparisonDescriptionNeeded && ComparisonDescription.class.isInstance(mismatchDescription)) {
            ComparisonDescription shazamMismatchDescription = (ComparisonDescription) mismatchDescription;
            shazamMismatchDescription.setComparisonFailure(true);
            shazamMismatchDescription.setExpected(expected);
            shazamMismatchDescription.setActual(actual);
            shazamMismatchDescription.setDifferencesMessage(message);
            shazamMismatchDescription.setMachineReadable(machineReadableOutput);
            shazamMismatchDescription.setIgnoredFieldsTracker(ignoredFieldsTracker);
            shazamMismatchDescription.setAliasTracker(aliasTracker);
            shazamMismatchDescription.setSortedFieldsTracker(sortedFieldsTracker);
            if (note != null) {
                shazamMismatchDescription.setNote(note);
            }
        }
        mismatchDescription.appendText(message);
        return false;
    }

    protected void setComparisonDescriptionNeeded(boolean comparisonDescriptionNeeded) {
        this.comparisonDescriptionNeeded = comparisonDescriptionNeeded;
    }

    protected boolean isComparisonDescriptionNeeded() {
        return comparisonDescriptionNeeded;
    }

    protected String buildUntrackedNote(MatcherConfiguration config) {
        if (!machineReadableOutput) {
            return null;
        }
        boolean hasTypesToIgnore = !config.getTypesToIgnore().isEmpty();
        boolean hasPatterns = !config.getPatternsToIgnore().isEmpty();
        boolean hasTypesToSort = !config.getTypesToSort().isEmpty();

        boolean hasIgnoreNote = hasTypesToIgnore || hasPatterns;
        boolean hasSortNote = hasTypesToSort;

        if (!hasIgnoreNote && !hasSortNote) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (hasTypesToIgnore && hasPatterns) {
            sb.append("Type-based and pattern-based ignoring are configured but field-level tracking is not available for them.");
        } else if (hasTypesToIgnore) {
            sb.append("Type-based ignoring is configured but field-level tracking is not available for it.");
        } else if (hasPatterns) {
            sb.append("Pattern-based ignoring is configured but field-level tracking is not available for it.");
        }
        if (hasSortNote) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("Type-based sorting is configured but field-level tracking is not available for it.");
        }
        return sb.toString();
    }

    protected boolean areCustomMatchersMatchingBeanOrJson(Object actual, JsonElement actualAsJsonElement, Description mismatchDescription, Gson gson, MatcherConfiguration matcherConfiguration) {
        boolean hasCustomMatchers = !matcherConfiguration.getCustomMatchers().isEmpty();
        boolean hasCustomMatcherPatterns = !matcherConfiguration.getCustomMatcherPatterns().isEmpty();

        if (!hasCustomMatchers && !hasCustomMatcherPatterns) {
            return true;
        }

        if (hasCustomMatchers) {
            if (actual == null) {
                for (Map.Entry<String, Matcher<?>> entry : matcherConfiguration.getCustomMatchers().entrySet()) {
                    Matcher<?> matcher = entry.getValue();
                    if (!matchesWithoutCastFailure(matcher, null)) {
                        appendFieldPath(matcher, mismatchDescription, matcherConfiguration);
                        describeMismatchSafely(matcher, null, mismatchDescription);
                        appendFieldJsonSnippet(null, mismatchDescription, gson);
                        return false;
                    }
                }
            } else {
                List<FailEntry> retryList = new ArrayList<>();

                for (Map.Entry<String, Matcher<?>> entry : matcherConfiguration.getCustomMatchers().entrySet()) {
                    String path = entry.getKey();
                    Matcher<?> matcher = entry.getValue();
                    Either<RuntimeException, Object> beanResult = findBeanAt(path, actual);
                    if (beanResult.isRight()) {
                        Object beanValue = beanResult.getRight();
                        if (!matcherPassesOnValue(matcher, beanValue)) {
                            retryList.add(FailEntry.matcherFailed(path, matcher, reportValueFor(matcher, beanValue),
                                    isContainerValue(beanValue)));
                        }
                    } else {
                        retryList.add(FailEntry.beanPath(path, matcher, beanResult.getLeft()));
                    }
                }

                if (!retryList.isEmpty()) {
                    List<FailEntry> finalFailures = new ArrayList<>();

                    for (FailEntry retryEntry : retryList) {
                        if (retryEntry.beanVerdictIsFinal) {
                            finalFailures.add(retryEntry);
                            continue;
                        }
                        Either<RuntimeException, Object> jsonResult = findJsonValueAt(retryEntry.path, actualAsJsonElement);
                        if (jsonResult.isRight()) {
                            Object jsonValue = jsonResult.getRight();
                            if (!matcherPassesOnValue(retryEntry.matcher, jsonValue)) {
                                // Normally the bean value gives the better message -- <7> rather than <7L>. But
                                // when the matcher cannot describe it at all, keeping it would replace the real
                                // reason for the failure with a cast complaint about a boxing the user never wrote.
                                if (retryEntry.kind == FailEntry.Kind.MATCHER_FAILED
                                        && canDescribeMismatch(retryEntry.matcher, retryEntry.value)) {
                                    finalFailures.add(retryEntry);
                                } else {
                                    finalFailures.add(FailEntry.jsonFailed(retryEntry.path, retryEntry.matcher, reportValueFor(retryEntry.matcher, jsonValue)));
                                }
                            }
                        } else {
                            finalFailures.add(retryEntry);
                        }
                    }

                    if (!finalFailures.isEmpty()) {
                        FailEntry first = finalFailures.get(0);
                        if (first.kind == FailEntry.Kind.BEAN_PATH) {
                            RuntimeException e = first.exception;
                            if (e instanceof PathNullPointerException) {
                                mismatchDescription.appendText(String.format("%s is null", ((PathNullPointerException) e).getPath()));
                                return false;
                            }
                            throw e;
                        }
                        appendFieldPath(first.matcher, mismatchDescription, matcherConfiguration);
                        describeMismatchSafely(first.matcher, first.value, mismatchDescription);
                        appendFieldJsonSnippet(first.value, mismatchDescription, gson);
                        return false;
                    }
                }
            }
        }

        if (hasCustomMatcherPatterns && actual != null
                && actualAsJsonElement != null && !actualAsJsonElement.isJsonNull()) {
            for (AbstractMap.SimpleEntry<Matcher<String>, Matcher<?>> entry : matcherConfiguration.getCustomMatcherPatterns()) {
                Matcher<String> fieldNamePattern = entry.getKey();
                Matcher<?> valueMatcher = entry.getValue();
                List<JsonElement> matchingValues = collectValuesByFieldNamePattern(actualAsJsonElement, fieldNamePattern);
                for (JsonElement je : matchingValues) {
                    // Same presentation as the path-based matchers get, so the two agree on a
                    // collection-valued field: hasSize works here as it does in with(path, matcher).
                    Object value = asMatchableValue(jsonElementToJavaValue(je));
                    if (!matchesWithoutCastFailure(valueMatcher, value)) {
                        mismatchDescription.appendDescriptionOf(fieldNamePattern).appendText(" ");
                        describeMismatchSafely(valueMatcher, value, mismatchDescription);
                        appendFieldJsonSnippet(value, mismatchDescription, gson);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Returns true when the matcher passes on {@code value}.
     * For a {@link BeanFinder.FanoutResult} every element must pass (recursively).
     * An <em>empty</em> FanoutResult returns {@code false}: there are no elements to validate
     * against, so the matcher cannot be considered satisfied (avoids vacuous-truth false positives
     * when the collection at the path is empty).
     */
    private static boolean matcherPassesOnValue(Matcher<?> matcher, Object value) {
        if (value instanceof BeanFinder.FanoutResult) {
            BeanFinder.FanoutResult fanout = (BeanFinder.FanoutResult) value;
            if (fanout.isEmpty()) {
                return false;
            }
            for (Object element : fanout) {
                if (!matcherPassesOnValue(matcher, element)) {
                    return false;
                }
            }
            return true;
        }
        return matchesWithoutCastFailure(matcher, asMatchableValue(value));
    }

    /**
     * Applies the matcher, treating a failed cast as "cannot compare, so does not match".
     *
     * <p>Hamcrest's ordering matchers resolve their type parameter to {@code Object}, so nothing is rejected on
     * a type check and the cast inside {@code compareTo} is what fails. A bare ordering matcher catches that
     * itself and answers false, but several combinators -- {@code allOf}, {@code both().and()}, {@code hasItem},
     * {@code everyItem}, {@code contains} -- call the inner matcher's {@code describeMismatch} from inside their
     * own {@code matches}, where nothing catches it, so the exception escaped the whole assertion.
     *
     * <p>False is the answer the bare matcher already gives for the same pairing, so this makes the composed
     * forms agree with it rather than changing what either means. It also lets the JSON retry run, which is what
     * rescues an {@code int}-valued field compared against a {@code Long}-boxed matcher.
     *
     * <p>The boundary, stated exactly: no assertion that passed now fails, and none that failed now passes. Only
     * configurations that previously <em>errored</em> change, and they settle on whichever verdict the retry
     * reaches. The cast is not lost -- {@link #describeMismatchSafely} re-runs the matcher and names it in the
     * failure message, so a caller's own mistyped matcher still says so rather than reading as a value mismatch.
     */
    private static boolean matchesWithoutCastFailure(Matcher<?> matcher, Object matchable) {
        try {
            return matcher.matches(matchable);
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Describes why {@code value} did not match, without letting the description itself fail.
     *
     * <p>A Hamcrest ordering matcher catches the failed cast inside {@code matchesSafely} and answers false,
     * but {@code describeMismatch} does not -- so {@code greaterThan(0)} against a {@code Long} threw
     * {@code ClassCastException} while building the message for a mismatch it had already decided. The
     * assertion failed either way; the exception replaced the explanation with a stack trace naming Hamcrest
     * internals rather than the field.
     *
     * <p>Whole numbers reach a matcher as {@code Long} whenever the value comes from the serialised JSON rather
     * than the object, so this is reachable from any ordering matcher written with an {@code int} literal. The
     * verdict is unchanged and remains the documented one -- the matcher's number has to be written in the same
     * form as the value's -- but the message now says so.
     */
    protected static void describeMismatchSafely(Matcher<?> matcher, Object value, Description mismatchDescription) {
        // A cast can fail in either half of the matcher, and both have to reach the message. matches() failing
        // is swallowed upstream so the JSON retry can run, which would otherwise lose the only evidence that
        // anything went wrong -- including a genuine bug in a caller's own matcher.
        ClassCastException castFailure = castFailureFrom(matcher, value);
        // Into a scratch buffer: a matcher may append part of its text before its own cast fails, and that
        // fragment would otherwise be left stranded in front of the replacement -- "was was <7L>".
        StringDescription scratch = new StringDescription();
        try {
            matcher.describeMismatch(value, scratch);
            mismatchDescription.appendText(scratch.toString());
        } catch (ClassCastException e) {
            if (castFailure == null) {
                castFailure = e;
            }
            mismatchDescription.appendText("was ").appendValue(value);
        }
        if (castFailure != null) {
            mismatchDescription.appendText(" -- this matcher could not compare it: ")
                    .appendText(String.valueOf(castFailure.getMessage()));
            if (value instanceof Number) {
                // Much the commonest cause, and the only one with a one-line remedy. Naming both types is the
                // useful part, so the hint stays generic rather than assuming a Long.
                mismatchDescription.appendText(". Write the matcher's number in the same form as the value's"
                        + " -- a whole number read from the serialised output is a Long."
                        + " See docs/custom-matching.md");
            }
            mismatchDescription.appendText(".");
        }
    }

    /** The cast failure this matcher raises for this value, or null when it answers normally. */
    private static ClassCastException castFailureFrom(Matcher<?> matcher, Object value) {
        try {
            matcher.matches(value);
            return null;
        } catch (ClassCastException e) {
            return e;
        }
    }

    /**
     * Whether this matcher can describe a mismatch against this value.
     *
     * <p>Catches every {@code RuntimeException}, not just the cast failure this was written for. It runs over
     * entries that are only candidates for reporting, and one of them throwing must not decide the outcome of
     * an assertion whose failure lies elsewhere -- before this existed such an entry was simply never described.
     */
    private static boolean canDescribeMismatch(Matcher<?> matcher, Object value) {
        try {
            matcher.describeMismatch(value, new StringDescription());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Presents a value in the form Hamcrest's container matchers expect: a {@code JsonArray} becomes a
     * list view, anything else passes through untouched. Applied to every value handed to a matcher,
     * whichever walk produced it.
     * <p>
     * A {@code JsonArray} arrives when the path could not be resolved against the object under
     * comparison — a raw JSON string input, or an array segment the bean walker does not traverse — so
     * the value comes from the serialised JSON instead. That is an {@code Iterable} but not a
     * {@code Collection}, so {@code hasSize} and {@code empty} answer false on the type check alone
     * whatever the array holds, and their negations are correspondingly true whatever it holds.
     * A {@link JsonElementUtil#asList(JsonArray) list view} lets them answer for real.
     * <p>
     * The view copies nothing, and it coerces a scalar element on read, so an element matcher written
     * against the value — {@code hasItem("a")}, {@code contains(1L, 2L)} — matches a collection of
     * scalars as it would on the object. An element that is itself an object or an array is handed
     * back unchanged: JSON carries no type information, so a matcher written against the element's
     * own class cannot match it either way.
     */
    private static Object asMatchableValue(Object value) {
        return value instanceof JsonArray ? JsonElementUtil.asList((JsonArray) value) : value;
    }

    /**
     * Returns true when the resolved bean value is a {@code Collection}, {@code Map} or array — a
     * container the path terminated at, rather than something it fanned out through — in which case
     * the serialised JSON form cannot give a better answer than the object already did.
     * <p>
     * Re-running such a matcher against the serialised JSON can only make the verdict worse: the
     * JSON form of a collection is a {@code JsonArray} and of a map a {@code JsonArray} of
     * single-entry objects, neither of which is a {@code Collection} or {@code Map}, so a container
     * matcher is false there on type grounds regardless of content — and its negation
     * correspondingly true. Retrying is only useful when the bean walker could not resolve the path
     * at all, or when the value is a scalar whose JSON form differs usefully (a Gson number is a
     * {@code Long} where the bean holds an {@code int}).
     * <p>
     * A {@link BeanFinder.FanoutResult} is deliberately excluded: it means the path fanned out
     * <em>through</em> a collection rather than terminating at one, so the matcher applies to the
     * leaf values and scalar coercion still matters.
     */
    private static boolean isContainerValue(Object value) {
        if (value instanceof BeanFinder.FanoutResult) {
            return false;
        }
        return value instanceof Collection || value instanceof Map || (value != null && value.getClass().isArray());
    }

    /**
     * Returns the value to store in a {@link FailEntry} for error reporting.
     * For a {@link BeanFinder.FanoutResult} we descend to the first failing leaf
     * so the mismatch description names the concrete bad value, not the whole list.
     */
    private static Object reportValueFor(Matcher<?> matcher, Object value) {
        if (value instanceof BeanFinder.FanoutResult) {
            for (Object element : (BeanFinder.FanoutResult) value) {
                if (!matcherPassesOnValue(matcher, element)) {
                    return reportValueFor(matcher, element);
                }
            }
        }
        return asMatchableValue(value);
    }

    /**
     * Append the custom matchers to a description, after the expected content.
     *
     * <p>The two modes need different wording. A matcher registered with {@code with(...)} replaces the field's
     * comparison, so the field is absent from the content above and the clause is the only thing said about it.
     * One registered with {@code alsoCheck(...)} leaves the field in that content, so the same clause would be
     * ambiguous -- "and also" marks it as an extra constraint on a value already shown.
     *
     *
     * <p>The branch reads the registration, not the rendered content, so it recognises the two rules that name
     * the path itself -- the replacing mode and {@code ignoring(path)}. It cannot see a field removed for some
     * other reason: an ignored <em>parent</em>, an ignored type, or a name pattern. In those cases an additional
     * matcher still reads "and also" while its field is absent from the content above. That affects the
     * description only, never a verdict.
     */
    protected void describeCustomMatchers(Description description, MatcherConfiguration matcherConfiguration) {
        // One pass over the map, branching per entry, so the order of the replacing clauses is exactly what it
        // was before the additional mode existed.
        Set<String> removedPaths = matcherConfiguration.getCustomMatcherPathsToIgnore();
        Set<String> explicitlyIgnored = matcherConfiguration.getPathsToIgnore();
        for (Map.Entry<String, Matcher<?>> entry : matcherConfiguration.getCustomMatchers().entrySet()) {
            String fieldPath = entry.getKey();
            boolean stillCompared = !removedPaths.contains(fieldPath) && !explicitlyIgnored.contains(fieldPath);
            description.appendText(stillCompared ? "\nand also " : "\nand ")
                    .appendText(fieldPath).appendText(" ")
                    .appendDescriptionOf(entry.getValue());
        }
    }

    protected void appendFieldPath(Matcher<?> matcher, Description mismatchDescription, MatcherConfiguration matcherConfiguration) {
        for (Map.Entry<String, Matcher<?>> entry : matcherConfiguration.getCustomMatchers().entrySet()) {
            if (entry.getValue().equals(matcher)) {
                mismatchDescription.appendText(entry.getKey()).appendText(" ");
            }
        }
    }

    protected void appendFieldJsonSnippet(Object actual, Description mismatchDescription, Gson gson) {
        JsonElement jsonTree = gson.toJsonTree(actual);
        if (!jsonTree.isJsonPrimitive() && !jsonTree.isJsonNull()) {
            mismatchDescription.appendText("\n" + gson.toJson(actual));
        }
    }

    private static class FailEntry {
        enum Kind { BEAN_PATH, MATCHER_FAILED, JSON_FAILED }

        final Kind kind;
        final String path;
        final Matcher<?> matcher;
        final Object value;
        final RuntimeException exception;
        /** True when the bean value already settles the verdict, so the JSON retry must not run. */
        final boolean beanVerdictIsFinal;

        static FailEntry beanPath(String path, Matcher<?> matcher, RuntimeException e) {
            return new FailEntry(Kind.BEAN_PATH, path, matcher, null, e, false);
        }

        static FailEntry matcherFailed(String path, Matcher<?> matcher, Object value, boolean beanVerdictIsFinal) {
            return new FailEntry(Kind.MATCHER_FAILED, path, matcher, value, null, beanVerdictIsFinal);
        }

        static FailEntry jsonFailed(String path, Matcher<?> matcher, Object value) {
            return new FailEntry(Kind.JSON_FAILED, path, matcher, value, null, false);
        }

        private FailEntry(Kind kind, String path, Matcher<?> matcher, Object value, RuntimeException exception,
                          boolean beanVerdictIsFinal) {
            this.kind = kind;
            this.path = path;
            this.matcher = matcher;
            this.value = value;
            this.exception = exception;
            this.beanVerdictIsFinal = beanVerdictIsFinal;
        }
    }
}
