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
import static java.util.Arrays.asList;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the matcher works as expected with primitives, Strings and Enums
 */
@RunWith(value = Parameterized.class)
public class MatcherAssertIsEqualTest {
	
	private final Object actual;
	private final Object expected;

	public MatcherAssertIsEqualTest(Object actual, Object expected) {
		this.actual = actual;
		this.expected = expected;
	}

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { 
				{ "banana", "banana" }, 
				{ true, true },
				{ 'a', 'a' },
				{ (byte)1, (byte)1 },
				{ (short)1, (short)1 },
				{ 10, 10 },
				{ 20l, 20l },
				{ 30.0f, 30.0f },
				{ 40.0d, 40.0d },
				{ EnumTest.ENUM, EnumTest.ENUM }
		};
		return asList(data);
	}

	@Test
	public void matchesExpectation() {
		assertThat(actual, sameBeanAs(expected));
	}
	
	private enum EnumTest {
		ENUM
	}

}
