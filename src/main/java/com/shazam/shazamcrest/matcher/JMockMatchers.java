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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Hamcrest matchers to be used within JMock expectations.
 */
class JMockMatchers {
	private final static Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
	
	/**
	 * Converts the expected and the actual object into json and uses {@link JSONAssert} to match them. If the objects
	 * don't match, the difference message is included in the {@link Description} object.<br/><br/>
	 * To be used inside JMock expectations.
	 * 
	 * @param expected The expected object
	 * @return the {@link Matcher}
	 */
	public static <T> Matcher<T> sameBeanAs(final T expected) {
		final String expectedJson = gson.toJson(expected);
		
		return new TypeSafeDiagnosingMatcher<T>() {
			@Override
			public void describeTo(Description description) {
				description.appendText(expectedJson);
			}

			@Override
			protected boolean matchesSafely(T actual, Description mismatchDescription) {
				String actualJson = gson.toJson(actual);

				try {
					JSONAssert.assertEquals(expectedJson, actualJson, true);
				} catch (AssertionError e) {
					return appendMismatchDescription(mismatchDescription, e);
				} catch (JSONException e) {
					return appendMismatchDescription(mismatchDescription, e);
				}

				return true;
			}
		};
	}
	
	private static boolean appendMismatchDescription(Description mismatchDescription, Throwable e) {
		mismatchDescription.appendText(e.getMessage());
		return false;
	}
}
