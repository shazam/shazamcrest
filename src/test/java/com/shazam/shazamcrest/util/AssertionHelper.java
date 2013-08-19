/*
 * Copyright 2013 Shazam Entertainment Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/
package com.shazam.shazamcrest.util;

import com.shazam.shazamcrest.MatcherAssert;
import com.shazam.shazamcrest.matcher.IgnoringMatcher;
import com.shazam.shazamcrest.matcher.Matchers;
import com.shazam.shazamcrest.model.Bean;
import com.shazam.shazamcrest.model.ChildBean;
import com.shazam.shazamcrest.model.ParentBean;

/**
 * Provides helper methods to reduce the noise in the test classes
 */
public class AssertionHelper {
	public static IgnoringMatcher<ChildBean> sameBeanAs(ChildBean.Builder expected) {
		return Matchers.sameBeanAs(expected.build());
	}
	
	public static void assertThat(ChildBean.Builder actual, IgnoringMatcher<ChildBean> matcher) {
		MatcherAssert.assertThat(actual.build(), matcher);
	}
	
	public static IgnoringMatcher<ParentBean> sameBeanAs(ParentBean.Builder expected) {
		return Matchers.sameBeanAs(expected.build());
	}
	
	public static void assertThat(ParentBean.Builder actual, IgnoringMatcher<ParentBean> matcher) {
		MatcherAssert.assertThat(actual.build(), matcher);
	}
	
	public static IgnoringMatcher<Bean> sameBeanAs(Bean.Builder expected) {
		return Matchers.sameBeanAs(expected.build());
	}
	
	public static void assertThat(Bean.Builder actual, IgnoringMatcher<Bean> matcher) {
		MatcherAssert.assertThat(actual.build(), matcher);
	}
}
