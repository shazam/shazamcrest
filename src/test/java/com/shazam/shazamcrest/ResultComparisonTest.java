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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.StringDescription;
import org.junit.ComparisonFailure;
import org.junit.Test;

/**
 * Unit tests for {@link ResultComparison}
 */
public class ResultComparisonTest {

	@Test
	public void throwsComparisonFailureWhenShazamDescriptionIsPassedIn() {
		ComparisonDescription shazamDescription = new ComparisonDescription();
		shazamDescription.setComparisonFailure(true);
		shazamDescription.setActual("actual");
		shazamDescription.setExpected("expected");
		shazamDescription.setDifferencesMessage("message");
		try {
			ResultComparison.containsComparableJson(shazamDescription);
			fail();
		} catch (ComparisonFailure e) {
			assertThat(e.getActual(), equalTo("actual"));
			assertThat(e.getExpected(), equalTo("expected"));
			assertThat(e.getMessage(), equalTo("message expected:<[expected]> but was:<[actual]>"));
		}
	}
	
	@Test
	public void doesNotThrowComparisonFailureWhenStringDescriptionIsPassedIn() {
		ResultComparison.containsComparableJson(new StringDescription());
	}
}
