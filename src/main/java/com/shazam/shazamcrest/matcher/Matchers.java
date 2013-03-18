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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.shazam.shazamcrest.ShazamDescription;

/**
 * Proxy class which determines if the matcher is being used within a JMock expectation or with an assertion.
 */
public class Matchers {

	public static <T> Matcher<T> sameBeanAs(final T expected) {
		if (expected == null) {
			return AssertionMatchers.isNull();
		}

		return new BaseMatcher<T>() {
			@Override
			public void describeTo(Description description) {
				description.appendDescriptionOf(resolveMatcher(description));
			}

			private Matcher<T> resolveMatcher(Description mismatchDescription) {
				if (mismatchDescription instanceof ShazamDescription) {
					return AssertionMatchers.sameBeanAs(expected);
				} else {
					return JMockMatchers.sameBeanAs(expected);
				}
			}

			@Override
			public boolean matches(Object item) {
				return JMockMatchers.sameBeanAs(expected).matches(item);
			}

			@Override
			public void describeMismatch(Object item, Description description) {
				resolveMatcher(description).describeMismatch(item, description);
			}
		};
	}
}
