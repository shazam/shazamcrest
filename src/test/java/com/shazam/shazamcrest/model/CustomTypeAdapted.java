package com.shazam.shazamcrest.model;

public class CustomTypeAdapted<T> {
    private Class<T> t;
    private String stringField;

    public CustomTypeAdapted(Class t, String stringField) {
        this.t = t;
        this.stringField = stringField;
    }

    public CustomTypeAdapted(CustomTypeAdapted expected) {
        this.t=expected.t;
        this.stringField=expected.stringField;
    }
}
