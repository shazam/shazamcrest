package com.github.karsaig.approvalcrest.matcher;


import com.github.karsaig.approvalcrest.ComparisonDescription;
import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.testdata.BeanWithPrimitives;
import com.github.karsaig.approvalcrest.util.*;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.DIRECTORY_CREATE_PERMISSONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.FILE_CREATE_PERMISSONS;
import static com.github.karsaig.approvalcrest.util.InMemoryFsUtil.TESTED_OS_CONFIGS;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Abstract class for common methods used by the JsonMatcher tests.
 *
 * @author Andras_Gyuro
 */
public abstract class AbstractFileMatcherTest extends AbstractTest {

    protected static final TestMatcherFactory MATCHER_FACTORY = new TestMatcherFactory();
    private static final Pattern NOT_APPROVED_PATTERN = Pattern.compile("-not", Pattern.LITERAL);

    protected BeanWithPrimitives getBeanWithPrimitives() {
        return PreBuilt.getBeanWithPrimitives();
    }

    protected String getBeanAsJsonString() {
        return "{ beanLong: 5, beanString: \"dummyString\", beanInt: 10  }";
    }

    protected String getBeanWithPrimitivesAsJsonString() {
        return PreBuilt.getBeanWithPrimitivesAsJsonString();
    }

    protected void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, boolean result) {
        inMemoryUnixFs(imfsi -> {
            try {
                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                Path testFile = Files.createDirectories(jsonDir).resolve("11b2ef-approved.json");
                Files.write(testFile, ImmutableList.of(expectedFileContent), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            JsonMatcher<Object> jsonMatcher = MATCHER_FACTORY.jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            if (jsonMatcher.matches(input) != result) {
                handleAssertFailure(input, jsonMatcher);
            }
        });
    }

    protected void runContentMatcherTestWithDummyTestInfo(String expectedFileContent, Consumer<TestMetaInformation> actualTest) {
        runTestWithDummyTestInfo(expectedFileContent, "content", actualTest);
    }

    protected void runJsonMatcherTestWithDummyTestInfo(String expectedFileContent, Consumer<TestMetaInformation> actualTest) {
        runTestWithDummyTestInfo(expectedFileContent, "json", actualTest);
    }

    protected void runTestWithDummyTestInfo(String expectedFileContent, String extension, Consumer<TestMetaInformation> actualTest) {
        inMemoryUnixFs(imfsi -> {
            try {
                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                Path testFile = Files.createDirectories(jsonDir).resolve("11b2ef-approved." + extension);
                Files.write(testFile, expectedFileContent.getBytes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            TestMetaInformation dummyTestInfo = dummyInformation(imfsi);
            actualTest.accept(dummyTestInfo);
        });
    }


    protected void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, String expectedExceptionMessage) {
        assertJsonMatcherWithDummyTestInfo(input, expectedFileContent, getDefaultFileMatcherConfig(), expectedExceptionMessage);
    }

    protected void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, FileMatcherConfig initialConfig, String expectedExceptionMessage) {
        assertJsonMatcherWithDummyTestInfo(input, expectedFileContent, initialConfig, Function.identity(), expectedExceptionMessage);
    }

    protected void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, Function<JsonMatcher<Object>, JsonMatcher<Object>> configurator, String expectedExceptionMessage) {
        assertJsonMatcherWithDummyTestInfo(input, expectedFileContent, getDefaultFileMatcherConfig(), configurator, expectedExceptionMessage);
    }

    protected void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, FileMatcherConfig initialConfig, Function<JsonMatcher<Object>, JsonMatcher<Object>> configurator, String expectedExceptionMessage) {
        assertJsonMatcherWithDummyTestInfo(input, expectedFileContent, initialConfig, configurator, expectedExceptionMessage == null ? null : error -> Assertions.assertEquals(expectedExceptionMessage, removeAiTip(error.getMessage())), AssertionError.class);
    }

    protected <T extends Throwable> void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, Function<JsonMatcher<Object>, JsonMatcher<Object>> configurator, Consumer<T> exceptionHandler, Class<T> clazz) {
        assertJsonMatcherWithDummyTestInfo(input, expectedFileContent, getDefaultFileMatcherConfig(), configurator, exceptionHandler, clazz);
    }

    protected <T extends Throwable> void assertJsonMatcherWithDummyTestInfo(Object input, String expectedFileContent, FileMatcherConfig initialConfig, Function<JsonMatcher<Object>, JsonMatcher<Object>> configurator, Consumer<T> exceptionHandler, Class<T> clazz) {
        inMemoryUnixFs(imfsi -> {
            try {
                Path jsonDir = imfsi.getTestPath().resolve("4ac405");
                Path testFile = Files.createDirectories(jsonDir).resolve("11b2ef-approved.json");
                Files.write(testFile, ImmutableList.of(expectedFileContent), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<InMemoryPermissions> beforeFileState = InMemoryFsUtil.getPermissons(imfsi);
            JsonMatcher<Object> matcherWithDefaultConfig = MATCHER_FACTORY.jsonMatcher(dummyInformation(imfsi), initialConfig);
            JsonMatcher<Object> jsonMatcher = configurator.apply(matcherWithDefaultConfig);
            if (exceptionHandler == null) {
                assertThat(input, jsonMatcher);
            } else {
                T actualError = assertThrows(clazz,
                        () -> assertThat(input, jsonMatcher));

                exceptionHandler.accept(actualError);
            }
            assertJsonMatcherWithDummyTestInfo(imfsi, expectedFileContent + System.lineSeparator(), "4ac405/11b2ef-approved.json");
            List<InMemoryPermissions> afterFileState = InMemoryFsUtil.getPermissons(imfsi);
            assertIterableEquals(beforeFileState, afterFileState);
        });
    }

    private void handleAssertFailure(Object input, JsonMatcher<Object> jsonMatcher) {
        ComparisonDescription description = new ComparisonDescription();
        description.appendDescriptionOf(jsonMatcher);
        jsonMatcher.describeMismatch(input, description);
        comparisonDescriptionHandler().accept("Actual doesn't match expected!", description);
    }

    protected void assertJsonMatcherWithDummyTestInfoForNotApprovedFile(Object input, String expectedFileContent, Function<JsonMatcher<Object>, JsonMatcher<Object>> configurator) {
        inMemoryUnixFs(imfsi -> {
            JsonMatcher<Object> matcherWithDefaultConfig = MATCHER_FACTORY.jsonMatcher(dummyInformation(imfsi), getDefaultFileMatcherConfig());
            JsonMatcher<Object> jsonMatcher = configurator.apply(matcherWithDefaultConfig);
            AssertionError actualError = assertThrows(AssertionError.class,
                    () -> assertThat(input, jsonMatcher));

            Assertions.assertEquals(getNotApprovedCreationMessage("4ac405", "11b2ef-not-approved.json", "11b2ef-approved.json"), actualError.getMessage());
            assertJsonMatcherWithDummyTestInfo(imfsi, "/*dummyTestClassName.dummyTestMethodName*/\n" + expectedFileContent, "4ac405/11b2ef-not-approved.json");

            List<InMemoryPermissions> actualFiles = InMemoryFsUtil.getPermissons(imfsi);
            List<InMemoryPermissions> expected = new ArrayList<>();
            expected.add(new InMemoryPermissions("4ac405", DIRECTORY_CREATE_PERMISSONS));
            expected.add(new InMemoryPermissions("4ac405/11b2ef-not-approved.json", FILE_CREATE_PERMISSONS));
            assertIterableEquals(expected, actualFiles);
        });
    }

    protected String getExcceptionMessageForDummyTestInfo(String differentPart) {
        return "Expected file 4ac405/11b2ef-approved.json\n" + differentPart;
    }

    private void assertJsonMatcherWithDummyTestInfo(InMemoryFsInfo imfsi, String expectedFileContent, String path) {
        List<InMemoryFiles> actualFiles = getFiles(imfsi);
        List<InMemoryFiles> expected = singletonList(new InMemoryFiles(path, expectedFileContent));

        assertIterableEquals(expected, actualFiles);
    }

    protected void inMemoryUnixFs(Consumer<InMemoryFsInfo> test) {
        InMemoryFsUtil.inMemoryUnixFsWithFileAttributeSupport(test);
    }

    protected void inMemoryWindowsFs(Consumer<InMemoryFsInfo> test) {
        InMemoryFsUtil.inMemoryWindowsFs(test);
    }

    protected void inMemoryFs(Configuration config, Consumer<InMemoryFsInfo> test) {
        InMemoryFsUtil.inMemoryFs(config, test);
    }

    public static Stream<Arguments> supportedOsPermutations(Object[][] input){
        return Arrays.stream(input).flatMap(in -> TESTED_OS_CONFIGS.stream().map(os -> {
            Object[] newArray = new Object[in.length + 1];
            newArray[0] = os;
            System.arraycopy(in, 0, newArray, 1, in.length);
            return Arguments.of(newArray);
        }));
    }

    protected class DummyInformation implements TestMetaInformation {

        private final Path path;
        private final String testClassName;
        private final String testMethodName;
        private final Path workingDirectory;

        public DummyInformation(InMemoryFsInfo imfsi) {
            this(imfsi.getTestPath(), imfsi.getWorkingDirectory());
        }

        public DummyInformation(Path path, Path workingDirectory) {
            this(path, "dummyTestClassName", "dummyTestMethodName", workingDirectory);
        }

        public DummyInformation(Path path, String testClassName, String testMethodName, Path workingDirectory) {
            this.path = path;
            this.testClassName = testClassName;
            this.testMethodName = testMethodName;
            this.workingDirectory = workingDirectory;
        }


        @Override
        public Path getTestClassPath() {
            return path;
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


    }


    protected DummyInformation dummyInformation(InMemoryFsInfo imfsi) {
        return new DummyInformation(imfsi);
    }

    protected DummyInformation dummyInformation(InMemoryFsInfo imfsi, String testClassName, String testMethodName) {
        return new DummyInformation(imfsi.getTestPath(), testClassName, testMethodName, imfsi.getWorkingDirectory());
    }

    protected List<InMemoryFiles> getFiles(InMemoryFsInfo imfsi) {
        return InMemoryFsUtil.getFiles(imfsi);
    }

    protected List<InMemoryFiles> getFiles(FileSystem fs) {
        return InMemoryFsUtil.getFiles(fs);
    }

    protected String readFile(Path path) {
        return InMemoryFsUtil.readFile(path);
    }

    protected String getNotApprovedCreationMessage(String classHash, String createdFile, String renameTo) {
        return getNotApprovedCreationMessage(classHash + File.separator + createdFile, renameTo);
    }

    protected String getNotApprovedCreationMessage(String createdFile, String renameTo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Not approved file created: '");
        builder.append(createdFile);
        builder.append("';\n please verify its contents and rename it to '");
        builder.append(renameTo);
        builder.append("'.");
        builder.append("\n[AI tip] Re-run with fmAI=true for the absolute path of the file that needs to be renamed.");
        return builder.toString();
    }

    protected String getMachineReadableNotApprovedCreationMessage(Path notApprovedAbsolutePath, Path approvedAbsolutePath) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("failureType", "NEW_FILE");
        json.addProperty("test", "dummyTestClassName#dummyTestMethodName");
        json.addProperty("notApprovedFile", notApprovedAbsolutePath.toAbsolutePath().toString());
        json.addProperty("approveTo", approvedAbsolutePath.toAbsolutePath().toString());
        json.addProperty("action", "Rename (or copy) the notApprovedFile to the approveTo path above");
        return new com.google.gson.GsonBuilder().disableHtmlEscaping().create().toJson(json);
    }

    protected void writeFile(Path path, String content) {
        InMemoryFsUtil.writeFile(path, content);
    }


    protected Path approveFile(Path from) {
        try {
            Path to = from.getParent().resolve(NOT_APPROVED_PATTERN.matcher(from.getFileName().toString()).replaceAll(Matcher.quoteReplacement("")));
            return Files.move(from, to);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileMatcherConfig getDefaultFileMatcherConfig() {
        return new FileMatcherConfig(false, false, false, true);
    }

    public static FileMatcherConfig getDefaultFileMatcherConfigWithLenientMatching() {
        return new FileMatcherConfig(false, false, false, false);
    }

    public static FileMatcherConfig enableInPlaceOverwrite() {
        return new FileMatcherConfig(true, false, false, true);
    }

    public static FileMatcherConfig enableInPlaceOverwriteSkippingCustomMatchers() {
        return enableInPlaceOverwrite().withSkipCustomMatchersOnUpdate(true);
    }

    /** The skip asked for without an update running: it has to stay inert. */
    public static FileMatcherConfig skipCustomMatchersWithoutInPlaceOverwrite() {
        return getDefaultFileMatcherConfig().withSkipCustomMatchersOnUpdate(true);
    }

    public static FileMatcherConfig enableInPlaceOverwriteNonStrict() {
        return new FileMatcherConfig(true, false, false, false);
    }

    public static FileMatcherConfig enablePassOnCreate() {
        return new FileMatcherConfig(false, true, false, true);
    }

    public static FileMatcherConfig enableInPlaceOverwriteAndPassOnCreate() {
        return new FileMatcherConfig(true, true, false, true);
    }

    public static FileMatcherConfig enableInPlaceOverwriteNonStrictAndPassOnCreate() {
        return new FileMatcherConfig(true, true, false, false);
    }

    public static FileMatcherConfig enableExpectedFileSorting() {
        return new FileMatcherConfig(false, false, true, true);
    }

    public static FileMatcherConfig enableExpectedFileSortingWithLenientMatching() {
        return new FileMatcherConfig(false, false, true, false);
    }

    protected static <T> T modifyObject(T input, Function<T, T> modifier) {
        return modifier.apply(input);
    }
}
