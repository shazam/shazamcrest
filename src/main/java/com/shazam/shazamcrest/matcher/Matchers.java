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

/**
 * Entry point for the matchers available in Shazamcrest.
 */
public class Matchers {

	/**
	 * Returns a {@link NullMatcher} in case the expectation is null, or a
	 * {@link IgnoringDiagnosingMatcher} otherwise.
	 * 
	 * @param expected the expected bean to match against
	 * @return an {@link IgnoringMatcher} instance
	 */
	public static <T> IgnoringMatcher<T> sameBeanAs(final T expected) {
		if (expected == null) {
			return new NullMatcher<T>(expected);
		}

		return new IgnoringDiagnosingMatcher<T>(expected);
	}
}
