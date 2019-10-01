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

import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

import org.hamcrest.StringDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for {@link ResultComparison}
 */
public class ResultComparisonTest {

    private static final String NO_REASON = "";
    private ComparisonDescription shazamDescription;

    @BeforeEach
    public void before() {
        shazamDescription = new ComparisonDescription();
        shazamDescription.setComparisonFailure(true);
        shazamDescription.setActual("actual");
        shazamDescription.setExpected("expected");
        shazamDescription.setDifferencesMessage("message");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throwsComparisonFailureWithCorrectActualAndExpectedWhenShazamDescriptionIsPassedIn() {
        try {
            ResultComparison.containsComparableJson(NO_REASON, shazamDescription);
            fail();
        } catch (AssertionFailedError e) {
            checkThat(e,
                    actual(equalTo("actual")),
                    expected(equalTo("expected"))
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throwsComparisonFailureWithCorrectMessageWhenShazamDescriptionIsPassedIn() {
        try {
            ResultComparison.containsComparableJson(NO_REASON, shazamDescription);
            fail();
        } catch (AssertionFailedError e) {
            checkThat(e, message(equalTo("message expected:<[expected]> but was:<[actual]>")));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throwsComparisonFailureWithAssertDescriptionInMessageWhenShazamDescriptionIsPassedIn() {
        try {
            ResultComparison.containsComparableJson("assert description", shazamDescription);
            fail();
        } catch (AssertionFailedError e) {
            checkThat(e, message(equalTo("assert description\nmessage expected:<[expected]> but was:<[actual]>")));
        }
    }

    @Test
    public void doesNotThrowComparisonFailureWhenStringDescriptionIsPassedIn() {
        ResultComparison.containsComparableJson("any reason", new StringDescription());
    }
}
