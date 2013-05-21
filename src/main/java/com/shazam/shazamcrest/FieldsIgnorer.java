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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Responsible for removing from the Json representation of the bean to compare the ignored fields specified on the matcher.
 */
public class FieldsIgnorer {

	public static JsonElement ignorePaths(JsonElement jsonElement, Set<String> pathsToIgnore) {
		if (pathsToIgnore.isEmpty()) {
			return jsonElement;
		}
		
		String pathToIgnore = headOf(pathsToIgnore);
		List<String> paths = asList(pathToIgnore.split(Pattern.quote(".")));
		try {
			ignorePath(jsonElement, paths);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(pathToIgnore + " can't be ignored because does not exist");
		}
		return ignorePaths(jsonElement, removePathFromSet(pathsToIgnore, pathToIgnore));
	}

	private static Set<String> removePathFromSet(Set<String> setToRemoveFrom, String stringToRemove) {
		Set<String> set = new HashSet<String>(setToRemoveFrom);
		set.remove(stringToRemove);
		return set;
	}

	private static void ignorePath(JsonElement jsonElement, final List<String> paths) {
		String field = headOf(paths);
		if (paths.size() == 1) {
			if (jsonElement.isJsonPrimitive()) {
				throw new IllegalArgumentException();
			}
			removeFieldFromElement(jsonElement, field);
		} else {
			JsonElement child = ((JsonObject)jsonElement).get(field);
			List<String> tail = paths.subList(1, paths.size());
			
			if (child == null) {
				return;
			}
			
			if (child.isJsonArray()) {
				Iterator<JsonElement> iterator = ((JsonArray)child).iterator();
				while (iterator.hasNext()) {
					ignorePath((JsonElement) iterator.next(), tail);
				}
			} else {
				ignorePath(child, tail);
			}
		}
	}

	private static void removeFieldFromElement(JsonElement jsonElement, String head) {
		if (!jsonElement.isJsonNull()) {
			((JsonObject)jsonElement).remove(head);
		}
	}

	private static String headOf(final Collection<String> paths) {
		return paths.iterator().next();
	}
}
