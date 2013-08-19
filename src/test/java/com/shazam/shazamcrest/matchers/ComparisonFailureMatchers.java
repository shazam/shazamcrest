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

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.ComparisonFailure;

/**
 * ComparisonFailure hamcrest matchers.
 */
public class ComparisonFailureMatchers {
	public static Matcher<ComparisonFailure> expected(Matcher<String> expectedMatcher) {
		return new FeatureMatcher<ComparisonFailure, String>(expectedMatcher, "ComparisonFailure with expected string", "expected string") {
			@Override
			protected String featureValueOf(ComparisonFailure actual) {
				return actual.getExpected();
			}
		};
	}

	public static Matcher<ComparisonFailure> actual(Matcher<String> actualMatcher) {
		return new FeatureMatcher<ComparisonFailure, String>(actualMatcher, "ComparisonFailure with actual string", "actual string") {
			@Override
			protected String featureValueOf(ComparisonFailure actual) {
				return actual.getActual();
			}
		};
	}
	
	public static Matcher<ComparisonFailure> message(Matcher<String> messageMatcher) {
		return new FeatureMatcher<ComparisonFailure, String>(messageMatcher, "ComparisonFailure with message string", "message string") {
			@Override
			protected String featureValueOf(ComparisonFailure actual) {
				return actual.getMessage();
			}
		};
	}
	
	public static void checkThat(ComparisonFailure e, Matcher<ComparisonFailure>... matchers) {
		org.hamcrest.MatcherAssert.assertThat(e, allOf(matchers));
	}
}
