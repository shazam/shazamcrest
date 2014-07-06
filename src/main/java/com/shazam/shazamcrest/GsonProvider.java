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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.graph.GraphAdapterBuilder;

import java.util.List;

/**
 * Provides an instance of {@link Gson}. If any class type has been ignored on the matcher, the {@link Gson} provided
 * will include an {@link ExclusionStrategy} which will skip the serialisation/deserialisation of fields for that type.
 */
public class GsonProvider {
    private static GsonBuilder GSON = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting();

    /**
     * Returns a {@link Gson} instance containing {@link ExclusionStrategy} based on the object types to ignore during
     * serialisation/deserialisation.
     *
     * @param typesToIgnore          the object types to exclude from serialisation/deserialisation.
     * @param circularReferenceTypes cater for circular referenced objects
     * @return an instance of {@link Gson}
     */
    public static Gson gson(final List<Class<?>> typesToIgnore, List<Class<?>> circularReferenceTypes) {

        if (!circularReferenceTypes.isEmpty()) {
            final GraphAdapterBuilder graphAdapterBuilder = new GraphAdapterBuilder();
            for (final Class<?> circularReferenceType : circularReferenceTypes) {
                graphAdapterBuilder.addType(circularReferenceType);
            }
            graphAdapterBuilder.registerOn(GSON);
        }

        if (typesToIgnore.isEmpty()) {
            return GsonProvider.GSON.create();
        } else {
            return GSON.setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return false;
                }
                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return (typesToIgnore.contains(clazz));
                }
            }).create();
        }

    }
}
