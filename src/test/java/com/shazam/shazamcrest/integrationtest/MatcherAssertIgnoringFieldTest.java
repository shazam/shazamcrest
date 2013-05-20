/*
 * Copyright 2013 Shazam Entertainment Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
*/
package com.shazam.shazamcrest.integrationtest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.shazam.shazamcrest.MatcherAssert;

/**
 * Tests for {@link MatcherAssert} which verify that fields can be ignored from the comparison.
 */
public class MatcherAssertIgnoringFieldTest {

	@Test
	public void ignoresField() {
		TestBean bean = new TestBean("apple", 1);
		TestBean expected = new TestBean("banana", 1);
		
		assertThat(bean, sameBeanAs(expected).ignoring("field1"));
	}
	
	@Test
	public void ignoresFields() {
		TestBean bean = new TestBean("apple", 1);
		TestBean expected = new TestBean("banana", 2);
		
		assertThat(bean, sameBeanAs(expected).ignoring("field1").ignoring("field2"));
	}
	
	@Test
	public void ignoresFieldInNestedBean() {
		ParentTestBean bean = new ParentTestBean("apple", new TestBean("orange", 1));
		ParentTestBean expected = new ParentTestBean("apple", new TestBean("banana", 1));
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField2.field1"));
	}
	
	@Test
	public void allowsToSpecifySubpathOnNullObjects() {
		ParentTestBean bean = new ParentTestBean("apple", null);
		ParentTestBean expected = new ParentTestBean("apple", null);
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField2.nonExistingField"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenSubpathIsSpecifiedOnPrimitiveField() {
		ParentTestBean bean = new ParentTestBean("apple", new TestBean("orange", 1));
		ParentTestBean expected = new ParentTestBean("apple", new TestBean("banana", 1));
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField2.field1.subpath"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenPathDoesNotExist() {
		ParentTestBean bean = new ParentTestBean("apple", new TestBean("orange", 1));
		ParentTestBean expected = new ParentTestBean("apple", new TestBean("banana", 1));
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField2.field4"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinList() {
		String parentField1 = "apple";
		TestBean testBean = new TestBean("orange", 1);
		List<TestBean> list = listOf(new TestBean("banana", 1), new TestBean("grape", 1));
		List<TestBean> expectedList = listOf(new TestBean("kiwi", 1), new TestBean("plum", 1));
		
		ParentTestBean bean = new ParentTestBean(parentField1, testBean, list);
		ParentTestBean expected = new ParentTestBean(parentField1, testBean, expectedList);
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField3.field1"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinListThatContainsNullBeans() {
		String parentField1 = "apple";
		TestBean testBean = new TestBean("orange", 1);
		List<TestBean> list = listOf(new TestBean("banana", 1), null, new TestBean("grape", 1));
		List<TestBean> expectedList = listOf(new TestBean("kiwi", 1), null, new TestBean("plum", 1));
		
		ParentTestBean bean = new ParentTestBean(parentField1, testBean, list);
		ParentTestBean expected = new ParentTestBean(parentField1, testBean, expectedList);
		
		assertThat(bean, sameBeanAs(expected).ignoring("parentField3.field1"));
	}
	
	private List<TestBean> listOf(TestBean... testBeans) {
		List<TestBean> list = new ArrayList<TestBean>();
		for (TestBean testBean : testBeans) {
			list.add(testBean);
		}
		return list;
	}
}
