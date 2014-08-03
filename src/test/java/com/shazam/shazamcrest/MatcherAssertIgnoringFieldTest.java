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

import static com.google.common.collect.Maps.newHashMap;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static com.shazam.shazamcrest.model.ChildBean.Builder.child;
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static com.shazam.shazamcrest.util.AssertionHelper.assertThat;
import static com.shazam.shazamcrest.util.AssertionHelper.sameBeanAs;

import java.util.Map;

import org.junit.ComparisonFailure;
import org.junit.Test;

import com.shazam.shazamcrest.model.Bean;
import com.shazam.shazamcrest.model.ChildBean;
import com.shazam.shazamcrest.model.ParentBean;

/**
 * Tests for {@link MatcherAssert} which verify that fields can be ignored from the comparison.
 */
public class MatcherAssertIgnoringFieldTest {

	@Test
	public void ignoresField() {
		Bean.Builder expected = bean().string("banana");
		Bean.Builder actual = bean().string("apple");
		
		assertThat(actual, sameBeanAs(expected).ignoring("string"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchedEvenAfterIgnoringField() {
		Bean.Builder expected = bean().string("banana").integer(2);
		Bean.Builder actual = bean().string("apple");
		
		assertThat(actual, sameBeanAs(expected).ignoring("string"));
	}
	
	@Test
	public void ignoresFields() {
		ChildBean.Builder expected = child().childString("banana").childInteger(1);
		ChildBean.Builder actual = child().childString("apple").childInteger(2);
		
		assertThat(actual, sameBeanAs(expected).ignoring("childString").ignoring("childInteger"));
	}
	
	@Test
	public void ignoresFieldInNestedBean() {
		ParentBean.Builder expected = parent().childBean(child().childString("banana"));
		ParentBean.Builder actual = parent().childBean(child().childString("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBean.childString"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchEvenAfterIgnoringFieldInNestedBean() {
		ParentBean.Builder expected = parent().parentString("expected").childBean(child().childString("banana"));
		ParentBean.Builder actual = parent().childBean(child().childString("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBean.childString"));
	}
	
	@Test
	public void allowsToSpecifySubpathOnNullObjects() {
		ParentBean.Builder expected = parent().parentString("banana");
		ParentBean.Builder actual = parent().parentString("banana");
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBean.nonExistingField"));
	}
	
	@Test
	public void allowsToIgnoreNullObjects() {
		ParentBean.Builder expected = parent().parentString("banana");
		ParentBean.Builder actual = parent().parentString("banana");
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBean"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenSubpathIsSpecifiedOnPrimitiveField() {
		ParentBean.Builder expected = parent().childBean(child().childString("banana"));
		ParentBean.Builder actual = parent().childBean(child().childString("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBean.childString.subpath"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinList() {
		ParentBean.Builder expected = parent()
				.addToChildBeanList(child().childString("kiwi"))
				.addToChildBeanList(child().childString("plum"));
		ParentBean.Builder actual = parent()
				.addToChildBeanList(child().childString("banana"))
				.addToChildBeanList(child().childString("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBeanList.childString"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchAfterIgnoringFieldsInBeansWhitinList() {
		ParentBean.Builder expected = parent()
				.addToChildBeanList(child().childString("kiwi").childInteger(2))
				.addToChildBeanList(child().childString("plum"));
		ParentBean.Builder actual = parent()
				.addToChildBeanList(child().childString("banana"))
				.addToChildBeanList(child().childString("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBeanList.childString"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinListThatContainsNullBeans() {
		ParentBean.Builder expected = parent()
				.addToChildBeanList(child().childString("kiwi"))
				.addToChildBeanList((ChildBean)null)
				.addToChildBeanList(child().childString("plum"));
		ParentBean.Builder actual = parent()
				.addToChildBeanList(child().childString("banana"))
				.addToChildBeanList((ChildBean)null)
				.addToChildBeanList(child().childString("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("childBeanList.childString"));
	}
	
	@Test
	public void ignoresFieldsInMapWhereKeyIsString() {
		Map<Object, Object> expectedMap = newHashMap();
		expectedMap.put("key", bean().integer(1).string("value").build());
		MapContainer expected = new MapContainer(expectedMap);

		Map<Object, Object> actualMap = newHashMap();
		actualMap.put("key", bean().integer(1).string("unexpected value").build());
		MapContainer actual = new MapContainer(actualMap);
		
		assertThat(actual, sameBeanAs(expected).ignoring("map.key.string"));
	}
	
	@Test
	public void ignoresFieldsInMapWhereKeyIsPrimitive() {
		Map<Object, Object> expectedMap = newHashMap();
		expectedMap.put(1, bean().integer(1).string("value").build());
		MapContainer expected = new MapContainer(expectedMap);
		
		Map<Object, Object> actualMap = newHashMap();
		actualMap.put(1, bean().integer(1).string("unexpected value").build());
		MapContainer actual = new MapContainer(actualMap);
		
		assertThat(actual, sameBeanAs(expected).ignoring("map.1.string"));
	}
	
	@Test
	public void ignoresFieldsInMapWhereKeyIsEnum() {
		Map<Object, Object> expectedMap = newHashMap();
		expectedMap.put(TestEnum.ONE, bean().integer(1).string("value").build());
		MapContainer expected = new MapContainer(expectedMap);
		
		Map<Object, Object> actualMap = newHashMap();
		actualMap.put(TestEnum.ONE, bean().integer(1).string("unexpected value").build());
		MapContainer actual = new MapContainer(actualMap);
		
		assertThat(actual, sameBeanAs(expected).ignoring("map.ONE.string"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenLastPathSegmentDoesNotExistInMap() {
		Map<Object, Object> expectedMap = newHashMap();
		expectedMap.put(bean().string("key").build(), bean().string("value").build());
		MapContainer expected = new MapContainer(expectedMap);

		Map<Object, Object> actualMap = newHashMap();
		actualMap.put(bean().string("key").build(), bean().string("unexpected value").build());
		MapContainer actual = new MapContainer(actualMap);
		
		assertThat(actual, sameBeanAs(expected).ignoring("map.key"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenPathDoesNotExistInMap() {
		Map<Object, Object> expectedMap = newHashMap();
		expectedMap.put(bean().string("key").build(), bean().string("value").build());
		MapContainer expected = new MapContainer(expectedMap);
		
		Map<Object, Object> actualMap = newHashMap();
		actualMap.put(bean().string("key").build(), bean().string("unexpected value").build());
		MapContainer actual = new MapContainer(actualMap);
		
		assertThat(actual, sameBeanAs(expected).ignoring("map.key.subpath"));
	}
	
	private enum TestEnum {
		ONE
	}
	
	private static class MapContainer {
		@SuppressWarnings("unused")
		private Map<Object, Object> map;
		
		public MapContainer(Map<Object, Object> map) {
			this.map = map;
		}
	}
}
