/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CircularReferenceBean
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
