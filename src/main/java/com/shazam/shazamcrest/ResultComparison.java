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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.hamcrest.Description;
import org.opentest4j.AssertionFailedError;

/**
 * Determines if the {@link Description} contains comparable Json
 */
public class ResultComparison {
    /**
     * Throws a {@link AssertionFailedError} if the description passed in is of type {@link ComparisonDescription},
     * and the mismatch has been defined as a comparison failure.
     *
     * @param reason the {@link MatcherAssert#assertThat(String, Object, org.hamcrest.Matcher)} reason
     * @param description the {@link Description} which potentially holds the comparison failure information
     * @throws AssertionFailedError
     */
    public static void containsComparableJson(String reason, Description description) throws AssertionFailedError {
        if (description instanceof ComparisonDescription) {
            ComparisonDescription shazamDescription = (ComparisonDescription) description;
            if (shazamDescription.isComparisonFailure()) {
                throw new AssertionFailedError(
                        comparisonFailureMessage(reason, shazamDescription),
                        shazamDescription.getExpected(),
                        shazamDescription.getActual()
                );
            }
        }
    }

    private static String comparisonFailureMessage(String reason, ComparisonDescription shazamDescription) {
        return (isNotBlank(reason) ? reason + "\n" : "") + shazamDescription.getDifferencesMessage() +
                String.format(" expected:<[%s]> but was:<[%s]>", shazamDescription.getExpected(), shazamDescription.getActual());
    }
}
