/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.integrationtest;

import static com.shazam.shazamcrest.integrationtest.ComparisonFailureMatchers.actual;
import static com.shazam.shazamcrest.integrationtest.ComparisonFailureMatchers.expected;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

import org.hamcrest.Matcher;
import org.junit.ComparisonFailure;
import org.junit.Test;

import com.shazam.shazamcrest.MatcherAssert;

/**
 * MatcherAssert tests checking the failure cases
 */
public class MatcherAssertFailureTest {

	@Test(expected = ComparisonFailure.class)
	public void comparisonFailureWhenBeansNotMatching() {
		TestBean expected = new TestBean("value1", 1);
		TestBean actual = new TestBean("value2", 2);

		MatcherAssert.assertThat(actual, sameBeanAs(expected));
	}

	@Test(expected = AssertionError.class)
	public void assertionErrorWhenNormalMatchersUsed() {
		MatcherAssert.assertThat("value1", equalTo("value2"));
	}

	@Test
	public void containsDiagnosticsWhenActualIsNull() {
		TestBean expected = new TestBean("value1", 1);
		TestBean actual = null;

		try {
			MatcherAssert.assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(is(notANullValue())), actual(is(equalTo("null"))));
		}
	}

	@Test
	public void containsDiagnosticsWhenExpectedIsNull() {
		TestBean expected = null;
		TestBean actual = new TestBean("value2", 2);

		try {
			MatcherAssert.assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(is(equalTo("null"))), actual(is(notANullValue())));
		}
	}


	private Matcher<String> notANullValue() {
		return notNullValue(String.class);
	}

	private void checkThat(ComparisonFailure e, Matcher<ComparisonFailure> matcher1, Matcher<ComparisonFailure> matcher2) {
		org.hamcrest.MatcherAssert.assertThat(e, allOf(matcher1, matcher2));
	}
}
