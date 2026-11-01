package com.github.karsaig.approvalcrest.matcher.typeadapters;

import com.github.karsaig.approvalcrest.InaccessibleFieldException;
import com.github.karsaig.approvalcrest.ReflectUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.newSetFromMap;

/**
 * A TypeAdapterFactory that uses {@link ReflectUtil#getFieldValueViaUnsafe(Field, Object)}
 * to read fields from types in locked modules, producing JSON output identical to Gson's
 * built-in ReflectiveTypeAdapterFactory.
 * <p>
 * This factory is only active in "safe" mode and only handles types where:
 * <ul>
 *   <li>The type is in a locked module (cannot be accessed via setAccessible)</li>
 *   <li>Unsafe is available</li>
 * </ul>
 * <p>
 * When Unsafe is not available (denied, removed in a future JDK, or "fallback" mode), this factory
 * returns null. What happens next depends on whether a module could still be opened: with the
 * approvalcrest agent attached it can, so the type is not locked at all and Gson's own reflective
 * adapter serializes it field-based; without the agent the {@link GetterBasedTypeAdapterFactory}
 * handles it.
 */
public class UnsafeFieldTypeAdapterFactory implements TypeAdapterFactory {

    private static final ThreadLocal<Set<Object>> VISITED = ThreadLocal.withInitial(
            () -> newSetFromMap(new IdentityHashMap<>()));

    private final Set<Class<?>> additionalSkipTypes;

    public UnsafeFieldTypeAdapterFactory() {
        this(java.util.Collections.emptySet());
    }

    public UnsafeFieldTypeAdapterFactory(Set<Class<?>> additionalSkipTypes) {
        this.additionalSkipTypes = additionalSkipTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();

        if (ReflectUtil.isForceMode()) {
            return null;
        }

        // Don't intercept types that Gson handles natively
        if (shouldSkip(rawType)) {
            return null;
        }

        if (!ReflectUtil.isInLockedModule(rawType)) {
            return null;
        }

        if (!ReflectUtil.isUnsafeAvailable()) {
            return null;
        }

        List<FieldInfo> fields = collectFields(rawType);
        if (fields.isEmpty()) {
            return null;
        }

        return (TypeAdapter<T>) new UnsafeFieldTypeAdapter(gson, fields);
    }

    private boolean shouldSkip(Class<?> type) {
        if (type.isArray()) return true;
        if (type.isPrimitive()) return true;
        if (type.isEnum()) return true;
        if (type == String.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (Boolean.class == type) return true;
        if (Character.class == type) return true;
        if (Iterable.class.isAssignableFrom(type)) return true;
        if (java.util.Map.class.isAssignableFrom(type)) return true;
        if (java.util.Optional.class.isAssignableFrom(type)) return true;
        if (java.util.OptionalInt.class == type) return true;
        if (java.util.OptionalLong.class == type) return true;
        if (java.util.OptionalDouble.class == type) return true;
        if (com.google.common.base.Optional.class.isAssignableFrom(type)) return true;
        for (Class<?> skip : additionalSkipTypes) {
            if (skip.isAssignableFrom(type)) return true;
        }
        return false;
    }

    private List<FieldInfo> collectFields(Class<?> type) {
        List<FieldInfo> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
                    continue;
                }
                if (field.isSynthetic()) {
                    continue;
                }
                fields.add(new FieldInfo(field));
            }
        }
        return fields;
    }

    private static class FieldInfo {
        final Field field;
        final String name;

        FieldInfo(Field field) {
            this.field = field;
            this.name = field.getName();
        }
    }

    private static class UnsafeFieldTypeAdapter extends TypeAdapter<Object> {
        private final Gson gson;
        private final List<FieldInfo> fields;

        UnsafeFieldTypeAdapter(Gson gson, List<FieldInfo> fields) {
            this.gson = gson;
            this.fields = fields;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            Set<Object> visited = VISITED.get();
            if (!visited.add(value)) {
                // Circular reference — write null to break the cycle
                out.nullValue();
                return;
            }
            try {
                out.beginObject();
                for (FieldInfo fi : fields) {
                    try {
                        Object fieldValue = ReflectUtil.getFieldValueViaUnsafe(fi.field, value);
                        out.name(fi.name);
                        if (fieldValue == null) {
                            out.nullValue();
                        } else {
                            @SuppressWarnings("unchecked")
                            TypeAdapter<Object> adapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(fieldValue.getClass()));
                            adapter.write(out, fieldValue);
                        }
                    } catch (InaccessibleFieldException e) {
                        // Should not happen when Unsafe is available, but skip if it does
                    }
                }
                out.endObject();
            } finally {
                visited.remove(value);
            }
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            JsonElement element = gson.getAdapter(JsonElement.class).read(in);
            if (element.isJsonNull()) {
                return null;
            }
            return element;
        }
    }
}
