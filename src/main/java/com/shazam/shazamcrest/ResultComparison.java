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

import org.hamcrest.Description;
import org.junit.ComparisonFailure;

/**
 * Determines if the {@link Description} contains comparable Json
 */
class ResultComparison {
	/**
	 * Throws a {@link ComparisonFailure} if the description passed in is of type {@link ShazamDescription},
	 * and the mismatch has been defined as a comparison failure.
	 * 
	 * @param description the {@link Description} which potentially holds the comparison failure information
	 * @throws ComparisonFailure
	 */
	static void containsComparableJson(Description description) throws ComparisonFailure {
		if (description instanceof ShazamDescription) {
			ShazamDescription shazamDescription = (ShazamDescription) description;
			if (shazamDescription.isComparisonFailure()) {
				throw new ComparisonFailure(
						shazamDescription.getDifferencesMessage(),
						shazamDescription.getExpected(),
						shazamDescription.getActual()
				);
			}
		}
	}
}
