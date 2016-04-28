package com.shazam.shazamcrest;

import com.shazam.shazamcrest.model.BeanWithPrimitives;
/**
 * Abstract class for common methods used by the JsonMatcher tests.
 * @author Andras_Gyuro
 *
 */
public abstract class AbstractJsonMatcherTest {

	protected BeanWithPrimitives getBeanWithPrimitives(){
		short beanShort = 1;
		boolean beanBoolean = true;
		byte beanByte = 2;
		char beanChar = 'c';
		float beanFloat = 3f;
		int beanInt = 4;
		double beanDouble = 5d;
		long beanLong = 6L;

		BeanWithPrimitives bean = BeanWithPrimitives.Builder.beanWithPrimitives()
				.beanShort(beanShort)
				.beanBoolean(beanBoolean)
				.beanByte(beanByte)
				.beanChar(beanChar)
				.beanFloat(beanFloat)
				.beanInt(beanInt)
				.beanDouble(beanDouble)
				.beanLong(beanLong)
				.build();

		return bean;
	}

	protected String getBeanAsJsonString(){
		return "{ beanLong: 5, beanString: \"dummyString\", beanInt: 10  }";
	}
}
