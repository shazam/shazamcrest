package com.shazam.shazamcrest.matcher;

import static com.shazam.shazamcrest.BeanFinder.findBeanAt;
import static com.shazam.shazamcrest.CyclicReferenceDetector.getClassesWithCircularReferences;
import static com.shazam.shazamcrest.FieldsIgnorer.MARKER;
import static com.shazam.shazamcrest.FieldsIgnorer.findPaths;
import static com.shazam.shazamcrest.matcher.JsonMatcherUtils.SEPARATOR;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.shazam.shazamcrest.ComparisonDescription;

/**
 * <p>
 * Matcher for asserting expected DTOs. Searches for an approved JSON file in
 * the same directory as the test file:
 * <ul>
 * 		<li>If found, the matcher will assert the contents of the JSON file to the actual object,
 * which is serialized to a JSON String. </li>
 * 		<li>If not found, a non-approved JSON file is created, that must be
 * verified and renamed to "*-approved.json" by the developer. </li>
 * </ul>
 * The files and directories are hashed with SHA-1 algorithm by default to avoid too long file
 * and path names.
 * These are generated in the following way:
 * <ul>
 *   <li> the directory name is the first {@value #NUM_OF_HASH_CHARS} characters of the hashed <b>class name</b>. </li>
 *   <li> the file name is the first {@value #NUM_OF_HASH_CHARS} characters of the hashed <b>test method name</b>. </li>
 * </ul>
 *
 * This default behaviour can be overridden by using the {@link #withFileName(String)} for
 * custom file name and {@link #withPathName(String)} for custom path.
 * </p>
 *
 * @author Andras_Gyuro
 *
 */
public class JsonMatcher<T> extends DiagnosingMatcher<T> implements CustomisableMatcher<T> {

	private static final int NUM_OF_HASH_CHARS = 6;

	private String pathName;
	private String fileName;
	private String customFileName;
	private String fileNameWithPath;
	private String uniqueId;
	private String testClassName;
	private String testMethodName;
	private String testClassNameHash;

	private final Map<String, Matcher<?>> customMatchers = new HashMap<String, Matcher<?>>();
	private final List<Class<?>> typesToIgnore = new ArrayList<Class<?>>();
	private final List<Matcher<String>> patternsToIgnore = new ArrayList<Matcher<String>>();
	private final Set<Class<?>> circularReferenceTypes = new HashSet<Class<?>>();
	private JsonElement expected;
	private JsonMatcherUtils jsonMatcherUtils = new JsonMatcherUtils();

	private final Set<String> pathsToIgnore = new HashSet<String>();
	private GsonConfiguration configuration;

	@Override
	public void describeTo(final Description description) {
		Gson gson = GsonProvider.gson(typesToIgnore, patternsToIgnore, circularReferenceTypes, configuration);
		description.appendText(filterJson(gson, expected));
		for (String fieldPath : customMatchers.keySet()) {
			description.appendText("\nand ").appendText(fieldPath).appendText(" ")
					.appendDescriptionOf(customMatchers.get(fieldPath));
		}
	}

	@Override
	public JsonMatcher<T> ignoring(final String fieldPath) {
		pathsToIgnore.add(fieldPath);
		return this;
	}

	@Override
	public JsonMatcher<T> ignoring(final Class<?> clazz) {
		typesToIgnore.add(clazz);
		return this;
	}

	@Override
	public JsonMatcher<T> ignoring(final Matcher<String> fieldNamePattern) {
		patternsToIgnore.add(fieldNamePattern);
		return this;
	}

	@Override
	public <V> JsonMatcher<T> with(final String fieldPath, final Matcher<V> matcher) {
		throw new UnsupportedOperationException("JSON approval with custom matcher not yet supported!");
	}

	@Override
	public JsonMatcher<T> withGsonConfiguration(final GsonConfiguration configuration) {
		this.configuration = configuration;
		return this;
	}

	/**
	 * Concatenates a unique ID to the generated file name. Useful for test cases
	 * containing multiple verifications.
	 *
	 * @param uniqueId
	 *          a {@link String} object, that uniquely identifies the file.
	 * @return current instance
	 */
	public JsonMatcher<T> withUniqueId(final String uniqueId) {
		this.uniqueId = uniqueId;
		return this;
	}

