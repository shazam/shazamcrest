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

import static com.shazam.shazamcrest.model.ChildBean.Builder.child;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parent bean of {@link ChildBean}, used for tests
 */
@SuppressWarnings("unused")
public class ParentBean {
	private String parentField1;
	private ChildBean parentField2;
	private List<ChildBean> parentField3;
	private Map<String, ChildBean> parentField4;
	
	private ParentBean(Builder builder) {
		parentField1 = builder.parentField1;
		parentField2 = builder.parentField2;
		parentField3 = builder.parentField3;
		parentField4 = builder.parentField4;
	}
	
	public static class Builder {
		private String parentField1;
		private ChildBean parentField2;
		private List<ChildBean> parentField3 = new ArrayList<ChildBean>();
		private Map<String, ChildBean> parentField4 = new HashMap<String, ChildBean>();

		public static Builder parent() {
			return new Builder();
		}
		
		public Builder parentField1(String parentField1) {
			this.parentField1 = parentField1;
			return this;
		}
		
		public Builder parentField2(ChildBean.Builder parentField2) {
			this.parentField2 = parentField2.build();
			return this;
		}
		
		public Builder parentField2(String childField1, int childField2) {
			parentField2(child().childField1(childField1).childField2(childField2));
			return this;
		}
		
		public Builder addParentField3(ChildBean.Builder testBean) {
			parentField3.add(testBean.build());
			return this;
		}
		
		public Builder addParentField3(String field1, int field2) {
			addParentField3(child().childField1(field1).childField2(field2));
			return this;
		}
		
		public Builder addParentField3(ChildBean testBean) {
			parentField3.add(testBean);
			return this;
		}
		
		public Builder parentField4(String key, ChildBean.Builder testBean) {
			parentField4.put(key, testBean.build());
			return this;
		}
		
		public ParentBean build() {
			return new ParentBean(this);
		}
	}
}
