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
 * Bean with all java primitives, used for tests
 */
@SuppressWarnings("unused")
public class BeanWithPrimitives {
	private int beanInteger;
	private byte beanByte;
	private char beanChar;
	private short beanShort;
	private long beanLong;
	private float beanFloat;
	private double beanDouble;
	private boolean beanBoolean;

	private BeanWithPrimitives(Builder builder) {
		beanInteger = builder.beanInt;
		beanByte = builder.beanByte;
		beanChar = builder.beanChar;
		beanShort = builder.beanShort;
		beanLong = builder.beanLong;
		beanFloat = builder.beanFloat;
		beanDouble = builder.beanDouble;
		beanBoolean = builder.beanBoolean;
	}

	public static class Builder {
		private int beanInt;
		private byte beanByte;
		private char beanChar;
		private short beanShort;
		private long beanLong;
		private float beanFloat;
		private double beanDouble;
		private boolean beanBoolean;

		public static Builder beanWithPrimitives() {
			return new Builder();
		}

		public Builder beanInt(int beanInt) {
			this.beanInt = beanInt;
			return this;
		}

		public Builder beanByte(byte beanByte) {
			this.beanByte = beanByte;
			return this;
		}

		public Builder beanChar(char beanChar) {
			this.beanChar = beanChar;
			return this;
		}

		public Builder beanShort(short beanShort) {
			this.beanShort = beanShort;
			return this;
		}

		public Builder beanLong(long beanLong) {
			this.beanLong = beanLong;
			return this;
		}

		public Builder beanFloat(float beanFloat) {
			this.beanFloat = beanFloat;
			return this;
		}

		public Builder beanDouble(double beanDouble) {
			this.beanDouble = beanDouble;
			return this;
		}

		public Builder beanBoolean(boolean beanBoolean) {
			this.beanBoolean = beanBoolean;
			return this;
		}

		public BeanWithPrimitives build() {
			return new BeanWithPrimitives(this);
		}
	}
}
