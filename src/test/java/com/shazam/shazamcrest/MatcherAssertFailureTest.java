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

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.shazam.shazamcrest.model.Bean;

/**
 * MatcherAssert tests checking the failure cases
 */
public class MatcherAssertFailureTest {

	@Test
	public void throwsComparisonFailureWhenBeansNotMatching() {
		Bean expected = bean().string("value1").integer(1).build();
		Bean actual = bean().string("value2").integer(2).build();

		assertThrows(AssertionFailedError.class, () ->
				assertThat(actual, sameBeanAs(expected)));
	}

	@Test
	public void throwsAssertionErrorWhenNormalMatchersUsed() {
		assertThrows(AssertionError.class, () ->
				assertThat("value1", equalTo("value2")));
	}
}
