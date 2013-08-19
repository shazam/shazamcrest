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

/**
 * Simple bean used for tests
 */
@SuppressWarnings("unused")
public class Bean {
	private String field1;
	private int field2;

	private Bean(Builder builder) {
		field1 = builder.field1;
		field2 = builder.field2;
	}

	public String getField1() {
		return field1;
	}
	
	public static class Builder {
		private String field1;
		private int field2;

		public static Builder bean() {
			return new Builder();
		}
		
		public Builder field1(String field1) {
			this.field1 = field1;
			return this;
		}
		
		public Builder field2(int field2) {
			this.field2 = field2;
			return this;
		}
		
		public Bean build() {
			return new Bean(this);
		}
	}
}
