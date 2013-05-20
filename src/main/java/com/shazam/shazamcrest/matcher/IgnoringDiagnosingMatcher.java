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

import static com.shazam.shazamcrest.FieldsIgnorer.ignorePaths;
import static com.shazam.shazamcrest.GsonProvider.gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.shazam.shazamcrest.ComparisonDescription;

/**
 * Extends the functionalities of {@link DiagnosingMatcher} with the possibility to specify fields and object types to
 * ignore in the comparison.
 */
class IgnoringDiagnosingMatcher<T> extends DiagnosingMatcher<T> implements IgnoringMatcher<T> {
	private static final JsonParser jsonParser = new JsonParser();

	protected final List<Class<?>> typesToIgnore = new ArrayList<Class<?>>();
	private final Set<String> pathsToIgnore = new HashSet<String>();
	private final T expected;

	public IgnoringDiagnosingMatcher(T expected) {
		this.expected = expected;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(filterJson(expected));
	}

	@Override
	protected boolean matches(Object actual, Description mismatchDescription) {
		String expectedJson = filterJson(expected);
		String actualJson = filterJson(actual);

		return assertEquals(expectedJson, actualJson, mismatchDescription);
	}

	@Override
	public IgnoringMatcher<T> ignoring(String string) {
		pathsToIgnore.add(string);
		return this;
	}

	@Override
	public IgnoringMatcher<T> ignoring(Class<?> clazz) {
		typesToIgnore.add(clazz);
		return this;
	}

	protected boolean appendMismatchDescription(Description mismatchDescription, String expectedJson, String actualJson, String message) {
		if (mismatchDescription instanceof ComparisonDescription) {
			ComparisonDescription shazamMismatchDescription = (ComparisonDescription) mismatchDescription;
			shazamMismatchDescription.setComparisonFailure(true);
			shazamMismatchDescription.setExpected(expectedJson);
			shazamMismatchDescription.setActual(actualJson);
			shazamMismatchDescription.setDifferencesMessage(message);
		}
		mismatchDescription.appendText(message);
		return false;
	}

	private boolean assertEquals(final String expectedJson, String actualJson, Description mismatchDescription) {
		try {
			JSONAssert.assertEquals(expectedJson, actualJson, true);
		} catch (AssertionError e) {
			return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, e.getMessage());
		} catch (JSONException e) {
			return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, e.getMessage());
		}

		return true;
	}

	private String filterJson(Object bean) {
		Gson gson = gson(typesToIgnore);
		String unfilteredJson = gson.toJson(bean);
		JsonElement filteredJson = ignorePaths(jsonParser.parse(unfilteredJson), pathsToIgnore);

		return gson.toJson(filteredJson);
	}
}