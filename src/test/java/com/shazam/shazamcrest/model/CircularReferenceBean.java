package com.shazam.shazamcrest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CircularReferenceBean
 * Created by Brian on 2014-07-06.
 */
@SuppressWarnings("unused")
public class CircularReferenceBean {

    private InnerParent parent;

    public CircularReferenceBean(InnerParent parent) {
        this.parent = parent;
    }

    private CircularReferenceBean(Builder builder) {
        parent = builder.parent;
    }

    public static Builder cyclicBean() {
        return new Builder();
    }

    public InnerParent getParent() {
        return parent;
    }

    public static class InnerChild{
        private InnerParent parent;
        private String attribute1;

        public InnerChild(InnerParent parent, String attribute1) {
            this.parent = parent;
            this.attribute1 = attribute1;
        }

        private InnerChild(Builder builder) {
            parent = builder.parent;
            attribute1 = builder.attribute1;
        }

        public static Builder innerChild() {
            return new Builder();
        }

        public String getAttribute1() {
            return attribute1;
        }

        public InnerParent getParent() {
            return parent;
        }

        public static final class Builder {
            private InnerParent parent;
            private String attribute1;

            private Builder() {
            }

            public Builder parent(InnerParent parent) {
                this.parent = parent;
                return this;
            }

            public Builder attribute1(String attribute1) {
                this.attribute1 = attribute1;
                return this;
            }

            public InnerChild build() {
                return new InnerChild(this);
            }
        }
    }
    public static class InnerParent{

        private List<InnerChild> innerChildren = new ArrayList<InnerChild>();
        private String parentAttribute;

        private InnerParent(Builder builder) {
            setInnerChildren(builder.innerChildren);
            setParentAttribute(builder.parentAttribute);
        }

        public static Builder innerParent() {
            return new Builder();
        }


        public List<InnerChild> getInnerChildren() {
            return innerChildren;
        }

        public void setInnerChildren(List<InnerChild> innerChildren) {
            this.innerChildren = innerChildren;
        }

        public String getParentAttribute() {
            return parentAttribute;
        }

        public void setParentAttribute(String parentAttribute) {
            this.parentAttribute = parentAttribute;
        }

        public static final class Builder {
            private List<InnerChild> innerChildren;
            private String parentAttribute;

            private Builder() {
            }

            public Builder innerChildren(List<InnerChild> innerChildren) {
                this.innerChildren = innerChildren;
                return this;
            }

            public Builder parentAttribute(String parentAttribute) {
                this.parentAttribute = parentAttribute;
                return this;
            }

            public InnerParent build() {
                return new InnerParent(this);
            }
        }
    }

    public static final class Builder {
        private InnerParent parent;

        private Builder() {
        }

        public Builder parent(InnerParent parent) {
            this.parent = parent;
            return this;
        }

        public CircularReferenceBean build() {
            return new CircularReferenceBean(this);
        }
    }
}
