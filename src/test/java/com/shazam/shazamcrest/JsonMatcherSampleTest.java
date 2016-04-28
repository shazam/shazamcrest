package com.shazam.shazamcrest;

import org.junit.Test;

import com.shazam.shazamcrest.matcher.Matchers;

/**
 * Sample test for using {@link com.shazam.shazamcrest.matcher.Matchers#sameJsonAsApproved()}.
 * @author Andras_Gyuro
 *
 */
public class JsonMatcherSampleTest {

	/**
	 * An example for using the Matchers.sameJsonAsApproved() to verify an object.
	 * When first run, the test will create the json file that must be checked and renamed by the developer.
	 * The generated file name will be placed next to the test files in a directory. The generation algorithm
	 * creates SHA-1 hashes from the name of the class and name of the test method and uses these to create the directory name and json
	 * file name.
	 *
	 * The following example will create 'abe6b8\fc5014-not-approved.json' which must be renamed
	 * to 'abe6b8\fc5014-approved.json' after its contents has been verified.
	 * The next test run will find this approved file and will assert its contents to the actual beanChild
	 * object.
	 */
	@Test
	public void testWithSameJsonAsApprovedMatcher(){
		DummyBean beanParent = new DummyBean(1, "a parent bean", true, null);
		DummyBean beanChild = new DummyBean(2, "a child bean", false, beanParent);

		MatcherAssert.assertThat(beanChild, Matchers.sameJsonAsApproved());
	}


	private class DummyBean{

		private int beanInt;
		private String beanString;
		private boolean beanBoolean;
		private DummyBean beanParent;

		public DummyBean(int beanInt, String beanString, boolean beanBoolean, DummyBean beanParent) {
			super();
			this.beanInt = beanInt;
			this.beanString = beanString;
			this.beanBoolean = beanBoolean;
			this.beanParent = beanParent;
		}

		public int getBeanInt() {
			return beanInt;
		}

		public String getBeanString() {
			return beanString;
		}

		public boolean isBeanBoolean() {
			return beanBoolean;
		}

		public DummyBean getBeanParent() {
			return beanParent;
		}

	}

}
