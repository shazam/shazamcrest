/*
 * Copyright 2013 Shazam Entertainment Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.matcher;

import java.lang.reflect.Type;

import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

/**
 * Entry point for the matchers available in Shazamcrest.
 */
public class Matchers {

	/**
	 * Returns a {@link NullMatcher} in case the expectation is null, a
	 * {@link IsEqualMatcher} if it's a primitive, String or Enum
	 * or a {@link DiagnosingCustomisableMatcher} otherwise.
	 * 
	 * @param expected the expected bean to match against
	 * @return an {@link CustomisableMatcher} instance
	 */
	public static <T> CustomisableMatcher<T> sameBeanAs(final T expected) {
		if (expected == null) {
			return new NullMatcher<T>(expected);
		}
		
		if (isPrimitiveOrWrapper(expected.getClass()) || expected.getClass() == String.class || expected.getClass().isEnum()) {
			return new IsEqualMatcher<T>(expected);
		}
		
		return new DiagnosingCustomisableMatcher<T>(expected);
	}
	
		/**
	 * This allows you to register Gson Builder TypeAdaptors
	 * @param type This is the Class that you would like to change the Json representation of
	 * @param typeAdaptor this must be an implementation of JsonDeserializer/TypeAdator otherwise it will not know how to Render the JSON for your type
	 * @see /"https://google.github.io/gson/apidocs/com/google/gson/GsonBuilder.html#registerTypeAdapter-java.lang.reflect.Type-java.lang.Object-/"
	 */
	public static void registerCustomGsonTypeAdaptor(Type type,Object typeAdaptor) {
		GsonProvider.registerTypeAdapter(type,typeAdaptor);
	}

	/**
	 * This allows you to Remove a Gson Builder TypeAdaptor
	 * @param type The Class that you would remove from the Custom Gson Type Adaptors

	 */
	public static void unRegisterCustomGsonTypeAdaptor(Type type) {
		GsonProvider.removeCustomTypeAdapter(type);
	}


}
