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

import static com.shazam.shazamcrest.matchers.ChildBeanMatchers.childStringEqualTo;
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

import com.shazam.shazamcrest.model.ParentBean;

/**
 * Tests which verify the possibility to match beans applying hamcrest matchers on specific fields.
 */
public class MatcherAssertCustomMatchingTest {

	@Test
	public void matchesPrimitiveWithCustomMatcher() {
		ParentBean.Builder expected = parent().childBean(child().childString("apple"));
		ParentBean.Builder actual = parent().childBean(child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBean.childString", equalTo("banana")));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchOnPrimitive() {
		ParentBean.Builder expected = parent().childBean(child().childString("apple"));
		ParentBean.Builder actual = parent().childBean(child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBean.childString", equalTo("kiwi")));
	}
	
	@Test
	public void matchesFieldWithCustomMatcher() {
		ParentBean.Builder expected = parent().childBean(child().childString("apple"));
		ParentBean.Builder actual = parent().childBean(child().childString("banana").childInteger(2));
		
		assertThat(actual, sameBeanAs(expected).with("childBean", childStringEqualTo("banana")));
	}
	
	@Test
	public void matchesFieldWithChainOfCustomMatchers() {
		ParentBean.Builder expected = parent().childBean(child().childString("apple")).parentString("kiwi");
		ParentBean.Builder actual = parent().childBean(child().childString("banana").childInteger(2)).parentString("strawberry");
		
		assertThat(actual, sameBeanAs(expected).with("childBean", childStringEqualTo("banana")).with("parentString", equalTo("strawberry")));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchOnField() {
		ParentBean.Builder expected = parent().childBean(child().childString("apple"));
		ParentBean.Builder actual = parent().childBean(child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBean", childStringEqualTo("kiwi")));
	}
	
	@Test
	public void matchesItemInCollectionWithCustomMatcher() {
		ParentBean.Builder expected = parent().addToChildBeanList(child().childString("kiwi"));
		ParentBean.Builder actual = parent().addToChildBeanList(child().childString("apple")).addToChildBeanList(child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBeanList", hasItem(childStringEqualTo("banana"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchACollection() {
		ParentBean.Builder expected = parent().addToChildBeanList(child().childString("kiwi"));
		ParentBean.Builder actual = parent().addToChildBeanList(child().childString("apple")).addToChildBeanList(child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBeanList", hasItem(childStringEqualTo("kiwi"))));
	}
	
	@Test
	public void matchesItemInMap() {
		ParentBean.Builder expected = parent().putToChildBeanMap("key", child().childString("apple"));
		ParentBean.Builder actual = parent().putToChildBeanMap("key", child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBeanMap", hasEntry(equalTo("key"), childStringEqualTo("banana"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenCustomMatcherDoesNotMatchAMap() {
		ParentBean.Builder expected = parent().putToChildBeanMap("key", child().childString("apple"));
		ParentBean.Builder actual = parent().putToChildBeanMap("key", child().childString("banana"));
		
		assertThat(actual, sameBeanAs(expected).with("childBeanMap", hasEntry(equalTo("key"), childStringEqualTo("kiwi"))));
	}
	
	@Test(expected = AssertionError.class)
	public void failsWhenActualIsNull() {
		MatcherAssert.assertThat(null, sameBeanAs(bean()).with("string", startsWith("field")));
	}
}
