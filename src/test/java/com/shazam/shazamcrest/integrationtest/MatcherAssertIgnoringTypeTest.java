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

import com.shazam.shazamcrest.MatcherAssert;

/**
 * Tests for {@link MatcherAssert} which verify that fields of specific object types can be ignored from the comparison.
 */
public class MatcherAssertIgnoringTypeTest {

	@Test
	public void ignoresType() {
		ParentTestBean bean = new ParentTestBean("parentField1", new TestBean("field1", 1));
		ParentTestBean expected = new ParentTestBean("parentField1", new TestBean("field2", 2));

		assertThat(bean, sameBeanAs(expected).ignoring(TestBean.class));
	}
}
