package com.github.karsaig.approvalcrest.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import com.github.karsaig.approvalcrest.testdata.BeanWithPrimitives;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * When an in-place overwrite fails, the error has to identify the file it could not write.
 *
 * <p>It previously interpolated the object under comparison instead, so the message named no file
 * at all and dumped the whole serialised bean into the exception, the log and the CI output —
 * which matters when that bean carries sensitive data.
 *
 * <p>Uses a real temporary directory rather than the usual in-memory filesystem: jimfs records
 * POSIX permissions but does not enforce them, so a write to a read-only file there would succeed
 * and this failure path would never be reached.
 */
public class OverwriteFailureMessageTest extends AbstractFileMatcherTest {

    @TempDir
    Path tempDir;

    @Test
    public void overwriteFailureNamesTheFileAndNotTheContent() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")), "root ignores write permissions");

        BeanWithPrimitives actual = getBeanWithPrimitives();
        // Directory and file names are the 6-char SHA-1 prefixes of this class and method name.
        Path classDir = tempDir.resolve("1fe78c");
        Files.createDirectories(classDir);
        Path approved = classDir.resolve("7d6c70-approved.json");
        Files.write(approved, "{ \"dummyProperty\": \"dummyContent\" }".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(approved, PosixFilePermissions.fromString("r--r--r--"));

        DummyInformation testInfo = new DummyInformation(tempDir, "OverwriteFailureMessageTest",
                "overwriteFailureNamesTheFileAndNotTheContent", tempDir);
        JsonMatcher<BeanWithPrimitives> underTest =
                MATCHER_FACTORY.jsonMatcher(testInfo, enableInPlaceOverwriteAndPassOnCreate());

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> underTest.matches(actual));

        // fileNameWithPath is the base path; the -approved.json suffix is appended lower down,
        // which is also how the sibling "must exist in order to overwrite it" message reads.
        assertTrue(thrown.getMessage().contains("1fe78c") && thrown.getMessage().contains("7d6c70"),
                "the message must name the file it failed to write, was: " + thrown.getMessage());
        assertFalse(thrown.getMessage().contains("beanInteger"),
                "the message must not carry the serialised content, was: " + thrown.getMessage());
    }
}
