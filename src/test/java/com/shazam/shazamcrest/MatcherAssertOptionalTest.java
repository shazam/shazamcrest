/*
 * Copyright 2015 Shazam Entertainment Limited
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
import static org.hamcrest.Matchers.not;

import org.junit.Test;

import com.google.common.base.Optional;

/**
 * Tests which verifies {@link Optional} is compared correctly.
 */
public class MatcherAssertOptionalTest {

	@Test
    public void doesNotMatchWithDifferentOptionalReferences() {
        Bean expected = new Bean("foo", 1);
        Bean actual = new Bean("foo", 2);

        assertThat(actual, not(sameBeanAs(expected)));
    }

    @Test
    public void doesNotMatchWithAbsentAndPresent() {
        Bean expected = new Bean("foo", 1);
        Bean string = new Bean("foo", null);

        assertThat(string, not(sameBeanAs(expected)));
    }
    
    @SuppressWarnings("unused")
    private class Bean {
		private String string;
        private Optional<Integer> optional;

        Bean(String someString, Integer integer) {
            this.string = someString;
            this.optional = Optional.fromNullable(integer);
        }
    }
}