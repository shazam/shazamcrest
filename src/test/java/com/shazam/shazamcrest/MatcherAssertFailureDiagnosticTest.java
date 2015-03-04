/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.shazam.shazamcrest.FieldsIgnorer.MARKER;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.actual;
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.checkThat;
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.expected;
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.message;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.ComparisonFailure;
import org.junit.Test;

import com.shazam.shazamcrest.model.Bean;

/**
 * MatcherAssert tests checking the diagnostic of the failure cases
 */
@SuppressWarnings("unchecked")
public class MatcherAssertFailureDiagnosticTest {

	@Test
	public void containsDiagnosticsWhenActualIsNull() {
		Bean expected = bean().string("value1").integer(1).build();
		Bean actual = null;

		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(is(notANullValue())), actual(is(equalTo("null"))));
		}
	}

	@Test
	public void includesCorrectMessageWhenActualIsNull() {
		Bean expected = bean().string("value1").integer(1).build();
		Bean actual = null;

		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, message(is(equalTo("actual was null expected:<[{\n  \"string\": \"value1\",\n  \"integer\": 1\n}]> but was:<[null]>"))));
		}
	}

	@Test
	public void containsDiagnosticsWhenExpectedIsNull() {
		Bean expected = null;
		Bean actual = bean().string("value1").integer(1).build();

		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(is(equalTo("null"))), actual(is(notANullValue())));
		}
	}

	@Test
	public void includesCorrectMessageWhenExpectedIsNull() {
		Bean expected = null;
		Bean actual = bean().string("value1").integer(1).build();

		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exception expected");
		} catch (ComparisonFailure e) {
			checkThat(e, message(is(equalTo("actual is not null expected:<[null]> but was:<[{\n  \"string\": \"value1\",\n  \"integer\": 1\n}]>"))));
		}
	}
	
	@Test
	public void prettyPrintsTheJson() {
		Bean expected = bean().string("value1").integer(1).build();
		Bean actual = bean().string("value2").integer(2).build();
		
		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exceptionexpected");
		} catch (ComparisonFailure e) {
			checkThat(e, 
					expected(containsString("{\n  \"string\": \"value1\",\n  \"integer\": 1\n}")), 
					actual(containsString("{\n  \"string\": \"value2\",\n  \"integer\": 2\n}")));
		}
	}
	
	@Test
	public void includesAssertDescriptionInDiagnostic() {
		Bean expected = bean().string("value1").integer(1).build();
		Bean actual = bean().string("value2").integer(2).build();
		
		try {
			assertThat("assertion description", actual, sameBeanAs(expected));
			fail("Exceptionexpected");
		} catch (ComparisonFailure e) {
			checkThat(e, message(startsWith("assertion description\n")));
		}
	}
	
	@Test
	public void doesNotIncludeMarkerInDiagnosticsForSets() {
		Bean expected = bean().set(newHashSet(bean().integer(1).build())).build();
		Bean actual = bean().set(newHashSet(bean().integer(2).build())).build();
		
		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exceptionexpected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(not(containsString(MARKER))), actual(not(containsString(MARKER))));
		}
	}
	
	@Test
	public void doesNotIncludeMarkerInDiagnosticsForMaps() {
		Map<Bean, Bean> expectedMap = newHashMap();
		expectedMap.put(bean().integer(1).build(), bean().integer(1).build());
		Map<Bean, Bean> actualMap = newHashMap();
		actualMap.put(bean().integer(2).build(), bean().integer(2).build());
		Bean expected = bean().map(expectedMap).integer(1).build();
		Bean actual = bean().map(actualMap).build();
		
		try {
			assertThat(actual, sameBeanAs(expected));
			fail("Exceptionexpected");
		} catch (ComparisonFailure e) {
			checkThat(e, expected(not(containsString(MARKER))), actual(not(containsString(MARKER))));
		}
	}
	
	private Matcher<String> notANullValue() {
		return notNullValue(String.class);
	}
}
