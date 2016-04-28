package com.shazam.shazamcrest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameJsonAsApproved;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.Test.None;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.shazam.shazamcrest.matcher.GsonConfiguration;
import com.shazam.shazamcrest.model.BeanWithPrimitives;
/**
 * Unit tests which verify the basic usage of the
 * {@link com.shazam.shazamcrest.matcher.Matchers#sameJsonAsApproved()} method.
 * @author Andras_Gyuro
 *
 */
public class JsonMatcherBeanWithPrimitivesTest extends AbstractJsonMatcherTest {

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

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelAsStringIsSameAsApprovedJson(){
		String model = getBeanAsJsonString();

		assertThat(model, sameJsonAsApproved());
	}

	@Test(expected = None.class)
	public void shouldNotThrowAssertionErrorWhenModelIsSameAsApprovedJsonWithGsonConfiguration(){
	  GsonConfiguration config = new GsonConfiguration();
	  Date date = new GregorianCalendar(2016, 4, 27, 13, 30).getTime();
	  config.addTypeAdapter(Long.class, new DummyStringJsonSerializer());

		assertThat(actual, sameJsonAsApproved().withGsonConfiguration(config));
	}

	private class DummyStringJsonSerializer implements JsonDeserializer<Long>,JsonSerializer<Long>  {

		private static final String LONG_SUFFIX = " Long_variable";

		@Override
		public Long deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
		        Long result = null;
		        if (!json.isJsonNull()) {
		            String asString = json.getAsString();
		            result = Long.parseLong(asString.replace(LONG_SUFFIX, ""));
		        }
		        return result;
		    }

		@Override
		public JsonElement serialize(final Long src, final Type typeOfSrc, final JsonSerializationContext context) {
		    return new JsonPrimitive(src+LONG_SUFFIX);
		}

}

}