	/**
	 * Sets the file name to the given parameter.
	 *
	 * @param customFileName
	 *          a {@link String} object, which will be the base file name. The
	 *          approved/not-approved identifier and file extension will still be
	 *          concatenated to this.
	 * @return current instance
	 */
	public JsonMatcher<T> withFileName(final String customFileName) {
		this.customFileName = customFileName;
		return this;
	}

	/**
	 * Sets the file path to the given parameter.
	 *
	 * @param fileName
	 *          a {@link String} object, which will be the file path.
	 * @return current instance
	 */
	public JsonMatcher<T> withPathName(final String pathName) {
		this.pathName = pathName;
		return this;
	}

	@Override
	protected boolean matches(final Object actual, final Description mismatchDescription) {
		boolean matches = false;
		circularReferenceTypes.addAll(getClassesWithCircularReferences(actual));
		init();
		Gson gson = GsonProvider.gson(typesToIgnore, patternsToIgnore, circularReferenceTypes, configuration);
		createNotApprovedFileIfNotExists(actual, gson);
		initExpectedFromFile();

		if (areCustomMatchersMatching(actual, mismatchDescription, gson)) {

			String expectedJson = filterJson(gson, expected);

			JsonElement actualJsonElement = getAsJsonElement(gson, actual);

			if (actual == null) {
				matches = appendMismatchDescription(mismatchDescription, expectedJson, "null", "actual was null");
			} else {
				String actualJson = filterJson(gson, actualJsonElement);

				matches = assertEquals(expectedJson, actualJson, mismatchDescription);
			}
		} else {
			matches = false;
		}
		return matches;
	}

	private void init() {
		testMethodName = jsonMatcherUtils.getCallerTestMethodName();
		testClassName = jsonMatcherUtils.getCallerTestClassName();

		if (customFileName == null || customFileName.trim().isEmpty()) {
			fileName = hashFileName(testMethodName);
		} else {
			fileName = customFileName;
		}
		if (uniqueId != null) {
			fileName += SEPARATOR + uniqueId;
		}
		if (pathName == null || pathName.trim().isEmpty()) {
			testClassNameHash = hashFileName(testClassName);
			pathName = jsonMatcherUtils.getCallerTestClassPath() + File.separator + testClassNameHash;
		}

		fileNameWithPath = pathName + File.separator + fileName;
	}

	private String hashFileName(final String fileName) {
		return Hashing.sha1().hashString(fileName, Charsets.UTF_8).toString().substring(0, NUM_OF_HASH_CHARS);
	}

	private JsonElement getAsJsonElement(final Gson gson, final Object object) {
		JsonElement result;
		if (object instanceof String) {
			JsonParser jsonParser = new JsonParser();
			result = jsonParser.parse((String) object);
		} else {
			result = gson.toJsonTree(object);
		}
		return result;

	}

	private void initExpectedFromFile() {
		File approvedFile = jsonMatcherUtils.getApproved(fileNameWithPath);

		try {
			String approvedJsonStr = readFile(approvedFile);
			JsonParser jsonParser = new JsonParser();
			expected = jsonParser.parse(approvedJsonStr);
		} catch (IOException e) {
			throw new IllegalStateException(
					String.format("Exception while initializing expected from file: %s", approvedFile.toString()), e);
		}
	}

