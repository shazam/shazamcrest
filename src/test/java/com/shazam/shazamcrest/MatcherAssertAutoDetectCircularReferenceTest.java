package com.shazam.shazamcrest;

import com.shazam.shazamcrest.model.cyclic.CircularReferenceBean;
import org.junit.ComparisonFailure;
import org.junit.Test;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.model.cyclic.CircularReferenceBean.Builder.circularReferenceBean;

/**
 * Unit tests which verify circular references are handled automatically if autoDetectCircularReference is called.}
 */
public class MatcherAssertAutoDetectCircularReferenceTest {

    @Test
    public void doesNothingWhenAutoDetectCircularReferenceIsCalled() {
        CircularReferenceBean expected = circularReferenceBean("parent", "child1", "child2").build();
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThat(actual, sameBeanAs(expected).autoDetectCircularReference());
    }

    @Test(expected = StackOverflowError.class)
    public void throwsStackOverFlowExceptionWhenAutoDetectCircularReferenceIsNotCalledOnBeanWithCircularReference() {
        CircularReferenceBean expected = circularReferenceBean("parent", "child1", "child2").build();
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThat(actual, sameBeanAs(expected));
    }
}

