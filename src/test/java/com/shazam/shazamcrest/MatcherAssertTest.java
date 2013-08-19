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

import org.junit.Test;

import com.shazam.shazamcrest.model.Bean;
import com.shazam.shazamcrest.model.ChildBean;

/**
 * MatcherAssert tests checking the happy day scenarios.
 */
public class MatcherAssertTest {
	
	@Test
	public void doesNothingWhenBeansMatch() {
		Bean expected = bean().field1("value1").field2(1).build();
		Bean actual = bean().field1("value1").field2(1).build();

		assertThat(actual, sameBeanAs(expected));
	}

	@Test
	public void doesNothingWhenBothBeansAreNull() {
		ChildBean expected = null;
		ChildBean actual = null;

		assertThat(actual, sameBeanAs(expected));
	}
}
