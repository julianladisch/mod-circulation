package api.support.matchers;

import static java.time.ZoneOffset.UTC;
import static org.folio.circulation.support.utils.DateTimeUtil.formatDateTime;
import static org.folio.circulation.support.utils.DateTimeUtil.isAfterMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isBeforeMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isSameMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.parseDateTime;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.folio.circulation.support.ClockManager;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TextDateTimeMatcher {

  public static Matcher<String> isEquivalentTo(ZonedDateTime expected) {
    return isEquivalentTo(
      expected == null ? null : expected.toOffsetDateTime());
  }

  public static Matcher<String> isEquivalentTo(OffsetDateTime expected) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time matching: %s", formatDateTime(expected)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        //response representation might vary from request representation
        final ZonedDateTime zoned = parseDateTime(textRepresentation);

        if (zoned == null) {
          return expected == null;
        }

        return expected.isEqual(zoned.toOffsetDateTime());
      }
    };
  }

  public static Matcher<String> isEquivalentTo(Instant expected) {
    return isEquivalentToAtUTC(expected);
  }

  public static Matcher<String> isEquivalentToAtUTC(ZonedDateTime expected) {
    return isEquivalentToAtUTC(expected == null ? null : expected.toInstant());
  }

  public static Matcher<String> isEquivalentToAtUTC(Instant expected) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "an RFC-3339 formatted date and time with a UTC (zero) offset matching: %s", expected.toString()));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        //response representation might vary from request representation
        final ZonedDateTime actual = parseDateTime(textRepresentation);

        if (actual == null) {
          return expected == null;
        }

        //The zoned date time could have a higher precision than milliseconds
        //This makes comparison to an ISO formatted date time using milliseconds
        //excessively precise and brittle
        //Discovered when using JDK 13.0.1 instead of JDK 1.8.0_202-b08
        return isSameMillis(ZonedDateTime.ofInstant(expected, UTC),
          actual.withZoneSameInstant(UTC));
      }
    };
  }

  public static Matcher<String> withinSecondsAfter(int seconds, ZonedDateTime after) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time within %s seconds after %s",
          seconds, formatDateTime(after)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        //response representation might vary from request representation
        final ZonedDateTime actual = parseDateTime(textRepresentation);

        if (actual == null) {
          return false;
        }

        return !isBeforeMillis(actual, after) &&
          isBeforeMillis(actual, after.plusSeconds(seconds));
      }
    };
  }

  public static Matcher<String> withinSecondsBefore(int seconds, ZonedDateTime before) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time within %s seconds before %s",
          seconds, formatDateTime(before)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        final ZonedDateTime actual = parseDateTime(textRepresentation);

        if (actual == null) {
          return false;
        }

        return isAfterMillis(actual, before.minusSeconds(seconds)) &&
          !isAfterMillis(actual, before);
      }
    };
  }

  public static Matcher<String> withinSecondsBeforeNow(int seconds) {
    return withinSecondsBefore(seconds, ClockManager.getClockManager().getZonedDateTime());
  }
}
