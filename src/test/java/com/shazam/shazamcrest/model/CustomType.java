package com.shazam.shazamcrest.model;

public class CustomType {
    private Class classType;
    private String classTypeString;

    public CustomType(Class classType) {
        this.classType = classType;
        this.classTypeString=classType.getName();
    }

    public Class getClassType() {
        return classType;
    }

    public void setClassType(Class classType) {
        this.classType = classType;
    }

    public String getClassTypeString() {
        return classTypeString;
    }

    public void setClassTypeString(String classTypeString) {
        this.classTypeString = classTypeString;
    }
}
