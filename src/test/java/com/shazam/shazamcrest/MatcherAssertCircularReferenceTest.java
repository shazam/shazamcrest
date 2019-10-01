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
import static com.shazam.shazamcrest.model.cyclic.CircularReferenceBean.Builder.circularReferenceBean;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.shazam.shazamcrest.model.cyclic.CircularReferenceBean;
import com.shazam.shazamcrest.model.cyclic.Element;
import com.shazam.shazamcrest.model.cyclic.Four;
import com.shazam.shazamcrest.model.cyclic.One;
import com.shazam.shazamcrest.model.cyclic.Two;

/**
 * Unit tests which verify circular references are handled automatically.
 */
public class MatcherAssertCircularReferenceTest {

    @Test
    public void doesNothingWhenAutoDetectCircularReferenceIsCalled() {
        CircularReferenceBean expected = circularReferenceBean("parent", "child1", "child2").build();
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThat(actual, sameBeanAs(expected));
    }

    @Test
    public void shouldNotThrowStackOverFlowExceptionWhenExpectedBeanIsNullAndTheActualNotNull() {
        CircularReferenceBean expected = null;
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThrows(AssertionFailedError.class, () ->
                assertThat(actual, sameBeanAs(expected)));
    }

    @Test
    public void shouldNotThrowStackOverflowExceptionWhenCircularReferenceExistsInAComplexGraph() {
        Four root = new Four();
        Four child1 = new Four();
        Four child2 = new Four();
        root.setGenericObject(child1);
        child1.setGenericObject(root); // circular
        root.setSubClassField(child2);

        One subRoot = new One();
        One subRootChild = new One();
        subRoot.setGenericObject(subRootChild);
        subRootChild.setGenericObject(subRoot); // circular

        child2.setGenericObject(subRoot);

        assertThat(root, sameBeanAs(root));
    }

    @Test
    public void doesNotThrowStackOverflowErrorWhenComparedObjectsHaveDifferentCircularReferences() {
        One expected = new One();
        One expectedChild = new One();
        expected.setGenericObject(expectedChild);
        expectedChild.setGenericObject(expected);

        Two actual = new Two();
        Two actualChild = new Two();
        actual.setGenericObject(actualChild);
        actualChild.setGenericObject(actual);

        assertThrows(AssertionFailedError.class, () ->
                assertThat(actual, sameBeanAs(expected)));
    }

    @Test
    public void shouldNotTakeAges() {
        assertThrows(AssertionFailedError.class, () ->
                assertThat(Element.ONE, sameBeanAs(Element.TWO)));
    }

    @Test
    public void doesNotThrowStackOverflowErrorWhenCircularReferenceIsInTheSecondLevelUpperClass() {
        assertThat(new RuntimeException(), sameBeanAs(new RuntimeException()));
    }

    @Test
    public void doesNotThrowStackOverflowExceptionWithAMoreNestedObject() {
        final Throwable throwable = new Throwable(new Exception(new RuntimeException(new ClassCastException())));

        assertThat(throwable, sameBeanAs(throwable));
    }

    @Test
    public void doesNotReturn0x1InDiagnosticWhenUnnecessary() {
        try {
            assertThat(Element.ONE, sameBeanAs(Element.TWO));

            fail("expected ComparisonFailure");
        } catch (AssertionFailedError e) {
            assertThat(e.getExpected().getStringRepresentation(), not(containsString("0x1")));
            assertThat(e.getActual().getStringRepresentation(), not(containsString("0x1")));
        }
    }
}