	private String readFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append("\n");
		        line = br.readLine();
		    }
		    return sb.toString();
		} finally {
		    br.close();
		}
	}

	private String filterJson(final Gson gson, final JsonElement jsonElement) {
		Set<String> set = new HashSet<String>();
		set.addAll(pathsToIgnore);

		JsonElement filteredJson = findPaths(jsonElement, set);

		return removeSetMarker(gson.toJson(filteredJson));
	}

	private boolean assertEquals(final String expectedJson, final String actualJson,
			final Description mismatchDescription) {
		try {
			JSONAssert.assertEquals(expectedJson, actualJson, true);
		} catch (AssertionError e) {
			return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, getAssertMessage(e));
		} catch (JSONException e) {
			return appendMismatchDescription(mismatchDescription, expectedJson, actualJson, getAssertMessage(e));
		}

		return true;
	}

	private String getAssertMessage(final Throwable t) {
		String result;
		if (testClassNameHash == null) {
			result = "Expected file " + fileNameWithPath + "\n" + t.getMessage();
		} else {
			result = "Expected file " + testClassNameHash + File.separator + jsonMatcherUtils.getFullFileName(fileName, true)
					+ "\n" + t.getMessage();
		}
		return result;
	}

	private String removeSetMarker(final String json) {
		return json.replaceAll(MARKER, "");
	}

	private void createNotApprovedFileIfNotExists(final Object toApprove, final Gson gson) {
		File approvedFile = jsonMatcherUtils.getApproved(fileNameWithPath);

		if (!approvedFile.exists()) {
			try {
				String approvedFileName = approvedFile.getName();
				String content;
				if (String.class.isInstance(toApprove)) {
					JsonParser jsonParser = new JsonParser();
					JsonElement toApproveJsonElement = jsonParser.parse(String.class.cast(toApprove));
					content = removeSetMarker(gson.toJson(toApproveJsonElement));
				} else {
					content = removeSetMarker(gson.toJson(toApprove));
				}
				String createdFileName = jsonMatcherUtils.createNotApproved(fileNameWithPath, content, testClassName + "." + testMethodName);
				String message;
				if (testClassNameHash == null) {
					message = "Not approved file created '" + createdFileName
							+ "', please verify it's contents and rename it to '" + approvedFileName + "'.";
				} else {
					message = "Not approved file created '" + testClassNameHash + File.separator + createdFileName
							+ "', please verify it's contents and rename it to '" + approvedFileName + "'.";
				}
				fail(message);

			} catch (IOException e) {
				throw new IllegalStateException(
						String.format("Exception while creating not approved file %s", toApprove.toString()), e);
			}
		}
	}

	private boolean appendMismatchDescription(final Description mismatchDescription, final String expectedJson,
			final String actualJson, final String message) {
		if (mismatchDescription instanceof ComparisonDescription) {
			ComparisonDescription shazamMismatchDescription = (ComparisonDescription) mismatchDescription;
			shazamMismatchDescription.setComparisonFailure(true);
			shazamMismatchDescription.setExpected(expectedJson);
			shazamMismatchDescription.setActual(actualJson);
			shazamMismatchDescription.setDifferencesMessage(message);
		}
		mismatchDescription.appendText(message);
		return false;
	}

	private boolean areCustomMatchersMatching(final Object actual, final Description mismatchDescription,
			final Gson gson) {
		boolean result = true;
		Map<Object, Matcher<?>> customMatching = new HashMap<Object, Matcher<?>>();
		for (Entry<String, Matcher<?>> entry : customMatchers.entrySet()) {
			Object object = actual == null ? null : findBeanAt(entry.getKey(), actual);
			customMatching.put(object, customMatchers.get(entry.getKey()));
		}

		for (Entry<Object, Matcher<?>> entry : customMatching.entrySet()) {
			Matcher<?> matcher = entry.getValue();
			Object object = entry.getKey();
			if (!matcher.matches(object)) {
				appendFieldPath(matcher, mismatchDescription);
				matcher.describeMismatch(object, mismatchDescription);
				appendFieldJsonSnippet(object, mismatchDescription, gson);
				result = false;
			}
		}
		return result;
	}

	private void appendFieldJsonSnippet(final Object actual, final Description mismatchDescription, final Gson gson) {
		JsonElement jsonTree = gson.toJsonTree(actual);
		if (!jsonTree.isJsonPrimitive() && !jsonTree.isJsonNull()) {
			mismatchDescription.appendText("\n" + gson.toJson(actual));
		}
	}

	private void appendFieldPath(final Matcher<?> matcher, final Description mismatchDescription) {
		for (Entry<String, Matcher<?>> entry : customMatchers.entrySet()) {
			if (entry.getValue().equals(matcher)) {
				mismatchDescription.appendText(entry.getKey()).appendText(" ");
			}
		}
	}

	@VisibleForTesting
	void setJsonMatcherUtils(JsonMatcherUtils jsonMatcherUtils){
		this.jsonMatcherUtils = jsonMatcherUtils;
	}

}
