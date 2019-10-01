package com.shazam.shazamcrest;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static com.shazam.shazamcrest.matchers.ComparisonFailureMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class MatcherAssertDateTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

    @Test
    public void doesNothingWhenDatesAreTheSame() throws ParseException {
        Date one = DATE_FORMAT.parse("01/01/2016 10:01:23.456");
        Date two = DATE_FORMAT.parse("01/01/2016 10:01:23.456");

        assertThat(one, sameBeanAs(two));
    }

    @Test
    public void throwsComparisonFailureWhenDatesDifferByMillisecondsOnly() throws ParseException {
        Date one = DATE_FORMAT.parse("01/01/2016 10:00:00.300");
        Date two = DATE_FORMAT.parse("01/01/2016 10:00:00.500");

        assertThrows(AssertionFailedError.class, () ->
                assertThat(one, sameBeanAs(two)));
    }

    @Test
    public void throwsComparisonFailureWhenDatesDifferMinutesOnly() throws ParseException {
        Date one = DATE_FORMAT.parse("01/01/2016 10:00:00.000");
        Date two = DATE_FORMAT.parse("01/01/2016 10:00:01.000");

        assertThrows(AssertionFailedError.class, () ->
                assertThat(one, sameBeanAs(two)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void showsMillisecondsOnDates() throws ParseException {
        Date one = DATE_FORMAT.parse("02/01/2016 23:59:59.999");
        Date two = DATE_FORMAT.parse("11/10/966 00:00:00.000");
        try {
            assertThat(one, sameBeanAs(two));
        } catch (AssertionFailedError e) {
            checkThat(e, message(containsString("Oct 11, 0966 12:00:00.000 AM")), message(containsString("Jan 2, 2016 11:59:59.999 PM")));
            return;
        }
        fail("Exception expected but not thrown");
    }

}
