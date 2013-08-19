/*
 * Copyright 2013 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.shazam.shazamcrest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;

import java.util.HashMap;
import java.util.Map;

import org.junit.ComparisonFailure;
import org.junit.Test;

/**
 * MatcherAssert tests for beans containing maps
 */
public class MatcherAssertMapTest {

	@Test(expected = ComparisonFailure.class)
	public void failsWhenMapContainsDifferentBeansWithSameToStringValueForKeys() {
		Bean bean1 = new Bean(new Key("key1"), new Value("value"));
		Bean bean2 = new Bean(new Key("key2"), new Value("value"));
		
		assertThat(bean1, sameBeanAs(bean2));
	}
	
	@Test
	public void succeedsWhenMapContainsSameBeans() {
		Bean bean1 = new Bean(new Key("key1"), new Value("value"));
		Bean bean2 = new Bean(new Key("key1"), new Value("value"));
		
		assertThat(bean1, sameBeanAs(bean2));
	}
	
	private class Bean {
		private Map<Key, Value> map = new HashMap<Key, Value>();
		
		public Bean(Key key, Value value) {
			map.put(key, value);
		}
	}
	
	private class Key {
		@SuppressWarnings("unused")
		private String key;
		
		public Key(String key) {
			this.key = key;
		}
		
		@Override
		public String toString() {
			return "key";
		}
	}
	
	private class Value {
		@SuppressWarnings("unused")
		private String value;
		
		public Value(String value) {
			this.value = value;
		}
	}
}
