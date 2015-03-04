Changelog
===========

Version 0.11 - 2015/03/04
-----

It's now possible to ignore all the fields which name matches a given Hamcrest matcher.
Fixed diagnostic in case actual value is null.

Version 0.10 - 2015/02/16
-----

Automatic detection of circular references.
Fixed comparison of Guava Optional.

Version 0.9 - 2014/09/17
-----

Fixed random comparison failures for sets and maps.

Version 0.8 - 2014/07/16
-----

Handled circular references.

Version 0.7 - 2013/10/20
-----

Fixed NullPointerException thrown when custom matching is applied to a null object.

Version 0.6 - 2013/10/16
-----

The matcher is now using IsEqual Hamcrest matcher when Enums are compared.

Version 0.5 - 2013/10/14
-----

Description given to assertThat is now preserved in ComparisonFailure.

Version 0.4 - 2013/10/10
-----

Added option to match specific fields with custom matchers.
The matcher is now using IsEqual Hamcrest matcher when Strings or primitives are compared.

Version 0.3 - 2013/08/16
-----

Added option to ignore specific fields or Java types from the comparison.

Version 0.2 - 2013/05/15
-----

Fixed Map serialisation.

Version 0.1 - 2013/03/20
-----

Initial release.
