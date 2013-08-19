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
import static com.shazam.shazamcrest.model.ChildBean.Builder.child;
import static com.shazam.shazamcrest.model.ParentBean.Builder.parent;
import static com.shazam.shazamcrest.util.AssertionHelper.assertThat;
import static com.shazam.shazamcrest.util.AssertionHelper.sameBeanAs;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.fail;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import com.shazam.shazamcrest.model.ParentBean.Builder;

/**
 * Tests which verify the diagnostic displayed when a custom matcher fails.
 */
public class MatcherAssertCustomMatchingDiagnosticTest {

	@Test
	public void includesDescriptionAndMismatchDescriptionForFailingMatcherOnPrimiteField() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana"));
		
		try {
			assertThat(actual, sameBeanAs(expected).with("parentField2.childField1", equalTo("kiwi")));
			fail("Expected assertion error");
		} catch (AssertionError e) {
			MatcherAssert.assertThat(e.getMessage(), endsWith("and parentField2.childField1 \"kiwi\"\n     but: parentField2.childField1 was \"banana\""));
		}
	}
	
	@Test
	public void includesJsonSnippetOfNonPrimitiveFieldOnMatchFailure() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana").childField2(1));
		
		try {
			assertThat(actual, sameBeanAs(expected).with("parentField2", childField1EqualTo("kiwi")));
			fail("Expected assertion error");
		} catch (AssertionError e) {
			MatcherAssert.assertThat(e.getMessage(), endsWith("and parentField2 having string field \"kiwi\"\n     but: parentField2 string field was \"banana\"\n{\n  \"childField1\": \"banana\",\n  \"childField2\": 1\n}"));
		}
	}
	
	@Test
	public void doesNotIncludeJsonSnippetOnNullField() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent();
		
		try {
			assertThat(actual, sameBeanAs(expected).with("parentField2", childField1EqualTo("kiwi")));
			fail("Expected assertion error");
		} catch (AssertionError e) {
			MatcherAssert.assertThat(e.getMessage(), endsWith("and parentField2 having string field \"kiwi\"\n     but: parentField2 was null"));
		}
	}
	
	@Test
	public void throwsIllegalArgumentExceptionWhenFieldPathDoesNotExist() {
		Builder expected = parent().parentField2(child().childField1("apple"));
		Builder actual = parent().parentField2(child().childField1("banana"));
		
		try {
			assertThat(actual, sameBeanAs(expected).with("parentField2.nonExistingField", equalTo("kiwi")));
			fail("Expected assertion error");
		} catch (IllegalArgumentException e) {
			MatcherAssert.assertThat(e.getMessage(), endsWith("parentField2.nonExistingField does not exist"));
		}
	}
}
