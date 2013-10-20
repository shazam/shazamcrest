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

import static com.shazam.shazamcrest.matchers.ChildBeanMatchers.childField1EqualTo;
import static com.shazam.shazamcrest.model.Bean.Builder.bean;
import static com.shazam.shazamcrest.model.ChildBean.Builder.child;
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static com.shazam.shazamcrest.util.AssertionHelper.assertThat;
import static com.shazam.shazamcrest.util.AssertionHelper.sameBeanAs;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import com.shazam.shazamcrest.model.ParentBean.Builder;

/**
 * Tests which verify the possibility to match beans applying hamcrest matchers on specific fields.
 */
public class MatcherAssertCustomMatchingTest {

	@Test
	public void matchesPrimitiveWithCustomMatcher() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField2.childField1", equalTo("banana")));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchOnPrimitive() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField2.childField1", equalTo("kiwi")));
	}
	
	@Test
	public void matchesFieldWithCustomMatcher() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana").childField2(2));
		
		assertThat(actual, sameBeanAs(expected).with("parentField2", childField1EqualTo("banana")));
	}
	
	@Test
	public void matchesFieldWithChainOfCustomMatchers() {
		Builder expected = parent().parentField2(child().childField1("apple")).parentField1("kiwi");
		Builder actual = parent().parentField2(child().childField1("banana").childField2(2)).parentField1("strawberry");
		
		assertThat(actual, sameBeanAs(expected).with("parentField2", childField1EqualTo("banana")).with("parentField1", equalTo("strawberry")));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchOnField() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField2", childField1EqualTo("kiwi")));
	}
	
	@Test
	public void matchesItemInCollectionWithCustomMatcher() {
		Builder expected = parent().addParentField3(child().childField1("kiwi"));
		Builder actual = parent().addParentField3(child().childField1("apple")).addParentField3(child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField3", hasItem(childField1EqualTo("banana"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchACollection() {
		Builder expected = parent().addParentField3(child().childField1("kiwi"));
		Builder actual = parent().addParentField3(child().childField1("apple")).addParentField3(child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField3", hasItem(childField1EqualTo("kiwi"))));
	}
	
	@Test
	public void matchesItemInMap() {
		Builder expected = parent().parentField4("key", child().childField1("apple"));
		Builder actual = parent().parentField4("key", child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField4", hasEntry(equalTo("key"), childField1EqualTo("banana"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchAMap() {
		Builder expected = parent().parentField4("key", child().childField1("apple"));
		Builder actual = parent().parentField4("key", child().childField1("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("parentField4", hasEntry(equalTo("key"), childField1EqualTo("kiwi"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenActualIsNull() {
		MatcherAssert.assertThat(null, sameBeanAs(bean()).with("field1", startsWith("field")));
	}
}
