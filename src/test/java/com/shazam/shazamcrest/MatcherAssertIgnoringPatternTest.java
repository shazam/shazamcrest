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
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.shazam.shazamcrest.model.ParentBean;
import org.junit.Test;

/**
 * Tests for {@link MatcherAssert} which verify that fields that match supplied pattern are ignored.
 */
public class MatcherAssertIgnoringPatternTest {

	@Test
	public void ignoresByExactName() {
	    ParentBean expected = parent().childBean("value3", 123).childBean("value3", 123).build();
	    ParentBean actual = parent().childBean("value3", 123).childBean("value3a", 123).build();;

		assertThat(actual, sameBeanAs(expected).ignoring(is("childString")));
	}
	
    @Test
    public void ignoresAny() {
        ParentBean expected = parent().childBean("value3", 123).childBean("value3", 123).build();
        ParentBean actual = parent().childBean("value3", 1235).childBean("value3a", 123).build();;

        assertThat(actual, sameBeanAs(expected).ignoring(containsString("child")));
    }
}
