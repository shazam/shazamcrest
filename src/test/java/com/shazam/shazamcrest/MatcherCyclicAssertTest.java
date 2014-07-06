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

import com.shazam.shazamcrest.model.CircularReferenceBean;
import org.junit.Test;

import java.util.Arrays;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.model.CircularReferenceBean.InnerChild.innerChild;
import static com.shazam.shazamcrest.model.CircularReferenceBean.InnerParent.innerParent;
import static com.shazam.shazamcrest.model.CircularReferenceBean.cyclicBean;

/**
 * MatcherCyclicAssert tests checking the happy day scenarios.
 */
public class MatcherCyclicAssertTest {

    @Test
    public void doesNothingWhenBeansMatch() {

        final CircularReferenceBean expected = buildObjects("parent", "child1", "child2");
        final CircularReferenceBean actual = buildObjects("parent", "child1", "child2");

        assertThat(actual, sameBeanAs(expected).circularReference(CircularReferenceBean.InnerParent.class));
    }

    private CircularReferenceBean buildObjects(final String parentAttribute, final String... childAttribute) {
        final CircularReferenceBean.InnerParent innerParent = innerParent()
                .parentAttribute(parentAttribute)
                .build();
        final CircularReferenceBean.InnerChild innerChild1 = innerChild()
                .attribute1(childAttribute[0])
                .parent(innerParent)
                .build();
        final CircularReferenceBean.InnerChild innerChild2 = innerChild()
                .attribute1(childAttribute[1])
                .parent(innerParent)
                .build();
        innerParent.setInnerChildren(Arrays.asList(innerChild1, innerChild2));

        return cyclicBean().parent(innerParent).build();

    }


    @Test
    public void doesNothingWhenBothBeansAreNull() {
        CircularReferenceBean expected = null;
        CircularReferenceBean actual = null;

        assertThat(actual, sameBeanAs(expected));
    }
}
