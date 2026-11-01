package com.github.karsaig.approvalcrest.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.karsaig.approvalcrest.FileMatcherConfig;
import com.github.karsaig.approvalcrest.MatcherConfiguration;
import com.github.karsaig.approvalcrest.matcher.file.FileStoreMatcherUtils;
import com.github.karsaig.approvalcrest.util.InMemoryFsUtil;
import com.google.common.jimfs.Jimfs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Covers serialisation and approved-file format behaviours that are easy to get subtly wrong and
 * whose failure mode is silent: a comparison that cannot fail rather than one that fails loudly.
 */
public class SerialisationAndFileFormatTest {

    private static final Set<Class<?>> NO_CIRCULAR = Collections.emptySet();

    private static Gson gson() {
        return GsonProvider.gson(new MatcherConfiguration(), NO_CIRCULAR);
    }

    /** No equals()/hashCode() override - two instances with identical fields are NOT equal. */
    static class NoEquals {
        final String v;

        NoEquals(String v) {
            this.v = v;
        }
    }

    /**
     * A Set is sorted by JSON representation to keep the output stable, but sorting must not
     * deduplicate: elements that are not equals() yet serialise identically are still distinct
     * members and all of them belong in the output.
     */
    @Test
    public void setSerialisationKeepsElementsThatSerialiseIdentically() {
        Set<NoEquals> set = new LinkedHashSet<>();
        set.add(new NoEquals("same"));
        set.add(new NoEquals("same"));
        assertEquals(2, set.size(), "setup: the two beans are genuinely distinct Set members");

        JsonArray json = JsonParser.parseString(gson().toJson(set)).getAsJsonArray();

        assertEquals(2, json.size(), "both members must be serialised");
    }

    /**
     * The point of the above: if the duplicate were dropped, a set that lost or gained an element
     * would serialise identically to one that did not, so the difference could never fail a test.
     */
    @Test
    public void setsOfDifferentSizesDoNotSerialiseIdentically() {
        Set<NoEquals> actual = new LinkedHashSet<>();
        actual.add(new NoEquals("same"));
        actual.add(new NoEquals("same"));

        Set<NoEquals> expected = new LinkedHashSet<>();
        expected.add(new NoEquals("same"));

        assertNotEquals(gson().toJson(expected), gson().toJson(actual),
                "a set of 2 must be distinguishable from a set of 1");
    }

    /** Ordering is still deterministic regardless of the set's own iteration order. */
    @Test
    public void setSerialisationIsOrderedDeterministically() {
        Set<NoEquals> forwards = new LinkedHashSet<>();
        forwards.add(new NoEquals("b"));
        forwards.add(new NoEquals("a"));
        forwards.add(new NoEquals("c"));

        Set<NoEquals> backwards = new LinkedHashSet<>();
        backwards.add(new NoEquals("c"));
        backwards.add(new NoEquals("a"));
        backwards.add(new NoEquals("b"));

        assertEquals(gson().toJson(forwards), gson().toJson(backwards),
                "insertion order must not affect the serialised form");
        assertEquals("[{\"v\":\"a\"},{\"v\":\"b\"},{\"v\":\"c\"}]",
                gson().toJson(forwards).replaceAll("\\s+", ""),
                "elements must be ordered by their JSON representation");
    }

    /**
     * The old collapsing behaviour remains available as a migration escape hatch for codebases
     * with a large approved-file corpus to re-approve.
     */
    @Test
    public void legacySetCollapseOptOutRestoresTheOldBehaviour() {
        Set<NoEquals> set = new LinkedHashSet<>();
        set.add(new NoEquals("same"));
        set.add(new NoEquals("same"));

        MatcherConfiguration legacy = new MatcherConfiguration().setLegacySetCollapse(true);
        JsonArray json = JsonParser.parseString(
                GsonProvider.gson(legacy, NO_CIRCULAR).toJson(set)).getAsJsonArray();

        assertEquals(1, json.size(), "the opt-out collapses identically-serialising elements again");
    }

    /**
     * Documents current behaviour: OffsetDateTime is normalised to UTC before formatting, so two
     * values at the same instant with different offsets serialise identically and an offset-only
     * difference cannot fail a test. See docs/supported-types.md.
     */
    @Test
    public void offsetDateTimeIsNormalisedToUtc() {
        OffsetDateTime plusFive = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(5));
        OffsetDateTime utc = OffsetDateTime.of(2024, 1, 1, 7, 0, 0, 0, ZoneOffset.UTC);
        assertNotEquals(plusFive, utc, "setup: the two values are different OffsetDateTimes");
        assertEquals(plusFive.toInstant(), utc.toInstant(), "setup: but they are the same instant");

