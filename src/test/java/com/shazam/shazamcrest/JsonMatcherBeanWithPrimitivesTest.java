package com.shazam.shazamcrest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameJsonAsApproved;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.Test.None;

import com.shazam.shazamcrest.model.BeanWithPrimitives;
/**
 * Unit tests which verify the basic usage of the
 * {@link com.shazam.shazamcrest.matcher.Matchers#sameJsonAsApproved()} method.
 * @author Andras_Gyuro
 *
 */
public class JsonMatcherBeanWithPrimitivesTest {

	private BeanWithPrimitives actual;

	@Before
	public void setUp(){
		actual = getBeanWithPrimitives();
	}


	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelIsSameAsApprovedJson(){
			assertThat(actual, sameJsonAsApproved());
	}

	@Test(expected = ComparisonFailure.class)
	public void shouldThrowAssertionErrorWhenModelDiffersFromApprovedJson(){
		assertThat(actual, sameJsonAsApproved());
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelDiffersFromApprovedJsonButFieldIsIgnored(){
		assertThat(actual, sameJsonAsApproved().ignoring("beanLong").ignoring("beanBoolean"));
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelDiffersFromApprovedJsonButFieldIsIgnoredWithMatcher(){
		Matcher<String> endsWithLongMatcher = Matchers.endsWith("Long");
		Matcher<String> endsWithBooleanMatcher = Matchers.endsWith("Boolean");

		assertThat(actual, sameJsonAsApproved().ignoring(endsWithLongMatcher).ignoring(endsWithBooleanMatcher));
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelDiffersFromApprovedJsonButFieldIsIgnoredWithClass(){
		Matcher<String> endsWithLongMatcher = Matchers.endsWith("Long");
		Matcher<String> endsWithBooleanMatcher = Matchers.endsWith("Boolean");

		assertThat(actual, sameJsonAsApproved().ignoring(Long.class).ignoring(Boolean.class));
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelIsSameAsApprovedJsonWithUniqueId(){
			assertThat(actual, sameJsonAsApproved().withUniqueId("idTest"));
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelIsSameAsApprovedJsonWithFileName(){
		assertThat(actual, sameJsonAsApproved().withFileName("bean-with-primitive-values"));
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelIsSameAsApprovedJsonWithFileNameAndPathName(){
		assertThat(actual, sameJsonAsApproved().withPathName("src/test/jsons").withFileName("bean-with-primitive-values-2"));
	}

	private BeanWithPrimitives getBeanWithPrimitives(){
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
}
