Shazamcrest
===========

'Shazamcrest' is a library that extends the functionality of [hamcrest](http://hamcrest.org/).

Assertions on complete beans are made simpler by serialising the actual and expected beans to json, and comparing
  the two. The diagnostics are leveraging the comparison functionality of IDEs like Eclipse or IntelliJ.


Usage
-----

Having a Person bean with the following structure:

<pre>Person person
    |-- String name
    |-- String surname
    |-- Address address
        |-- String streetName
        |-- int streetNumber</pre>

to compare two Person beans with Shazamcrest we would write:

<code>assertThat(actualPerson, sameBeanAs(expectedPerson));</code>

instead of explicitly match every field of the bean and sub-beans:

<pre><code>assertThat(actualPerson, allOf(
        hasProperty("name", equalTo(expectedPerson.name)),
        hasProperty("surname", equalTo(expectedPerson.surname)),
        hasProperty("address", allOf(
            hasProperty("streetName", equalTo(expectedPerson.address.streetName)),
            hasProperty("streetNumber", equalTo(expectedPerson.address.streetNumber)))
        )
    ));</code></pre>


Error Messages
-----

If the person address streetName does not match the expectations, the following diagnostic message is displayed:

<pre>org.junit.ComparisonFailure: address.streetName
        Expected: Via Roma
    got: Via Veneto
        expected:&lt;... "streetName": "Via [Roma]",
    "streetNumber...&gt; but was:&lt;... "streetName": "Via [Veneto]",
    "streetNumber...&gt;</pre>

The exception thrown is a ComparisonFailure which can be used by IDEs like Eclipse and IntelliJ to display a visual representation of the differences.

![Comparison failure diagnostic](http://tech.shazam.com/wp-content/uploads/2013/08/Screenshot.png)

Note: in order to get the ComparisonFailure on mismatch the "assertThat" to use is com.shazam.shazamcrest.MatcherAssert.assertThat 
rather than org.hamcrest.MatcherAssert.assertThat


Ignoring fields
-----

If we are not interested in matching the street name, we can ignore it:

<code>assertThat(actualPerson, sameBeanAs(expectedPerson).ignoring("address.streetName"));</code>


Custom matching
-----

If we want to make sure that the street name starts with "Via" at least:

<code>assertThat(actualPerson, sameBeanAs(expectedPerson).with("address.streetName"), startsWith("Via"));</code>

where startsWith is an Hamcrest matcher.


QuickStart
-----

To use, [download the zip](https://github.com/shazam/shazamcrest/archive/master.zip) or add the following to your project's pom.xml:
 
    <dependency>
        <groupId>com.shazam</groupId>
        <artifactId>shazamcrest</artifactId>
        <version>0.3</version>
    </dependency>

Known limitations
-----------------

* If a bean contains data with circular references, a StackOverflowError will be thrown during comparison.
* Sets and Maps with same data are serialised in non deterministic order, generating random comparison failures.

