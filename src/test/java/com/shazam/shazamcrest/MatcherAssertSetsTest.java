package com.shazam.shazamcrest;

import static com.google.common.collect.Sets.newHashSet;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.shazam.shazamcrest.model.Bean;

/**
 * Tests which verifies sets comparison is not affected by the order of the elements.
 */
public class MatcherAssertSetsTest {

    static Stream<Object[]> data() {
        return Arrays.stream(new Object[20][0]);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void ignoresOrderingInSet() {
        Bean expected = bean().set(newHashSet(
                bean().string("a").build(),
                bean().string("b").build()))
                .build();

        Bean actual = bean().set(newHashSet(
                bean().string("a").build(),
                bean().string("b").build()))
                .build();

        assertThat(actual, sameBeanAs(expected));
    }

	@ParameterizedTest
	@MethodSource("data")
    public void ignoresOrderingInNestedSet() {
        Bean expected = bean().set(newHashSet(
                bean().set(newHashSet(
                        bean().string("a").build(),
                        bean().string("b").build())).build(),
                bean().set(newHashSet(
                        bean().string("a").build(),
                        bean().string("b").build())).build()))
                .build();

        Bean actual = bean().set(newHashSet(
                bean().set(newHashSet(
                        bean().string("a").build(),
                        bean().string("b").build())).build(),
                bean().set(newHashSet(
                        bean().string("a").build(),
                        bean().string("b").build())).build()))
                .build();

        assertThat(actual, sameBeanAs(expected));
    }

	@ParameterizedTest
	@MethodSource("data")
    public void ignoresOrderingForSetsImplementations() {
        Bean expected = bean().hashSet(newHashSet(
                bean().string("a").build(),
                bean().string("b").build()))
                .build();

        Bean actual = bean().hashSet(newHashSet(
                bean().string("a").build(),
                bean().string("b").build()))
                .build();

        assertThat(actual, sameBeanAs(expected));
    }
}
