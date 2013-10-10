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

import static org.hamcrest.Matchers.equalTo;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

/**
 * {@link DiagnosingCustomisableMatcher} which applies the {@link IsEqual} matcher when the object to compare is a String
 * or a primitive type.
 */
class IsEqualMatcher<T> extends DiagnosingCustomisableMatcher<T> {

	public IsEqualMatcher(T expected) {
		super(expected);
	}
	
	@Override
	public void describeTo(Description description) {
		equalTo(expected).describeTo(description);
	}

	@Override
	protected boolean matches(Object actual, Description mismatchDescription) {
		Matcher<T> equalTo = equalTo(expected);
		boolean matches = equalTo.matches(actual);
		if (!matches) {
			equalTo.describeMismatch(actual, mismatchDescription);
		}
		return matches;
	}
}