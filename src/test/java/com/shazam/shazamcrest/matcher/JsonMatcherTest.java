package com.shazam.shazamcrest.matcher;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.anyString;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.shazam.shazamcrest.AbstractJsonMatcherTest;
import com.shazam.shazamcrest.model.BeanWithPrimitives;

/**
 * Unit test for the {@link JsonMatcher}.
 * Verifies creation of not approved files.
 * @author Andras_Gyuro
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonMatcherTest extends AbstractJsonMatcherTest{

	private static final String DUMMY_FILE_NAME = "fileName";
	private static final String DUMMY_APPROVED_FILE_NAME = DUMMY_FILE_NAME+"-approved";
	private static final String DUMMY_TEST_PATH = "DummyTestPath";
	private static final String DUMMY_CLASS_NAME = "DummyClassName";
	private static final String DUMMY_METHOD_NAME = "DummyMethodName";
	private static final String CLASS_HASH = "31a03e";
	private static final String METHOD_HASH = "f8e392";
	private static final String DUMMY_FILE_NAME_WITH_PATH = DUMMY_TEST_PATH+File.separator+ CLASS_HASH+File.separator+METHOD_HASH;
	private static final String DUMMY_COMMENT = DUMMY_CLASS_NAME+"."+DUMMY_METHOD_NAME;
	@Mock
	private JsonMatcherUtils utils;

	@Test
	public void testRunShouldCreateNotApprovedFileWhenNotExists() throws IOException{
		File dummyFile = new File(DUMMY_APPROVED_FILE_NAME);
		BeanWithPrimitives actual = getBeanWithPrimitives();

		when(utils.getCallerTestMethodName()).thenReturn(DUMMY_METHOD_NAME);
		when(utils.getCallerTestClassName()).thenReturn(DUMMY_CLASS_NAME);
		when(utils.getApproved(DUMMY_FILE_NAME_WITH_PATH)).thenReturn(dummyFile);
		when(utils.getCallerTestClassPath()).thenReturn(DUMMY_TEST_PATH);
		when(utils.createNotApproved(Mockito.eq(DUMMY_FILE_NAME_WITH_PATH), anyString(), eq(DUMMY_COMMENT))).thenReturn(DUMMY_FILE_NAME);
		JsonMatcher<BeanWithPrimitives> matcher =new JsonMatcher<BeanWithPrimitives>();
		matcher.setJsonMatcherUtils(utils);

		try{
			assertThat(actual, matcher);
		}catch(AssertionError er){
			assertThat(er.getMessage(), is(getNotApprovedCreationMessage()));
		}
	}

	@Test
	public void testRunShouldCreateNotApprovedFileWhenNotExistsAndModelAsString() throws IOException{
		File dummyFile = new File(DUMMY_APPROVED_FILE_NAME);
		String actual = getBeanAsJsonString();

		when(utils.getCallerTestMethodName()).thenReturn(DUMMY_METHOD_NAME);
		when(utils.getCallerTestClassName()).thenReturn(DUMMY_CLASS_NAME);
		when(utils.getApproved(DUMMY_FILE_NAME_WITH_PATH)).thenReturn(dummyFile);
		when(utils.getCallerTestClassPath()).thenReturn(DUMMY_TEST_PATH);
		when(utils.createNotApproved(Mockito.eq(DUMMY_FILE_NAME_WITH_PATH), anyString(), eq(DUMMY_COMMENT))).thenReturn(DUMMY_FILE_NAME);
		JsonMatcher<String> matcher =new JsonMatcher<String>();
		matcher.setJsonMatcherUtils(utils);

		try{
			assertThat(actual, matcher);
		}catch(AssertionError er){
			assertThat(er.getMessage(), is(getNotApprovedCreationMessage()));
		}
	}

	private String getNotApprovedCreationMessage(){
		StringBuilder builder = new StringBuilder();
		builder.append("Not approved file created '");
		builder.append(CLASS_HASH);
		builder.append("\\");
		builder.append(DUMMY_FILE_NAME);
		builder.append("', please verify it's contents and rename it to '");
		builder.append(DUMMY_APPROVED_FILE_NAME);
		builder.append("'.");
		return builder.toString();
	}



}
