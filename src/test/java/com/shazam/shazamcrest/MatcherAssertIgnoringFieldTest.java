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

import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static com.shazam.shazamcrest.model.ChildBean.Builder.child;
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static com.shazam.shazamcrest.util.AssertionHelper.assertThat;
import static com.shazam.shazamcrest.util.AssertionHelper.sameBeanAs;

import org.junit.ComparisonFailure;
import org.junit.Test;

import com.shazam.shazamcrest.model.Bean;
import com.shazam.shazamcrest.model.ChildBean;
import com.shazam.shazamcrest.model.ParentBean.Builder;

/**
 * Tests for {@link MatcherAssert} which verify that fields can be ignored from the comparison.
 */
public class MatcherAssertIgnoringFieldTest {

	@Test
	public void ignoresField() {
		Bean.Builder expected = bean().field1("banana");
		Bean.Builder actual = bean().field1("apple");
		
		assertThat(actual, sameBeanAs(expected).ignoring("field1"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchedEvenAfterIgnoringField() {
		Bean.Builder expected = bean().field1("banana").field2(2);
		Bean.Builder actual = bean().field1("apple");
		
		assertThat(actual, sameBeanAs(expected).ignoring("field1"));
	}
	
	@Test
	public void ignoresFields() {
		ChildBean.Builder expected = child().childField1("banana").childField2(1);
		ChildBean.Builder actual = child().childField1("apple").childField2(2);
		
		assertThat(actual, sameBeanAs(expected).ignoring("childField1").ignoring("childField2"));
	}
	
	@Test
	public void ignoresFieldInNestedBean() {
		Builder expected = parent().parentField2(child().childField1("banana"));
		Builder actual = parent().parentField2(child().childField1("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField2.childField1"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchEvenAfterIgnoringFieldInNestedBean() {
		Builder expected = parent().parentField1("expected").parentField2(child().childField1("banana"));
		Builder actual = parent().parentField2(child().childField1("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField2.childField1"));
	}
	
	@Test
	public void allowsToSpecifySubpathOnNullObjects() {
		Builder expected = parent().parentField1("banana");
		Builder actual = parent().parentField1("banana");
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField2.nonExistingField"));
	}
	
	@Test
	public void allowsToIgnoreNullObjects() {
		Builder expected = parent().parentField1("banana");
		Builder actual = parent().parentField1("banana");
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField2"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionWhenSubpathIsSpecifiedOnPrimitiveField() {
		Builder expected = parent().parentField2(child().childField1("banana"));
		Builder actual = parent().parentField2(child().childField1("orange"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField2.childField1.subpath"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinList() {
		Builder expected = parent()
				.addParentField3(child().childField1("kiwi"))
				.addParentField3(child().childField1("plum"));
		Builder actual = parent()
				.addParentField3(child().childField1("banana"))
				.addParentField3(child().childField1("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField3.childField1"));
	}
	
	@Test(expected = ComparisonFailure.class)
	public void failsWhenBeanDoesNotMatchAfterIgnoringFieldsInBeansWhitinList() {
		Builder expected = parent()
				.addParentField3(child().childField1("kiwi").childField2(2))
				.addParentField3(child().childField1("plum"));
		Builder actual = parent()
				.addParentField3(child().childField1("banana"))
				.addParentField3(child().childField1("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField3.childField1"));
	}
	
	@Test
	public void ignoresFieldsInBeansWhitinListThatContainsNullBeans() {
		Builder expected = parent()
				.addParentField3(child().childField1("kiwi"))
				.addParentField3((ChildBean)null)
				.addParentField3(child().childField1("plum"));
		Builder actual = parent()
				.addParentField3(child().childField1("banana"))
				.addParentField3((ChildBean)null)
				.addParentField3(child().childField1("grape"));
		
		assertThat(actual, sameBeanAs(expected).ignoring("parentField3.childField1"));
	}
}
