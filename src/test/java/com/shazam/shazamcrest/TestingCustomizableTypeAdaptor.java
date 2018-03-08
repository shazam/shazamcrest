package com.shazam.shazamcrest;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.shazam.shazamcrest.matcher.CustomisableMatcher;
import com.shazam.shazamcrest.model.CustomTypeAdapted;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

public class TestingCustomizableTypeAdaptor {

    private CustomTypeAdapted expected;
    @Rule
    public ExpectedException expectedException=ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        expected = new CustomTypeAdapted(Object.class, "String Field");
    }

    @Test()
    public void testCurrentBehaviourForTypeInferenceOnClass() {
        expectedException.expectMessage("Attempted to serialize java.lang.Class: java.lang.Object. Forgot to register a type adapter?");
        expectedException.expect(UnsupportedOperationException.class);
        assertThat(new CustomTypeAdapted(expected),sameBeanAs(expected));
    }

    @Test()
    public void testCurrentBehaviourForTypeInferenceOnClassUsingIgnoring() {
        expectedException.expectMessage("Attempted to serialize java.lang.Class: java.lang.Object. Forgot to register a type adapter?");
        expectedException.expect(UnsupportedOperationException.class);
        assertThat(new CustomTypeAdapted(expected),sameBeanAs(expected).ignoring("t"));
    }

    @Test
    public void testAddedTypeAdaptorAssertionFailure() {
        CustomisableMatcher<CustomTypeAdapted> matcher = sameBeanAs(expected).usingTypeAdaptors(getTypeAdaptors());
        try {
            assertThat(new CustomTypeAdapted(Object.class, "notSame"), matcher);
        }
        catch (ComparisonFailure comparisonFailure) {
            assertThat(comparisonFailure.getExpected(),equalTo("{\n  \"t\": \"java.lang.Object\",\n  \"stringField\": \"String Field\"\n}"));
        }
    }

    @Test
    public void testAddedTypeAdaptor() {
        CustomisableMatcher<CustomTypeAdapted> matcher = sameBeanAs(expected).usingTypeAdaptors(getTypeAdaptors());
        assertThat(new CustomTypeAdapted(expected), matcher);
    }

    private HashMap<Class,TypeAdapter> getTypeAdaptors() {
        HashMap<Class, TypeAdapter> typeAdapters = new HashMap<Class, TypeAdapter>();
        typeAdapters.put(CustomTypeAdapted.class,createTypeAdaptor());
        return typeAdapters;
    }

    private TypeAdapter createTypeAdaptor() {
        return new TypeAdapter<CustomTypeAdapted>() {

            @Override
            public void write(JsonWriter out, CustomTypeAdapted value) throws IOException {
                out.beginObject();
                if (value==null) {
                    out.nullValue();
                }
                else {
                    for (Field field : CustomTypeAdapted.class.getDeclaredFields()) {
                        out.name(field.getName());
                        if (field.getType().equals(Class.class)) {
                            try {
                                field.setAccessible(true);
                                out.value(((Class)field.get(value)).getName());
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            try {
                                field.setAccessible(true);
                                out.value(String.valueOf(field.get(value)));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                out.endObject();
            }

            @Override
            public CustomTypeAdapted read(JsonReader in) throws IOException {
                throw new UnsupportedOperationException("should notbe used");
                //return null;
            }
        };
    }

}
