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

import static com.shazam.shazamcrest.ResultComparison.containsComparableJson;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.ComparisonFailure;

/**
 * Modified version of {@link org.hamcrest.MatcherAssert}. If the matcher doesn't match, uses
 * {@link ResultComparison#containsComparableJson(Description)} to determine if a {@link ComparisonFailure} should be
 * thrown. The exception is thrown instead of {@link AssertionError}, so that IDE like eclipse and IntelliJ can display a
 * pop-up window highlighting the String differences.
 */
public class MatcherAssert {
	/**
	 * @see org.hamcrest.MatcherAssert#assertThat(Object, Matcher)
	 */
	public static <T> void assertThat(T actual, Matcher<? super T> matcher) {
        assertThat("", actual, matcher);
    }
	
    /**
     * Checks if the object matches the condition defined by the matcher provided.
     * 
     * @param reason describes the assertion
     * @param actual the object that will be matched against the matcher
     * @param matcher defines the condition the object have to fulfil in order to match
     */
    public static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
        if (!matcher.matches(actual)) {
            Description description = new ShazamDescription();
            description.appendText(reason)
                       .appendText("\nExpected: ")
                       .appendDescriptionOf(matcher)
                       .appendText("\n     but: ");
            matcher.describeMismatch(actual, description);
            
            containsComparableJson(description);
            
            throw new AssertionError(description.toString());
        }
    }
}
