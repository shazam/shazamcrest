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
import static com.shazam.shazamcrest.model.CircularReferenceBean.Builder.circularReferenceBean;

import org.junit.ComparisonFailure;
import org.junit.Test;

import com.shazam.shazamcrest.model.CircularReferenceBean;

/**
 * Unit tests which verify circular references are handled without throwing a {@link StackOverflowError}
 */
public class MatcherAssertCircularReferenceTest {

    @Test
    public void doesNothingWhenBeansMatch() {
        CircularReferenceBean expected = circularReferenceBean("parent", "child1", "child2").build();
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThat(actual, sameBeanAs(expected).circularReference(CircularReferenceBean.Parent.class));
    }

    @Test
    public void doesNothingWhenBothBeansAreNull() {
        CircularReferenceBean expected = null;
        CircularReferenceBean actual = null;

        assertThat(actual, sameBeanAs(expected));
    }
    
    @Test(expected = ComparisonFailure.class)
    public void throwsComparisonFailureWhenCircularReferenceBeansDiffer() {
        CircularReferenceBean expected = circularReferenceBean("expectedParent", "expectedChild1", "expectedChild2").build();
        CircularReferenceBean actual = circularReferenceBean("actualParent", "actualChild1", "actualChild2").build();
        
        assertThat(actual, sameBeanAs(expected).circularReference(CircularReferenceBean.Parent.class));
    }
}
