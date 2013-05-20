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

import static com.shazam.shazamcrest.GsonProvider.gson;
import static org.hamcrest.CoreMatchers.nullValue;

import org.hamcrest.Description;

/**
 * {@link IgnoringDiagnosingMatcher} implementation which verifies a bean is null.
 */
class NullMatcher<T> extends IgnoringDiagnosingMatcher<T> {
	public NullMatcher(T expected) {
		super(expected);
	}

	@Override
	protected boolean matches(Object actual, Description mismatchDescription) {
		if (actual != null) {
			String actualJson = gson(typesToIgnore).toJson(actual);
			return appendMismatchDescription(mismatchDescription, "null", actualJson, "actual is not null");
		}
		return true;
	}

	@Override
	public void describeTo(Description description) {
		nullValue().describeTo(description);
	}
}