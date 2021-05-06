package api.support.matchers;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.folio.circulation.support.utils.DateTimeUtil.toDateTimeString;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.folio.circulation.support.ClockManager;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TextDateTimeMatcher {

  private static int MILLIS_PER_SECOND = 1000;

  public static Matcher<String> isEquivalentTo(ZonedDateTime expected) {
    return isEquivalentTo(expected.toOffsetDateTime());
  }

  public static Matcher<String> isEquivalentTo(OffsetDateTime expected) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time matching: %s", toDateTimeString(expected)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        //response representation might vary from request representation
        OffsetDateTime actual = ZonedDateTime.parse(textRepresentation).toOffsetDateTime();

        return expected.isEqual(actual);
      }
    };
  }

  public static Matcher<String> isEquivalentTo(Instant expected) {
    return isEquivalentToAtUTC(expected);
  }

  public static Matcher<String> isEquivalentToAtUTC(ZonedDateTime expected) {
    return isEquivalentToAtUTC(expected.toInstant());
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
        final var actual = OffsetDateTime.parse(textRepresentation);

        //The zoned date time could have a higher precision than milliseconds
        //This makes comparison to an ISO formatted date time using milliseconds
        //excessively precise and brittle
        //Discovered when using JDK 13.0.1 instead of JDK 1.8.0_202-b08
        return expected.truncatedTo(MILLIS).equals(actual.toInstant())
          && actual.getOffset().equals(UTC);
      }
    };
  }

  public static Matcher<String> withinSecondsAfter(int seconds, ZonedDateTime after) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time within %s seconds after %s",
          seconds, toDateTimeString(after)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        //response representation might vary from request representation
        ZonedDateTime actual = ZonedDateTime.parse(textRepresentation);

        return !actual.isBefore(after) &&
          secondsBetween(after, actual) < seconds;
      }
    };
  }

  public static Matcher<String> withinSecondsBefore(int seconds, ZonedDateTime before) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText(String.format(
          "a date time within %s seconds before %s",
          seconds, toDateTimeString(before)));
      }

      @Override
      protected boolean matchesSafely(String textRepresentation) {
        ZonedDateTime actual = ZonedDateTime.parse(textRepresentation);

        return actual.isBefore(before) &&
          secondsBetween(actual, before) < seconds;
      }
    };
  }

  public static Matcher<String> withinSecondsBeforeNow(int seconds) {
    return withinSecondsBefore(seconds, ClockManager.getClockManager().getZonedDateTime());
  }

  private static int secondsBetween(ZonedDateTime start, ZonedDateTime end) {
    final long millis = end.toInstant().toEpochMilli() - start.toInstant()
      .toEpochMilli();

    return (int) (millis / MILLIS_PER_SECOND);
  }
}
