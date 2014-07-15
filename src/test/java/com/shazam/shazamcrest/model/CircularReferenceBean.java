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

import static com.shazam.shazamcrest.model.CircularReferenceBean.Child.Builder.child;
import static com.shazam.shazamcrest.model.CircularReferenceBean.Parent.Builder.parent;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean containing a circular reference
 */
@SuppressWarnings("unused")
public class CircularReferenceBean {
	private Parent parent;

	private CircularReferenceBean(Builder builder) {
		parent = builder.parent;
	}

	public static final class Builder {
		private Parent parent;

		private Builder() {}
		
		public static Builder circularReferenceBean() {
			return new Builder();
		}
		
		public static Builder circularReferenceBean(final String parentAttribute, final String... childAttributes) {
	    	Parent parent = parent()
	    			.parentAttribute(parentAttribute)
	    			.build();
	    	for (String childAttribute : childAttributes) {
	    		Child child = child()
	    				.withChildAttribute(childAttribute)
	    				.withParent(parent)
	    				.build();
	    		parent.addChild(child);
			}

	        return circularReferenceBean().withParent(parent);
	    }

		public Builder withParent(Parent parent) {
			this.parent = parent;
			return this;
		}

		public CircularReferenceBean build() {
			return new CircularReferenceBean(this);
		}
	}

	public static class Child {
		private Parent parent;
		private String childAttribute;

		private Child(Builder builder) {
			parent = builder.parent;
			childAttribute = builder.childAttribute;
		}

		public String getAttribute1() {
			return childAttribute;
		}

		public Parent getParent() {
			return parent;
		}

		public static final class Builder {
			private Parent parent;
			private String childAttribute;

			private Builder() {}

			public static Builder child() {
				return new Builder();
			}

			public Builder withParent(Parent parent) {
				this.parent = parent;
				return this;
			}

			public Builder withChildAttribute(String childAttribute) {
				this.childAttribute = childAttribute;
				return this;
			}

			public Child build() {
				return new Child(this);
			}
		}
	}

	public static class Parent {
		private List<Child> children = new ArrayList<Child>();
		private String parentAttribute;

		private Parent(Builder builder) {
			this.children = builder.children;
			this.parentAttribute = builder.parentAttribute;
		}

		public void addChild(Child child) {
			children.add(child);
		}

		public static final class Builder {
			private List<Child> children = new ArrayList<Child>();
			private String parentAttribute;

			private Builder() {}
			
			public static Builder parent() {
				return new Builder();
			}

			public Builder parentAttribute(String parentAttribute) {
				this.parentAttribute = parentAttribute;
				return this;
			}

			public Parent build() {
				return new Parent(this);
			}
		}
	}
}
