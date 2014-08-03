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

import static java.lang.Math.max;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;

/**
 * Responsible for traversing the Json tree and ignore the specified set of field paths.
 */
public class FieldsIgnorer {
	public static JsonElement findPaths(JsonElement jsonElement, Set<String> pathsToFind) {
		if (pathsToFind.isEmpty()) {
			return jsonElement;
		}
		
		String pathToFind = headOf(pathsToFind);
		List<String> pathSegments = asList(pathToFind.split(Pattern.quote(".")));
		try {
			findPath(jsonElement, pathToFind, pathSegments);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(pathToFind + " does not exist");
		}
		return findPaths(jsonElement, removePathFromSet(pathsToFind, pathToFind));
	}

	private static Set<String> removePathFromSet(Set<String> setToRemoveFrom, String stringToRemove) {
		Set<String> set = new HashSet<String>(setToRemoveFrom);
		set.remove(stringToRemove);
		return set;
	}

	private static void findPath(JsonElement jsonElement, String pathToFind, final List<String> pathSegments) {
		String field = headOf(pathSegments);
		if (pathSegments.size() == 1) {
			ignorePath(jsonElement, pathToFind);
		} else {
			if (!jsonElement.isJsonObject()) {
				throw new IllegalArgumentException();
			}
			JsonElement child = jsonElement.getAsJsonObject().get(field);
			List<String> tail = pathSegments.subList(1, pathSegments.size());
			
			if (child == null) {
				return;
			}
			
			if (child.isJsonArray()) {
				Iterator<JsonElement> iterator = child.getAsJsonArray().iterator();
				while (iterator.hasNext()) {
					findPath((JsonElement) iterator.next(), pathToFind, tail);
				}
			} else {
				findPath(child, pathToFind, tail);
			}
		}
	}

	private static void ignorePath(JsonElement jsonElement, String pathToIgnore) {
		if (!jsonElement.isJsonNull()) {
			if (!jsonElement.isJsonObject()) {
				throw new IllegalArgumentException();
			}
			jsonElement.getAsJsonObject().remove(getLastSegmentOf(pathToIgnore));
		}
	}
	
	private static String getLastSegmentOf(String fieldPath) {
		String[] paths = fieldPath.split(Pattern.quote("."));
		if (paths.length == 0) {
			return fieldPath;
		}
		
		return paths[max(0, paths.length-1)];
	}

	private static String headOf(final Collection<String> paths) {
		return paths.iterator().next();
	}
}
