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

import com.shazam.shazamcrest.model.cyclic.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.shazam.shazamcrest.CyclicReferenceDetector.getClassesWithCircularReferences;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test that verifies that the {@link com.shazam.shazamcrest.CyclicReferenceDetector} works as anticipated.
 */
public class CyclicReferenceDetectorTests {
    @Test
    public void shouldReturnAnEmptySetWhenTheObjectIsNull() {
        Set<Class<?>> returnedObjects = getClassesWithCircularReferences(null);

        assertThat(returnedObjects.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAnEmptyListWhenThereIsNoCircularReference() {
        One one = new One();
        one.setGenericObject(new Two());
        Set<Class<?>> returnedObjects = getClassesWithCircularReferences(one);
        assertThat(returnedObjects.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnTheClassWithCyclicReferenceWhenTheObjectHasAFieldWithCyclicReference() {
        One one = new One();
        Two two = new Two();
        two.setGenericObject(one);
        one.setGenericObject(two);

        Set<Class<?>> returnedObjects = getClassesWithCircularReferences(one);

        assertThat(returnedObjects, hasItem(One.class));
    }

    @Test
    public void shouldReturnTheClassWithCyclicReferenceFieldWhenTheCyclicReferenceIsMoreThanTwoNodesAway() {
        One one = new One();
        Two two = new Two();
        Three three = new Three();

        one.setGenericObject(two);
        two.setGenericObject(three);
        three.setGenericObject(one);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }

    @Test
    public void shouldReturnAnEmptySetWhenTheObjectHasANullField() {
        One one = new One();
        one.setGenericObject(null);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses.isEmpty(), is(true));

    }

    @Test
    public void shouldReturnAnEmptySetWhenTheObjectHasAPrimitiveOrWrapperField() {
        One one = new One();
        one.setGenericObject(5);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAnEmptySetWhenTheObjectHasAStringField() {
        One one = new One();
        one.setGenericObject("irrelevant string");

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAnEmptySetWhenTheObjectHasAnEnumField() {
        One one = new One();
        one.setGenericObject(Five.FIVE);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnTheClassWithCircularReferenceFieldWhenTheFieldIsAListAndContainsAnObjectThatCausesCircularReference() {
        One one = new One();
        Two two = new Two();
        Three three = new Three();

        two.setGenericObject(three);
        three.setGenericObject(one);
        one.setGenericObject(Arrays.asList(two));

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }

    @SuppressWarnings("unchecked")
	@Test
    public void shouldReturnClassesWithCircularReferenceWhenAnObjectHasAListFieldWithMoreThanOneObjectsThatHasCircularReference() {
        One one = new One();
        Two two = new Two();
        Two two2 = new Two();
        Three three = new Three();
        three.setGenericObject(two2);
        two2.setGenericObject(three);

        two.setGenericObject(one);
        one.setGenericObject(Arrays.asList(two, three));

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItems(One.class, Three.class));
    }

    @Test
    public void shouldReturnTheClassWithCircularReferenceWhenTheObjectHasAFieldThatReferencesItself() {
        One one = new One();
        one.setGenericObject(one);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }

    @Test
    public void shouldReturnClassWithCircularReferenceWhenTheObjectHasAMapFieldWithAnObjectThatCausesCircularaReference() {
        One one = new One();
        Two two = new Two();

        two.setGenericObject(one);
        Map<Object, Two> twoMap = new HashMap<Object, Two>(1);

        twoMap.put(1, two);
        one.setGenericObject(twoMap);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }

    @Test
    public void shouldReturnAClassWithCircularReferenceWhenTheObjectHasAFieldFromTheSuperClassThatHasCircularReference() {
        Four four = new Four();
        One one = new One();
        one.setGenericObject(four);
        four.setGenericObject(one);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }

    @Test
    public void shouldReturnAClassWithCircularReferenceWhenTheObjectHasAMapFieldThatHasCircularReferenceOnTheKeySey() {
        One one = new One();
        Two two = new Two();

        two.setGenericObject(one);
        Map<Two, Object> twoMap = new HashMap<Two, Object>(1);

        twoMap.put(two, 1);
        one.setGenericObject(twoMap);

        Set<Class<?>> returnedClasses = getClassesWithCircularReferences(one);

        assertThat(returnedClasses, hasItem(One.class));
    }
}