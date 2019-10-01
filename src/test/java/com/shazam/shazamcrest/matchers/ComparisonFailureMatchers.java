/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.matchers;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.opentest4j.AssertionFailedError;

/**
 * ComparisonFailure hamcrest matchers.
 */
public class ComparisonFailureMatchers {
    public static Matcher<AssertionFailedError> expected(Matcher<String> expectedMatcher) {
        return new FeatureMatcher<AssertionFailedError, String>(expectedMatcher, "ComparisonFailure with expected string", "expected string") {
            @Override
            protected String featureValueOf(AssertionFailedError actual) {
                return actual.getExpected().getStringRepresentation();
            }
        };
    }

    public static Matcher<AssertionFailedError> actual(Matcher<String> actualMatcher) {
        return new FeatureMatcher<AssertionFailedError, String>(actualMatcher, "ComparisonFailure with actual string", "actual string") {
            @Override
            protected String featureValueOf(AssertionFailedError actual) {
                return actual.getActual().getStringRepresentation();
            }
        };
    }

    public static Matcher<AssertionFailedError> message(Matcher<String> messageMatcher) {
        return new FeatureMatcher<AssertionFailedError, String>(messageMatcher, "ComparisonFailure with message string", "message string") {
            @Override
            protected String featureValueOf(AssertionFailedError actual) {
                return actual.getMessage();
            }
        };
    }

    public static void checkThat(AssertionFailedError e, Matcher<AssertionFailedError>... matchers) {
        assertThat(e, allOf(matchers));
    }
}
