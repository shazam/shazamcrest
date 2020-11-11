package com.shazam.shazamcrest;

import org.junit.Test;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.Matchers.startsWith;

public class CustomMatcherOnInheritedFieldTest {

    @Test
    public void inheritedFieldsAreMatchedByCustomMatchers() {
        assertThat(new ChildClass(), sameBeanAs(new ChildClass())
                .with("packageProtectedField", startsWith("inherit"))
                .with("protectedField", startsWith("inherit"))
                .with("publicField", startsWith("inherit"))
        );
    }

    @Test
    public void notInheritedFieldsAreMatchedByCustomMatchers() {
        assertThat(new ChildClass(), sameBeanAs(new ChildClass())
                .with("childField", startsWith("child"))
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void inheritedPrivateFieldsCanNotBeMatched() {
        assertThat(new ChildClass(), sameBeanAs(new ChildClass())
                .with("privateField", startsWith("foo"))
        );
    }

    @SuppressWarnings("unused")
    private class GrandParentClass {
        private String privateField = "not inherited";
        String packageProtectedField = "inherited";
    }

    @SuppressWarnings("unused")
    private class ParentClass extends GrandParentClass {
        protected String protectedField = "inherited";
        public String publicField = "inherited";
    }

    @SuppressWarnings("unused")
    private class ChildClass extends ParentClass {
        private String childField = "child field";
    }
}