        assertEquals(gson().toJson(utc), gson().toJson(plusFive),
                "documented: OffsetDateTime values with different offsets serialise identically");
    }

    /**
     * Documents current behaviour: unlike OffsetDateTime, OffsetTime does not apply the UTC
     * override, so it keeps its offset. The two adapters disagree about whether offset matters.
     */
    @Test
    public void offsetTimeKeepsItsOffsetUnlikeOffsetDateTime() {
        OffsetTime plusFive = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(5));
        OffsetTime utc = OffsetTime.of(7, 0, 0, 0, ZoneOffset.UTC);

        assertNotEquals(gson().toJson(utc), gson().toJson(plusFive),
                "OffsetTime preserves the offset - inconsistent with OffsetDateTime above");
    }

    /**
     * An out-of-range bucket depth must surface as a configuration error naming the property,
     * not as an opaque StringIndexOutOfBoundsException from inside substring().
     */
    @Test
    public void outOfRangeBucketDepthIsRejectedWithAConfigurationError() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            Files.createDirectories(workDir);
            String shared = "shared";

            FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                    new FileMatcherConfig(false, false, false, true, shared, true, 2));

            for (int invalid : new int[]{-1, 0, 7, 99}) {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> utils.writeCanonical("{}", "c", workDir, shared, invalid),
                        "bucket depth " + invalid + " must be rejected");
                assertTrue(e.getMessage().contains("fileMatcherSharedDirBucketDepth"),
                        "the error must name the property, was: " + e.getMessage());
            }

            for (int valid : new int[]{1, 2, 6}) {
                utils.writeCanonical("{\"v\":" + valid + "}", "c", workDir, shared, valid);
            }
        }
    }

    /**
     * The same range is enforced at configuration time, so a bad -D value fails fast rather than
     * at the first shared-directory write.
     */
    @Test
    public void outOfRangeBucketDepthIsRejectedByTheConfig() {
        assertThrows(IllegalStateException.class,
                () -> new FileMatcherConfig(false, false, false, true, "shared", true, 0));
        assertThrows(IllegalStateException.class,
                () -> new FileMatcherConfig(false, false, false, true, "shared", true, 7));
    }

    /**
     * The header is written with a LF but must parse regardless of line ending, because a Windows
     * checkout with core.autocrlf=true turns it into a CRLF.
     */
    @Test
    public void headerIsStrippedForBothLfAndCrlfFiles() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            Files.createDirectories(workDir);
            FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                    new FileMatcherConfig(false, false, false, true, "shared", true, 2));

            Path lf = workDir.resolve("lf-approved.json");
            Files.write(lf, "/*comment*/\n{\"v\":1}".getBytes(StandardCharsets.UTF_8));
            assertEquals("{\"v\":1}", utils.readFile(lf, workDir), "LF file: header correctly stripped");

            Path crlf = workDir.resolve("crlf-approved.json");
            Files.write(crlf, "/*comment*/\r\n{\"v\":1}".getBytes(StandardCharsets.UTF_8));

            assertEquals("{\"v\":1}", utils.readFile(crlf, workDir),
                    "CRLF file: header must be stripped too");
        }
    }

    /**
     * A CRLF pointer file must be recognised as a pointer.
     */
    @Test
    public void pointerFileIsRecognisedForBothLfAndCrlf() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem(InMemoryFsUtil.JIMFS_UNIX_CONFIG)) {
            Path workDir = fs.getPath("/work");
            Files.createDirectories(workDir);
            FileStoreMatcherUtils utils = new FileStoreMatcherUtils("json",
                    new FileMatcherConfig(false, false, false, true, "shared", true, 2));

            Path lf = workDir.resolve("lf-approved.json");
            Files.write(lf, "/*c*/\n/*pointer:shared/ab/x-approved.json*/".getBytes(StandardCharsets.UTF_8));
            assertTrue(utils.isPointerFile(lf), "LF pointer correctly detected");

            Path crlf = workDir.resolve("crlf-approved.json");
            Files.write(crlf, "/*c*/\r\n/*pointer:shared/ab/x-approved.json*/".getBytes(StandardCharsets.UTF_8));

            assertTrue(utils.isPointerFile(crlf), "CRLF pointer must be detected too");
            assertEquals("shared/ab/x-approved.json", utils.readPointerTarget(crlf).orElse(null),
                    "CRLF pointer target must parse without a trailing carriage return");
        }
    }
}
