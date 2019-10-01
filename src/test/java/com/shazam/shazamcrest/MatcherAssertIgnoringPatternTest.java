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
import static com.shazam.shazamcrest.model.ChildBean.Builder.child;
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.shazam.shazamcrest.model.Bean;
import com.shazam.shazamcrest.model.ParentBean;

/**
 * Tests for {@link MatcherAssert} which verify that fields that match supplied pattern are ignored.
 */
public class MatcherAssertIgnoringPatternTest {

    @Test
    public void ignoresByExactName() {
        ParentBean expected = parent().childBean("value", 123).build();
        ParentBean actual = parent().childBean("eulav", 123).build();

        assertThat(actual, sameBeanAs(expected).ignoring(is("childString")));
    }

    @Test
    public void ignoresAnyMatchingFieldNames() {
        ParentBean expected = parent().childBean("value", 123).addToChildBeanList(child().childString("child").build()).build();
        ParentBean actual = parent().childBean("value", 321).addToChildBeanList(child().childString("dlihc").build()).build();

        assertThat(actual, sameBeanAs(expected).ignoring(containsString("child")));
    }

    @Test
    public void throwsComparisonFailureWhenNonIgnoredFieldsMismatch() {
        Bean expected = bean().string("1234").integer(12345).build();
        Bean actual = bean().string("1234").integer(54321).build();

        assertThrows(AssertionFailedError.class, () ->
                assertThat(actual, sameBeanAs(expected).ignoring(containsString("string"))));
    }
}
