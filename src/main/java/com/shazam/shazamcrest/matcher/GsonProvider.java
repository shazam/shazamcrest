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

import static com.google.common.collect.Sets.newTreeSet;
import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.graph.GraphAdapterBuilder;

/**
 * Provides an instance of {@link Gson}. If any class type has been ignored on the matcher, the {@link Gson} provided
 * will include an {@link ExclusionStrategy} which will skip the serialisation of fields for that type.
 */
@SuppressWarnings("rawtypes")
class GsonProvider {
    /**
     * Returns a {@link Gson} instance containing {@link ExclusionStrategy} based on the object types to ignore during
     * serialisation.
     *
     * @param typesToIgnore the object types to exclude from serialisation
     * @param circularReferenceTypes cater for circular referenced objects
     * @return an instance of {@link Gson}
     */
    public static Gson gson(final List<Class<?>> typesToIgnore, List<Class<?>> circularReferenceTypes) {
    	final GsonBuilder gsonBuilder = initGson();
    	
        if (!circularReferenceTypes.isEmpty()) {
            registerCircularReferenceTypes(circularReferenceTypes, gsonBuilder);
        }

        registerSetSerialisation(gsonBuilder);
        
        registerMapSerialisation(gsonBuilder);
        
        if (!typesToIgnore.isEmpty()) {
        	return registerTypesToIgnore(typesToIgnore, gsonBuilder).create();
        }
        
        return gsonBuilder.create();
    }

	private static GsonBuilder registerTypesToIgnore(final List<Class<?>> typesToIgnore, final GsonBuilder gsonBuilder) {
		return gsonBuilder.setExclusionStrategies(new ExclusionStrategy() {
		    @Override
		    public boolean shouldSkipField(FieldAttributes f) {
		        return false;
		    }
		    @Override
		    public boolean shouldSkipClass(Class<?> clazz) {
		        return (typesToIgnore.contains(clazz));
		    }
		});
	}

	private static void registerMapSerialisation(final GsonBuilder gsonBuilder) {
		gsonBuilder.registerTypeHierarchyAdapter(Map.class, new JsonSerializer<Map>() {
			@Override
			public JsonElement serialize(Map map, Type type, JsonSerializationContext context) {
				Gson gson = gsonBuilder.create();
				
        		ArrayListMultimap<Integer, Object> objects = mapObjectsByTheirJsonRepresentationHashCodes(map, gson);
        		return arrayOfObjectsOrderedByTheirJsonRepresentationHashCode(gson, objects, map);
			}
		});
	}

	private static void registerSetSerialisation(final GsonBuilder gsonBuilder) {
		gsonBuilder.registerTypeHierarchyAdapter(Set.class, new JsonSerializer<Set>() {
        	@Override
        	public JsonElement serialize(Set set, Type type, JsonSerializationContext context) {
        		Gson gson = gsonBuilder.create();

        		Set<Object> orderedSet = orderSetByElementsJsonRepresentation(set, gson);
        		return arrayOfObjectsOrderedByTheirJsonRepresentation(gson, orderedSet);
        	}
        });
	}

	private static void registerCircularReferenceTypes(List<Class<?>> circularReferenceTypes, GsonBuilder gsonBuilder) {
		GraphAdapterBuilder graphAdapterBuilder = new GraphAdapterBuilder();
		for (Class<?> circularReferenceType : circularReferenceTypes) {
		    graphAdapterBuilder.addType(circularReferenceType);
		}
		graphAdapterBuilder.registerOn(gsonBuilder);
	}
    
    @SuppressWarnings("unchecked")
	private static Set<Object> orderSetByElementsJsonRepresentation(Set set, final Gson gson) {
		Set<Object> objects = newTreeSet(new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return gson.toJson(o1).compareTo(gson.toJson(o2));
			}
		});
		objects.addAll(set);
		return objects;
	}
    
    @SuppressWarnings("unchecked")
	private static ArrayListMultimap<Integer, Object> mapObjectsByTheirJsonRepresentationHashCodes(Map map, Gson gson) {
    	ArrayListMultimap<Integer, Object> objects = ArrayListMultimap.create();
    	for (Entry<Object, Object> mapEntry : (Set<Map.Entry<Object, Object>>)map.entrySet()) {
    		objects.put(gson.toJson(mapEntry.getKey()).concat(gson.toJson(mapEntry.getValue())).hashCode(), mapEntry.getKey());
    	}
    	return objects;
    }

	private static JsonArray arrayOfObjectsOrderedByTheirJsonRepresentation(Gson gson, Set<Object> objects) {
		JsonArray array = new JsonArray();
		for (Object object : objects) {
			array.add(gson.toJsonTree(object));
		}
		return array;
	}
	
	private static JsonArray arrayOfObjectsOrderedByTheirJsonRepresentationHashCode(Gson gson, ArrayListMultimap<Integer, Object> objects, Map map) {
		ImmutableList<Integer> sortedMapKeySet = Ordering.natural().immutableSortedCopy(objects.keySet());
		JsonArray array = new JsonArray();
		if (allKeysArePrimitive(sortedMapKeySet, objects)) {
			for (Integer hashCode : sortedMapKeySet) {
				List<Object> objectsInTheSet = objects.get(hashCode);
				for (Object objectInTheSet : objectsInTheSet) {
					JsonObject jsonObject = new JsonObject();
					jsonObject.add(String.valueOf(objectInTheSet), gson.toJsonTree(map.get(objectInTheSet)));
					array.add(jsonObject);
				}
			}
		} else {
			for (Integer hashCode : sortedMapKeySet) {
				JsonArray keyValueArray = new JsonArray();
				List<Object> objectsInTheSet = objects.get(hashCode);
				for (Object objectInTheSet : objectsInTheSet) {
					keyValueArray.add(gson.toJsonTree(objectInTheSet));
					keyValueArray.add(gson.toJsonTree(map.get(objectInTheSet)));
					array.add(keyValueArray);
				}
			}
		}
		
		return array;
	}
	  
    private static boolean allKeysArePrimitive(ImmutableList<Integer> sortedMapKeySet, ArrayListMultimap<Integer, Object> objects) {
    	for (Integer integer : sortedMapKeySet) {
			List<Object> mapKeys = objects.get(integer);
			for (Object object : mapKeys) {
				if (!(isPrimitiveOrWrapper(object.getClass()) || object.getClass() == String.class || object.getClass().isEnum())) {
					return false;
				}
			}
		}
		return true;
	}

	private static GsonBuilder initGson() {
		return new GsonBuilder().setPrettyPrinting();
	}
}
