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

import java.lang.reflect.Field;
import java.util.*;

import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

/**
 * Detects classes with fields that have circular reference and returns a set of those classes.
 */

public class CyclicReferenceDetector {

    private Set<Object> nodesInPaths = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    private Set<Class<?>> circularReferenceTypes = new HashSet<Class<?>>();

    public static Set<Class<?>> getClassesWithCircularReferences(Object object) {
        CyclicReferenceDetector cyclicReferenceDetector = new CyclicReferenceDetector();

        if (object != null) {
            cyclicReferenceDetector.detectCircularReferenceOnObject(object, object.getClass());
        }

        return cyclicReferenceDetector.circularReferenceTypes;
    }

    /**
     * Returns a set of classes that have circular reference
     * @param object the object to check if it has circular reference
     * @param clazz the class being used (necessary if we also checking super class as getDeclaredFields only returns
     *              fields of a given class, but not its super class).
     */
    private void detectCircularReferenceOnObject(Object object, Class<?> clazz) {
        if (object == null || circularReferenceTypes.contains(clazz)) {
            return;
        }

        nodesInPaths.add(object);

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Object fieldValue = field.get(object);
                if (fieldValue != null) {
                    detectCircularReferenceOnField(fieldValue);
                }
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
        }
        detectCircularReferencesFromTheSuperClass(object, clazz);
        nodesInPaths.remove(object);
    }

    /**
     * Detects circular reference on a given field.
     * If a field is a {@link Iterable} or a {@link Map}, loops through the values and
     * detects cycles.
     * @param fieldValue the field/object to detect circular reference on.
     */
    private void detectCircularReferenceOnField(Object fieldValue) {
        if (fieldValue instanceof Iterable) {
            detectCircularReferenceFromObjectsContainedInAnIterable((Iterable) fieldValue);
        } else if (fieldValue instanceof Map) {
            detectCircularReferencesFromObjectsInAMap((Map) fieldValue);
        } else if (nodesInPaths.contains(fieldValue)) {
            circularReferenceTypes.add(fieldValue.getClass());
            nodesInPaths.remove(fieldValue);
        }

        if (validateAnObject(fieldValue)) {
            detectCircularReferenceOnObject(fieldValue, fieldValue.getClass());
        }
    }

    /**
     * For objects with super classes, check for objects subject to circular reference in super class.
     * @param object the object to check if it has circular reference
     * @param clazz Used to prevent stackOverFlow exception
     */
    private void detectCircularReferencesFromTheSuperClass(Object object, Class<?> clazz) {
        Class<?> superclass = object.getClass().getSuperclass();

        if (superclass != Object.class && superclass != clazz && validateAnObject(object)) {
            detectCircularReferenceOnObject(object, superclass);
        }
    }

    /**
     * Detect circular references on {@link Map}s, i.e HashMap, TreeMap, etc
     * @param map the {@link Map} with objects to checks for cyclic references on.
     */
    private void detectCircularReferencesFromObjectsInAMap(Map map) {
        nodesInPaths.remove(map);
        detectCircularReferenceFromObjectsContainedInAnIterable(map.values());
        detectCircularReferenceFromObjectsContainedInAnIterable(map.keySet());
    }

    /**
     * detects circular references on {@link Iterable}s, i.e {@link Collection}s.
     * @param iterable the object to iterate through.
     */
    private void detectCircularReferenceFromObjectsContainedInAnIterable(Iterable iterable) {
        nodesInPaths.remove(iterable);
        for (Object elementInCollection : iterable) {
            if (elementInCollection != null) {
                detectCircularReferenceOnObject(elementInCollection, elementInCollection.getClass());
            }
        }
    }

    /**
     * Checks to see if the given object is primitive or wrapper class, {@link String}, instance of {@link Iterable},
     * instance of {@link Map} or instance of {@link Enum}.
     *
     * @param object The object to validate
     * @return true if the object is not primitive/wrapper class and not a string class and not instance of (
     * {@link String} and {@link Iterable} and {@link Map} and {@link Enum})
     */
    private boolean validateAnObject(Object object) {
        return !isPrimitiveOrWrapper(object.getClass())
                && object.getClass() != String.class
                && !(object instanceof Iterable)
                && !(object instanceof Map)
                && !(object instanceof Enum);
    }
}
