Shazamcrest
===========

'Shazamcrest' is a library that extends the functionality of [hamcrest](http://hamcrest.org/).

Assertions on complete beans are made simpler by serialising the actual and expected beans to json, and comparing
  the two. The diagnostics are leveraging the comparison functionality of IDEs like Eclipse or IntelliJ.


Usage
-----

Having a Person bean with the following structure:

<pre><code>
    Person person
    |-- String name
    |-- String surname
    |-- Address address
        |-- String streetName
        |-- int streetNumber
</code></pre>

to compare two Person beans with Shazamcrest we would write:

<pre><code>
    assertThat(actualPerson, sameBeanAs(expectedPerson));
</code></pre>

instead of explicitly match every field of the bean and sub-beans:

<pre><code>
    assertThat(actualPerson, allOf(
        hasProperty("name", equalTo(expectedPerson.name)),
        hasProperty("surname", equalTo(expectedPerson.surname)),
        hasProperty("address", allOf(
            hasProperty("streetName", equalTo(expectedPerson.address.streetName)),
            hasProperty("streetNumber", equalTo(expectedPerson.address.streetNumber)))
        )
    ));
</code></pre>


Error Messages
-----

If the person address streetName does not match the expectations, the following diagnostic message is displayed:

<pre><code>
    org.junit.ComparisonFailure: address.streetName
        Expected: Via Roma
    got: Via Veneto
        expected:<... "streetName": "Via [Roma]",
    "streetNumber...> but was:<... "streetName": "Via [Veneto]",
    "streetNumber...>
</code></pre>

When the comparison result is a no match, the exception thrown is a ComparisonFailure, which can be used by IDEs like Eclipse and 
 IntelliJ to display a visual representation of the differences.

![Comparison failure diagnostic](http://tech.shazam.com/wp-content/uploads/2013/08/Screenshot.png)


QuickStart
-----

To use, [download the zip](https://github.com/shazam/shazamcrest/archive/master.zip) or add the following to your project's pom.xml:
 
<pre><code>
    <dependency>
        <groupId>com.shazam</groupId>
        <artifactId>shazamcrest</artifactId>
        <version>0.3</version>
    </dependency>
</code></pre>

Known limitations
-----------------

* If a bean contains data with circular references, a StackOverflowError will be thrown during comparison.
* Sets and Maps with same data are serialised in non deterministic order, generating random comparison failures.

