package com.github.karsaig.approvalcrest.matcher.typeadapters;

import com.github.karsaig.approvalcrest.ReflectUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.newSetFromMap;

/**
 * A TypeAdapterFactory that serializes objects from locked modules using their public getter methods.
 * <p>
 * This is the last-resort fallback (Tier 3) for when standard reflection cannot reach the fields
 * and no module can be opened. It produces JSON based on public getX()/isX() methods instead.
 * <p>
 * Active when:
 * <ul>
 *   <li>Mode is "fallback" (forces this factory regardless of Unsafe availability), OR</li>
 *   <li>Mode is "safe" AND no module could be opened (no agent attached, and Unsafe unavailable)
 *       AND the type is in — or inherits fields from — a locked module</li>
 * </ul>
 * <p>
 * The JSON format differs from field-based serialization (different property names/structure),
 * but this is expected — it's the price of functioning without --add-opens on future JDKs.
 */
public class GetterBasedTypeAdapterFactory implements TypeAdapterFactory {

    private static final ThreadLocal<Set<Object>> VISITED = ThreadLocal.withInitial(
            () -> newSetFromMap(new IdentityHashMap<>()));

    private final Set<Class<?>> additionalSkipTypes;

    public GetterBasedTypeAdapterFactory() {
        this(java.util.Collections.emptySet());
    }

    public GetterBasedTypeAdapterFactory(Set<Class<?>> additionalSkipTypes) {
        this.additionalSkipTypes = additionalSkipTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();

        if (ReflectUtil.isForceMode()) {
            return null;
        }

        // Skip types that Gson handles with built-in adapters
        if (shouldSkip(rawType)) {
            return null;
        }

        // A type that merely *inherits* locked-module fields counts too. Its own module is not
        // locked, so nothing else claims it, and Gson's reflective adapter then throws
        // JsonIOException on the inherited field instead of the type degrading to getters — as a
        // user's own exception class does on a JDK where no module can be opened.
        if (!ReflectUtil.isInLockedModule(rawType) && !ReflectUtil.inheritsFromLockedModule(rawType)) {
            return null;
        }

        // In fallback mode, always handle locked types here.
        // In safe mode, only handle if Unsafe is NOT available (with the agent attached the type
        // is not locked at all, so this factory is never reached for it)
        // (if Unsafe is available, UnsafeFieldTypeAdapterFactory handles it first).
        if (!ReflectUtil.isFallbackMode() && ReflectUtil.isUnsafeAvailable()) {
            return null;
        }

        List<GetterInfo> getters = collectGetters(rawType);
        if (getters.isEmpty()) {
            throw new IllegalStateException(
                    "approvalcrest cannot serialize type '" + rawType.getName() + "' in the current mode.\n"
                            + "The type is in a locked module and has no public accessor methods (getX(), isX(), or record-style).\n"
                            + "\nOptions:\n"
                            + "  1. Attach the approvalcrest agent, which opens modules with no JVM flags beyond it:\n"
                            + "       -javaagent:/path/to/approvalcrest-agent-<version>.jar\n"
                            + "     This is the recommended answer when the JDK denies sun.misc.Unsafe, because safe\n"
                            + "     mode cannot open modules on its own then.\n"
                            + "  2. Use the default 'safe' mode (no system property needed), which opens modules\n"
                            + "     automatically without the agent while sun.misc.Unsafe works.\n"
                            + "  3. Use 'force' mode with --add-opens: -DapprovalcrestReflection=force\n"
                            + "  4. Add --add-opens JVM flags for the specific module/package.\n"
                            + "\nSee https://github.com/karsaig/approvalcrest for details."
            );
        }

        return (TypeAdapter<T>) new GetterTypeAdapter(gson, getters);
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

    private List<GetterInfo> collectGetters(Class<?> type) {
        List<GetterInfo> result = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (isGetter(method)) {
                String name = derivePropertyName(method);
                result.add(new GetterInfo(method, name));
            }
        }
        return result;
    }

    private static final Set<String> EXCLUDED_METHOD_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
            "hashCode", "toString", "clone", "notify", "notifyAll", "wait", "getClass", "finalize",
            "fillInStackTrace"
    ));

    private boolean isGetter(Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (method.getParameterCount() != 0) {
            return false;
        }
        if (method.getReturnType() == void.class) {
            return false;
        }
        // Skip Object methods
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }
        String name = method.getName();
        if (EXCLUDED_METHOD_NAMES.contains(name)) {
            return false;
        }
        // Standard JavaBean getters: getX() or isX() for boolean
        if (name.startsWith("get") && name.length() > 3) {
            return true;
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return true;
        }
        // Record-style and fluent-style accessors: name(), value(), etc.
        // Any zero-arg non-void public method not in exclusion list
        return true;
    }

    private String derivePropertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            String property = name.substring(3);
            return Character.toLowerCase(property.charAt(0)) + property.substring(1);
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            String property = name.substring(2);
            return Character.toLowerCase(property.charAt(0)) + property.substring(1);
        }
        // Non-prefixed (record-style / fluent-style): use method name as-is
        return name;
    }

    private static class GetterInfo {
        final Method method;
        final String propertyName;

        GetterInfo(Method method, String propertyName) {
            this.method = method;
            this.propertyName = propertyName;
        }
    }

    private static class GetterTypeAdapter extends TypeAdapter<Object> {
        private final Gson gson;
        private final List<GetterInfo> getters;

        GetterTypeAdapter(Gson gson, List<GetterInfo> getters) {
            this.gson = gson;
            this.getters = getters;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            Set<Object> visited = VISITED.get();
            if (!visited.add(value)) {
                // Circular reference — write null to break cycle
                out.nullValue();
                return;
            }
            try {
                out.beginObject();
                for (GetterInfo gi : getters) {
                    try {
                        Object result = gi.method.invoke(value);
                        out.name(gi.propertyName);
                        if (result == null) {
                            out.nullValue();
                        } else {
                            @SuppressWarnings("unchecked")
                            TypeAdapter<Object> adapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(result.getClass()));
                            adapter.write(out, result);
                        }
                    } catch (Exception e) {
                        // Skip getters that throw
                    }
                }
                out.endObject();
            } finally {
                visited.remove(value);
            }
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            // Deserialization is not needed for approvalcrest (serialize-only comparison)
            JsonElement element = gson.getAdapter(JsonElement.class).read(in);
            if (element.isJsonNull()) {
                return null;
            }
            return element;
        }
    }

}
