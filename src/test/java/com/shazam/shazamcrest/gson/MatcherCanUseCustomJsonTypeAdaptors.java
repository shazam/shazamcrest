package com.shazam.shazamcrest.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.shazam.shazamcrest.model.CustomType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.*;
import static org.hamcrest.CoreMatchers.not;


public class MatcherCanUseCustomJsonTypeAdaptors {
    private int typeAdaptorWriteCalls =0;
    private int typeAdaptorReadCalls=0;
    private CustomType customTypeString1;
    private CustomType customTypeString2;
    private CustomType customTypeInteger;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        customTypeString1 = new CustomType(String.class);
        customTypeString2 = new CustomType(String.class);
        customTypeInteger = new CustomType(Integer.class);
    }

    @Test
    public void testWhenNoTypeAdaptorIsRegister() {
        expectedException.expectMessage("Forgot to register a type adapter?");
        expectedException.expect(UnsupportedOperationException.class);
        doAssertion();
    }

    @Test
    @Ignore("Test Will fail untill I've Implemented the change")
    public void testThatAfterRegisteringATypeAdaptorExceptionIsNotThrown() {
        registerType();
        doAssertion();
    }

    private void doAssertion() {
        assertThat(customTypeString1, not(sameBeanAs(customTypeInteger)));
        assertThat(customTypeString2, not(sameBeanAs(customTypeInteger)));
        assertThat(customTypeString1, sameBeanAs(customTypeString2));

    }

    private void registerType() {
        
    }


}
