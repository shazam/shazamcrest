/*
 * Copyright 2013 Shazam Entertainment Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/
package com.shazam.shazamcrest.matchers;

import static org.hamcrest.core.IsEqual.equalTo;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import com.shazam.shazamcrest.model.ChildBean;

/**
 * Collection of Hamcrest matchers for {@link ChildBean}
 */
public class ChildBeanMatchers {

	public static Matcher<ChildBean> childField1EqualTo(String string) {
		return new FeatureMatcher<ChildBean, String>(equalTo(string), "having string field", "string field") {
			@Override
			protected String featureValueOf(ChildBean actual) {
				return actual.getChildField1();
			}
		};
	}
}
