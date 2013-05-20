/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest.integrationtest;

import java.util.List;

import com.google.gson.annotations.Until;

/**
 * Parent bean of {@link TestBean}
 */
@SuppressWarnings("unused")
public class ParentTestBean {
	private String parentField1;
	private TestBean parentField2;
	private List<TestBean> parentField3;
	
	public ParentTestBean(String parentField1, TestBean parentField2) {
		this.parentField1 = parentField1;
		this.parentField2 = parentField2;
	}
	
	public ParentTestBean(String parentField1, TestBean parentField2, List<TestBean> parentField3) {
		this.parentField1 = parentField1;
		this.parentField2 = parentField2;
		this.parentField3 = parentField3;
	}
}
