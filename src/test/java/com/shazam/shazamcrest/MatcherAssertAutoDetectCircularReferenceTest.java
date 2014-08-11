package com.shazam.shazamcrest;

import com.shazam.shazamcrest.model.cyclic.CircularReferenceBean;
import com.shazam.shazamcrest.model.cyclic.Four;
import com.shazam.shazamcrest.model.cyclic.One;
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

    @Test(expected = ComparisonFailure.class)
    public void shouldNotThrowStackOverFlowExceptionWhenExpectedBeanIsNullAndTheActualNotNull() {
        CircularReferenceBean expected = null;
        CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

        assertThat(actual, sameBeanAs(expected).autoDetectCircularReference());
    }

    @Test(expected = Test.None.class)
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

        assertThat(root, sameBeanAs(root).autoDetectCircularReference());
    }
}