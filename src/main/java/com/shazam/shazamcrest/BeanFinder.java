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

import static java.util.Arrays.asList;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Returns the object corresponding to the path specified
 */
public class BeanFinder {

	public static Object findBeanAt(String fieldPath, Object object) {
		try {
			return findBeanAt(asList(fieldPath.split(Pattern.quote("."))), object);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(fieldPath + " does not exist");
		}
	}

	private static Object findBeanAt(List<String> fields, Object object) {
		for (Field field : object.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			if (headOf(fields).equals(field.getName())) {
				try {
					if (fields.size() == 1) {
						return field.get(object);
					} else {
						return findBeanAt(fields.subList(1, fields.size()), field.get(object));
					}
				} catch (IllegalAccessException e) {}
			}
		}
		
		throw new IllegalArgumentException();
	}

	private static String headOf(final Collection<String> paths) {
		return paths.iterator().next();
	}
}
