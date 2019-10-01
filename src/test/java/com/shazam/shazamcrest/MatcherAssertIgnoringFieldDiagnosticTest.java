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
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.*;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.shazam.shazamcrest.model.Bean;

/**
 * MatcherAssert tests checking the diagnostic of failure cases when some fields
 * are ignored
 */
public class MatcherAssertIgnoringFieldDiagnosticTest {

    @Test
    @SuppressWarnings("unchecked")
    public void doesNotIncludeIgnoredFieldsInDiagnostics() {
        Bean expected = bean().string("value1").integer(1).build();
        Bean actual = bean().string("value2").integer(2).build();

        try {
            assertThat(actual, sameBeanAs(expected).ignoring("string"));
            fail("Exceptionexpected");
        } catch (AssertionFailedError e) {
            checkThat(e, expected(not(containsString("string"))), actual(not(containsString("string"))));
        }
    }
}
