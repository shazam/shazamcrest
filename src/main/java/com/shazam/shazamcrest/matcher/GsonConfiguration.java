package com.shazam.shazamcrest.matcher;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.TypeAdapterFactory;

/**
 * Configuration file for {@link GsonProvider}.
 * @author Andras_Gyuro
 *
 */
public class GsonConfiguration {

    private List<TypeAdapterFactory> typeAdapterFactories = new ArrayList<TypeAdapterFactory>();
    private Map<Type, List<Object>> typeAdapters = new HashMap<Type, List<Object>>();
    private Map<Class<?>, List<Object>> typeHierarchyAdapter = new HashMap<Class<?>, List<Object>>();

    public void addTypeAdapterFactory(final TypeAdapterFactory factory) {
        typeAdapterFactories.add(factory);
    }

    public void addTypeAdapter(final Type key, final Object value) {
        if (typeAdapters.get(key) == null) {
            typeAdapters.put(key, new ArrayList<Object>());
            typeAdapters.get(key).add(value);
        } else {
            typeAdapters.get(key).add(value);
        }
    }

    public void addTypeHierarchyAdapter(final Class<?> key, final Object value) {
        if (typeHierarchyAdapter.get(key) == null) {
            typeHierarchyAdapter.put(key, new ArrayList<Object>());
            typeHierarchyAdapter.get(key).add(value);
        } else {
            typeHierarchyAdapter.get(key).add(value);
        }
    }

    public List<TypeAdapterFactory> getTypeAdapterFactories() {
        return typeAdapterFactories;
    }

    public Map<Type, List<Object>> getTypeAdapters() {
        return typeAdapters;
    }

    public Map<Class<?>, List<Object>> getTypeHierarchyAdapter() {
        return typeHierarchyAdapter;
    }

}
