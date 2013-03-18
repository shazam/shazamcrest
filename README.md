Shazamcrest
===========

'Shazamcrest' is a library that extends the functionality of [hamcrest](see http://hamcrest.org/).

Assertions on complete beans are made simpler by serialising the actual and expected beans to json, and comparing
  the two. The diagnostics are leveraging the comparison functionality of IDEs like Eclipse or IntelliJ.


Usage
=====

For an example on how to use it, see: [Example](http://git.io/mhvOdw)


Known limitations
=================

Beans are serialised using their getter methods, meaning any fields not having a getter method will be ignored.
