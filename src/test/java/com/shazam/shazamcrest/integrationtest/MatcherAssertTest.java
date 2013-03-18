/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.integrationtest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;

import org.junit.Test;

/**
 * MatcherAssert tests checking the happy day scenarios.
 */
public class MatcherAssertTest {
	@Test
	public void doesNothingWhenBeansMatch() {
		TestBean expected = new TestBean("value1", 1);
		TestBean actual = new TestBean("value1", 1);

		assertThat(actual, sameBeanAs(expected));
	}

	@Test
	public void doesNothingWhenBothBeansAreNull() {
		TestBean expected = null;
		TestBean actual = null;

		assertThat(actual, sameBeanAs(expected));
	}
}
