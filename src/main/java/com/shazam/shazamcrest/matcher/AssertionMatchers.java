/*
 * Copyright 2013 Shazam Entertainment Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/
package com.shazam.shazamcrest.matcher;

import static org.hamcrest.CoreMatchers.nullValue;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.ComparisonFailure;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shazam.shazamcrest.ShazamDescription;

/**
 * Hamcrest matchers to be used with {@link com.shazam.shazamcrest.MatcherAssert#assertThat(Object, Matcher)}
 */
class AssertionMatchers {
	private final static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

	public static <T> Matcher<T> isNull() {
		return new DiagnosingMatcher<T>() {
			@Override
			protected boolean matches(Object actual, Description mismatchDescription) {
				if (actual != null) {
					final String actualJson = gson.toJson(actual);
					return appendMismatchDescription(mismatchDescription, "null", actualJson, "actual is not null");
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				nullValue().describeTo(description);
			}
		};
	}

	/**
	 * Converts the expected and the actual object into json and uses {@link JSONAssert} to match them. If the objects
	 * don't match, the difference message, along with actual and expected json, is included in the {@link Description}
	 * object.<br/>
	 * <br/>
	 * To be used in association with {@link com.shazam.shazamcrest.MatcherAssert}, so that {@link ComparisonFailure} is
	 * thrown, in case of mismatch.
	 *
	 * @param expected The expected object
	 * @return the {@link Matcher}
	 */
	public static <T> Matcher<T> sameBeanAs(final T expected) {
		final String expectedJson = gson.toJson(expected);

		return new DiagnosingMatcher<T>() {
			@Override
			public void describeTo(Description description) {
				description.appendText(expectedJson);
			}

			protected boolean matches(Object actual, Description mismatchDescription) {
				String actualJson = gson.toJson(actual);

				try {
					JSONAssert.assertEquals(expectedJson, actualJson, true);
				} catch (AssertionError e) {
					return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, e.getMessage());
				} catch (JSONException e) {
					return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, e.getMessage());
				}

				return true;
			}

		};
	}

	private static boolean appendMismatchDescription(Description mismatchDescription, String expectedJson, String actualJson, String message) {
		if (mismatchDescription instanceof ShazamDescription) {
			ShazamDescription shazamMismatchDescription = (ShazamDescription) mismatchDescription;
			shazamMismatchDescription.setComparisonFailure(true);
			shazamMismatchDescription.setExpected(expectedJson);
			shazamMismatchDescription.setActual(actualJson);
			shazamMismatchDescription.setDifferencesMessage(message);
		}
		mismatchDescription.appendText(message);
		return false;
	}
}
