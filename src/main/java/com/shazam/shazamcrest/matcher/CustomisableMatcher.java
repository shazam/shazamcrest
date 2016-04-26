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

import org.hamcrest.Matcher;

/**
 * {@link Matcher} implementation where fields and object types can be skipped from the comparison, or matched with
 * custom matchers.
 */
public interface CustomisableMatcher<T> extends Matcher<T> {
	/**
	 * Specify the path of the field to be skipped from the matcher comparison.
	 * Example:
	 * <pre>sameBeanAs(expected).ignoring("beanField.subBeanField")</pre>
	 * 
	 * @param fieldPath the path of the field to be skipped from the comparison.
	 * @return the instance of the matcher
	 */
	CustomisableMatcher<T> ignoring(String fieldPath);

	/**
	 * Specify the object type of the fields to be skipped from the matcher comparison.
	 * Example:
	 * <pre>sameBeanAs(expected).ignoring(Bean.class)</pre>
	 * 
	 * @param clazz the object type to be skipped from the comparison.
	 * @return the instance of the matcher
	 */
	CustomisableMatcher<T> ignoring(Class<?> clazz);

    /**
	 * Specify the path of the field to be matched with a specific matcher.
	 * Example:
	 * <pre>sameBeanAs(expected).with("beanField.subBeanField", contains("element"))</pre>
	 * 
	 * @param fieldPath the path of the field to be matched with the provided matcher.
	 * @param matcher the Hamcrest matcher used to match the specified field.
	 * @return the instance of the matcher
	 */
	<V> CustomisableMatcher<T> with(String fieldPath, Matcher<V> matcher);

    /**
     * Specify a custom configuration for the Gson, for example, providing additional TypeAdapters.
     *
     * @param configuration {@link GsonConfiguration} object, containing TypeAdapterFactories, TypeAdapters and
     * TypeHierarchyAdapters.
     * @return the instance of the matcher
     */
    <V> CustomisableMatcher<T> withGsonConfiguration(GsonConfiguration configuration);

    /**
     * Specify the pattern of field names to ignore. Any bean property with a name that
     * matches the supplied pattern will be ignored.
     * Example:
     * <pre>assertThat(myBean, sameBeanAs(myResultBean).ignoring(is("mutationdate")).ignoring(containsString("version")))</pre>
     *
     * @param pattern the Hamcrest matcher used to match field names.
     * @return the instance of the matcher
     */
    CustomisableMatcher<T> ignoring(Matcher<String> fieldNamePattern);
}