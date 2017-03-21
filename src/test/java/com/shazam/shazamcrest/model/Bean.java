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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple bean used for tests
 */
@SuppressWarnings("unused")
public class Bean {
	private String string;
	private int integer;
	private Set<Bean> set;
	private Map<Bean, Bean> map;
	private HashSet<Bean> hashSet;
	private HashMap<Bean, Bean> hashMap;
	private Bean[] array;

	private Bean(Builder builder) {
		string = builder.string;
		integer = builder.integer;
		set = builder.set;
		map = builder.map;
		hashSet = builder.hashSet;
		hashMap = builder.hashMap;
		array = builder.array;
	}

	public String getField1() {
		return string;
	}

	public static class Builder {
		private String string;
		private int integer;
		private Set<Bean> set;
		private Map<Bean, Bean> map;
		private HashSet<Bean> hashSet;
		private HashMap<Bean, Bean> hashMap;
		private Bean[] array;

		public static Builder bean() {
			return new Builder();
		}

		public Builder string(String string) {
			this.string = string;
			return this;
		}

		public Builder integer(int integer) {
			this.integer = integer;
			return this;
		}

		public Builder set(Set<Bean> set) {
			this.set = set;
			return this;
		}

		public Builder map(Map<Bean, Bean> map) {
			this.map = map;
			return this;
		}

		public Builder hashSet(HashSet<Bean> hashSet) {
			this.hashSet = hashSet;
			return this;
		}

		public Builder hashMap(HashMap<Bean, Bean> hashMap) {
			this.hashMap = hashMap;
			return this;
		}

		public Builder array(Bean... array) {
			this.array = array;
			return this;
		}

		public Bean build() {
			return new Bean(this);
		}
	}
}
