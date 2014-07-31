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
	private String parentString;
	private ChildBean childBean;
	private List<ChildBean> childBeanList;
	private Map<String, ChildBean> childBeanMap;
	
	private ParentBean(Builder builder) {
		parentString = builder.parentString;
		childBean = builder.childBean;
		childBeanList = builder.childBeanList;
		childBeanMap = builder.childBeanMap;
	}
	
	public static class Builder {
		private String parentString;
		private ChildBean childBean;
		private List<ChildBean> childBeanList = new ArrayList<ChildBean>();
		private Map<String, ChildBean> childBeanMap = new HashMap<String, ChildBean>();

		public static Builder parent() {
			return new Builder();
		}
		
		public Builder parentString(String parentString) {
			this.parentString = parentString;
			return this;
		}
		
		public Builder childBean(ChildBean.Builder childBean) {
			this.childBean = childBean.build();
			return this;
		}
		
		public Builder childBean(String childString, int childInteger) {
			childBean(child().childString(childString).childInteger(childInteger));
			return this;
		}
		
		public Builder addToChildBeanList(ChildBean.Builder childBean) {
			childBeanList.add(childBean.build());
			return this;
		}
		
		public Builder addToChildBeanList(String childString, int childInteger) {
			addToChildBeanList(child().childString(childString).childInteger(childInteger));
			return this;
		}
		
		public Builder addToChildBeanList(ChildBean childBean) {
			childBeanList.add(childBean);
			return this;
		}
		
		public Builder putToChildBeanMap(String key, ChildBean.Builder childBean) {
			childBeanMap.put(key, childBean.build());
			return this;
		}
		
		public ParentBean build() {
			return new ParentBean(this);
		}
	}
}
