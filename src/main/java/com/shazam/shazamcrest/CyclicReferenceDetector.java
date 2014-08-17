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

import static java.util.Collections.newSetFromMap;
import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Detects classes with fields that have circular reference and returns a set of those classes.
 */
public class CyclicReferenceDetector {

    private Set<Object> nodesInPaths = newSetFromMap(new IdentityHashMap<Object, Boolean>());
    private Set<Class<?>> circularReferenceTypes = new HashSet<Class<?>>();
    
    /**
     * Returns a set of classes that have circular reference.
     * 
     * @param object the object to check if it has circular reference fields
     */
    public static Set<Class<?>> getClassesWithCircularReferences(Object object) {
        CyclicReferenceDetector cyclicReferenceDetector = new CyclicReferenceDetector();

        if (object != null) {
            cyclicReferenceDetector.detectCircularReferenceOnObject(object);
        }

        return cyclicReferenceDetector.circularReferenceTypes;
    }
    
    /**
     * Detects classes that have circular reference.
     * 
     * @param object the object to check if it has circular reference fields
     * @param clazz the class being used (necessary if we also checking super class as getDeclaredFields only returns
     *              fields of a given class, but not its super class)
     */
    private void detectCircularReferenceOnFields(Object object, Class<?> clazz) {
        if (object == null) {
            return;
        }

        nodesInPaths.add(object);

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Object fieldValue = field.get(object);
                if (fieldValue != null) {
                    detectCircularReferenceOnObject(fieldValue);
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
     * 
     * @param object the object to detect circular reference on
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void detectCircularReferenceOnObject(Object object) {
        if (object instanceof Iterable) {
            detectCircularReferenceFromObjectsContainedInAnIterable((Iterable) object);
        } else if (object instanceof Map) {
            detectCircularReferencesFromObjectsInAMap((Map) object);
        } else if (nodesInPaths.contains(object)) {
            circularReferenceTypes.add(object.getClass());
            return;
        }

        if (validateAnObject(object)) {
            detectCircularReferenceOnFields(object, object.getClass());
        }
    }

    /**
     * For objects with super classes, check for objects subject to circular reference in super class.
     * 
     * @param object the object to check if it has circular reference
     * @param clazz Used to prevent stackOverFlow exception
     */
    private void detectCircularReferencesFromTheSuperClass(Object object, Class<?> clazz) {
        Class<?> superclass = object.getClass().getSuperclass();

        if (superclass != Object.class && superclass != clazz && validateAnObject(object)) {
            detectCircularReferenceOnFields(object, superclass);
        }
    }

    /**
     * Detects circular references on {@link Map}s, i.e HashMap, TreeMap, etc.
     * 
     * @param map the {@link Map} with objects to checks for cyclic references on
     */
    private void detectCircularReferencesFromObjectsInAMap(Map<Object, Object> map) {
        nodesInPaths.remove(map);
        detectCircularReferenceFromObjectsContainedInAnIterable(map.values());
        detectCircularReferenceFromObjectsContainedInAnIterable(map.keySet());
    }

    /**
     * Detects circular references on {@link Iterable}s, i.e {@link Collection}s.
     * 
     * @param iterable the object to iterate through.
     */
    private void detectCircularReferenceFromObjectsContainedInAnIterable(Iterable<Object> iterable) {
        nodesInPaths.remove(iterable);
        for (Object elementInCollection : iterable) {
            if (elementInCollection != null) {
                detectCircularReferenceOnObject(elementInCollection);
            }
        }
    }

    /**
     * Checks to see if the given object is primitive or wrapper class, {@link String}, instance of {@link Iterable},
     * instance of {@link Map} or instance of {@link Enum}.
     *
     * @param object The object to validate
     * @return true if the object is not primitive/wrapper class and not an instance of
     * 			{@link String}, {@link Iterable}, {@link Map} or {@link Enum})
     */
    private boolean validateAnObject(Object object) {
        return !isPrimitiveOrWrapper(object.getClass())
                && object.getClass() != String.class
                && !(object instanceof Iterable)
                && !(object instanceof Map)
                && !(object instanceof Enum);
    }
}
