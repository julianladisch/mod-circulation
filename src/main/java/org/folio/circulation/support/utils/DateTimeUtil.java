package org.folio.circulation.support.utils;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DateTimeUtil {
  private DateTimeUtil() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  /**
   * A variation of ISO_LOCAL_TIME down to minutes.
   */
  public static final DateTimeFormatter TIME_MINUTES;
  static {
    TIME_MINUTES = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendValue(HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(MINUTE_OF_HOUR, 2)
      .toFormatter();
  }

  /**
   * A variation of ISO_LOCAL_TIME down to seconds.
   */
  public static final DateTimeFormatter TIME_SECONDS;
  static {
    TIME_SECONDS = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(TIME_MINUTES)
      .appendLiteral(':')
      .appendValue(SECOND_OF_MINUTE, 2)
      .toFormatter();
  }

  /**
   * A variation of ISO_LOCAL_TIME down to milliseconds.
   */
  public static final DateTimeFormatter TIME;
  static {
    TIME = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(TIME_SECONDS)
      .optionalStart()
      .appendFraction(MILLI_OF_SECOND, 3, 3, true)
      //.appendFraction(NANO_OF_SECOND, 0, 9, true)
      .parseLenient()
      .appendOffset("+HHMM", "Z")
      .parseStrict()
      .toFormatter();
  }

  /**
   * A variation of ISO_LOCAL_TIME down to nanoseconds.
   */
  public static final DateTimeFormatter TIME_NANOSECONDS;
  static {
    TIME_NANOSECONDS = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(TIME_SECONDS)
      .optionalStart()
      .appendFraction(NANO_OF_SECOND, 0, 9, true)
      .parseLenient()
      .appendOffset("+HHMM", "Z")
      .parseStrict()
      .toFormatter();
  }

  /**
   * A variation of ISO_OFFSET_DATE_TIME down to milliseconds.
   */
  public static final DateTimeFormatter DATE_TIME;
  static {
    DATE_TIME = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(ISO_LOCAL_DATE)
      .appendLiteral('T')
      .append(TIME)
      .toFormatter();
  }

  /**
   * A variation of ISO_OFFSET_DATE_TIME down to nanoseconds.
   */
  public static final DateTimeFormatter DATE_TIME_NANOSECONDS;
  static {
    DATE_TIME_NANOSECONDS = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(ISO_LOCAL_DATE)
      .appendLiteral('T')
      .append(TIME_NANOSECONDS)
      .toFormatter();
  }

  /**
   * Get standard dateTime formatters.
   *
   * @return A list of standard formatters.
   */
  public static List<DateTimeFormatter> getDateTimeFormatters() {
    DateTimeFormatter startOfDay = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
      .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
      .toFormatter();

    return List.of(
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      DateTimeFormatter.ISO_ZONED_DATE_TIME,
      DATE_TIME,
      startOfDay
    );
  }

  /**
   * Get standard time formatters.
   *
   * @return A list of standard formatters.
   */
  public static List<DateTimeFormatter> getTimeFormatters() {
    DateTimeFormatter startOfDay = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
      .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
      .toFormatter();

    return List.of(
      DateTimeFormatter.ISO_TIME,
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      DateTimeFormatter.ISO_ZONED_DATE_TIME,
      TIME,
      DATE_TIME,
      startOfDay
    );
  }

  /**
   * Parse the given value, returning a date using system time zone.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   *
   * @param value The value to parse into a LocalDate.
   * @return A date parsed from the value.
   */
  public static LocalDate parseDate(String value) {
    return parseDate(value, null);
  }

  /**
   * Parse the given value, returning a date.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A date parsed from the value.
   */
  public static LocalDate parseDate(String value, ZoneId zone) {
    if (value == null) {
      return normalizeDate(null);
    }

    List<DateTimeFormatter> formatters = getDateTimeFormatters();

    for (int i = 0; i < formatters.size(); i++) {
      try {
        DateTimeFormatter formatter = formatters.get(i)
          .withZone(normalizeZone(zone));
        return LocalDate.parse(value, formatter);
      } catch (DateTimeParseException e1) {
        if (i == formatters.size() - 1) {
          throw e1;
        }
      }
    }

    return LocalDate.parse(value);
  }

  /**
   * Parse the given value, returning a dateTime using system time zone.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   *
   * @param value The value to parse into a LocalDate.
   * @return A dateTime parsed from the value.
   */
  public static ZonedDateTime parseDateTime(String value) {
    return parseDateTime(value, null);
  }

  /**
   * Parse the given value, returning a dateTime.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A dateTime parsed from the value.
   */
  public static ZonedDateTime parseDateTime(String value, ZoneId zone) {
    if (value == null) {
      return normalizeDateTime((ZonedDateTime) null).withZoneSameInstant(zone);
    }

    List<DateTimeFormatter> formatters = getDateTimeFormatters();

    for (int i = 0; i < formatters.size(); i++) {
      try {
        DateTimeFormatter formatter = formatters.get(i)
          .withZone(normalizeZone(zone));
        return ZonedDateTime.parse(value, formatter);
      } catch (DateTimeParseException e1) {
        if (i == formatters.size() - 1) {
          throw e1;
        }
      }
    }

    return ZonedDateTime.parse(value);
  }

  /**
   * Parse the given value, returning a time using system time zone.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   *
   * @param value The value to parse into a LocalDate.
   * @return A time parsed from the value.
   */
  public static LocalTime parseTime(String value) {
    return parseTime(value, null);
  }

  /**
   * Parse the given value, returning a date.
   *
   * For compatibility with JodaTime, when value is null, then used now(Clock.systemUTC()).
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A time parsed from the value.
   */
  public static LocalTime parseTime(String value, ZoneId zone) {
    if (value == null) {
      return normalizeTime(null);
    }

    List<DateTimeFormatter> formatters = getTimeFormatters();

    for (int i = 0; i < formatters.size(); i++) {
      try {
        DateTimeFormatter formatter = formatters.get(i)
          .withZone(normalizeZone(zone));
        return LocalTime.parse(value, formatter);
      } catch (DateTimeParseException e1) {
        if (i == formatters.size() - 1) {
          throw e1;
        }
      }
    }

    return LocalTime.parse(value);
  }

  /**
   * Given a time zone, normalize it.
   *
   * JodaTime defaults the time zone when zone is null.
   * Java time does not.
   *
   * @param zone The time zone to normalize.
   * @return The provided time zone or if zone is null a default time zone.
   */
  public static ZoneId normalizeZone(ZoneId zone) {
    if (zone == null) {
      return ZoneId.systemDefault();
    }
    return zone;
  }


  /**
   * Given a dateTime, normalize it.
   *
   * JodaTime defaults the now() when dateTime is null.
   * Java time does not.
   *
   * @param dateTime The dateTime to normalize.
   * @return The provided dateTime or if dateTime is null now(Clock.systemUTC()).
   */
  public static ZonedDateTime normalizeDateTime(ZonedDateTime dateTime) {
    if (dateTime == null) {
      return ZonedDateTime.now(Clock.systemUTC());
    }
    return dateTime;
  }

  /**
   * Given an offset dateTime, normalize it.
   *
   * JodaTime defaults the now() when dateTime is null.
   * Java time does not.
   *
   * @param dateTime The dateTime to normalize.
   * @return The provided dateTime or if dateTime is null now(Clock.systemUTC()).
   */
  public static OffsetDateTime normalizeDateTime(OffsetDateTime dateTime) {
    if (dateTime == null) {
      return OffsetDateTime.now(Clock.systemUTC());
    }
    return dateTime;
  }

  /**
   * Given a local dateTime, normalize it.
   *
   * JodaTime defaults the now() when dateTime is null.
   * Java time does not.
   *
   * @param dateTime The dateTime to normalize.
   * @return The provided dateTime or if dateTime is null now(Clock.systemUTC()).
   */
  public static LocalDateTime normalizeDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return LocalDateTime.now(Clock.systemUTC());
    }
    return dateTime;
  }

  /**
   * Given a date, normalize it.
   *
   * JodaTime defaults the now() when date is null.
   * Java time does not.
   *
   * @param date The date to normalize.
   * @return The provided date or if date is null now(Clock.systemUTC()).
   */
  public static LocalDate normalizeDate(LocalDate date) {
    if (date == null) {
      return LocalDate.now(Clock.systemUTC());
    }
    return date;
  }

  /**
   * Given a time, normalize it.
   *
   * JodaTime defaults the now() when time is null.
   * Java time does not.
   *
   * @param time The time to normalize.
   * @return The provided date or if time is null now(Clock.systemUTC()).
   */
  public static LocalTime normalizeTime(LocalTime time) {
    if (time == null) {
      return LocalTime.now(Clock.systemUTC());
    }
    return time;
  }

  /**
   * Format the dateTime as a string using format "yyyy-MM-dd", in UTC.
   *
   * This will normalize the dateTime.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String formatDate(ZonedDateTime dateTime) {
    return normalizeDateTime(dateTime).withZoneSameInstant(UTC)
      .format(ISO_LOCAL_DATE);
  }

  /**
   * Format the offset dateTime as a string using format "yyyy-MM-dd", in UTC.
   *
   * This will normalize the offset dateTime.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String formatDate(OffsetDateTime dateTime) {
    return normalizeDateTime(dateTime)
      .withOffsetSameInstant(UTC).format(ISO_LOCAL_DATE);
  }

  /**
   * Format the date as a string using format "yyyy-MM-dd".
   *
   * This will normalize the date.
   *
   * @param date The date to convert to a string.
   * @return The converted date string.
   */
  public static String formatDate(LocalDate date) {
    return normalizeDate(date).format(ISO_LOCAL_DATE);
  }

  /**
   * Format the dateTime as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * This will normalize the dateTime.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String formatDateTime(ZonedDateTime dateTime) {
    return normalizeDateTime(dateTime).withZoneSameInstant(UTC)
      .format(DATE_TIME);
  }

  /**
   * Format the dateTime as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * This will normalize the dateTime.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String formatDateTimeNanoseconds(ZonedDateTime dateTime) {
    return normalizeDateTime(dateTime).withZoneSameInstant(UTC)
      .format(DATE_TIME_NANOSECONDS);
  }

  /**
   * Format the offset dateTime as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * This will normalize the offset dateTime.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String formatDateTime(OffsetDateTime dateTime) {
    return normalizeDateTime(dateTime)
      .withOffsetSameInstant(UTC).format(DATE_TIME);
  }

  /**
   * Format the date as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * The time is set to Midnight.
   *
   * This will normalize the date.
   *
   * @param date The date to convert to a string.
   * @return The converted date string.
   */
  public static String formatDateTime(LocalDate date) {
    return ZonedDateTime.of(normalizeDate(date), LocalTime.MIDNIGHT,
      UTC).format(DATE_TIME);
  }

  public static ZonedDateTime atEndOfTheDay(ZonedDateTime dateTime) {
    return dateTime.withHour(23).withMinute(59).withSecond(59);
  }

  public static OffsetDateTime toOffsetDateTime(ZonedDateTime dateTime) {
    return OffsetDateTime.ofInstant(dateTime.toInstant(), dateTime.getZone());
  }

  public static ZonedDateTime mostRecentDate(ZonedDateTime... dates) {
    return Stream.of(dates)
      .filter(Objects::nonNull)
      .max(ZonedDateTime::compareTo)
      .orElse(null);
  }

  public static ZonedDateTime toUtcDateTime(LocalDate date, LocalTime time) {
    return ZonedDateTime.of(date, time, UTC);
  }

  public static ZoneOffset toZoneOffset(ZoneId zoneId) {
    return ZoneOffset.of(zoneId.getRules().getOffset(Instant.now()).getId());
  }

  public static ZoneOffset toZoneOffset(String timezone) {
    return ZoneOffset.of(ZoneId.of(timezone).getRules().getOffset(Instant.now()).getId());
  }

  public static ZonedDateTime toStartOfDayDateTime(LocalDate localDate) {
    return toUtcDateTime(localDate, LocalTime.MIDNIGHT);
  }

  /**
   * Check if the the left date is before the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isBeforeMillis(ZonedDateTime left, ZonedDateTime right) {
    return normalizeDateTime(left).toInstant().toEpochMilli() <
      normalizeDateTime(right).toInstant().toEpochMilli();
  }

  /**
   * Check if the the left date is before the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isBeforeMillis(LocalDateTime left, LocalDateTime right) {
    return normalizeDateTime(left).toInstant(UTC).toEpochMilli() <
      normalizeDateTime(right).toInstant(UTC).toEpochMilli();
  }

  /**
   * Check if the the left time is before the right time in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * @param left the time to compare on the left.
   * @param right the time to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isBeforeMillis(LocalTime left, LocalTime right) {
    return normalizeTime(left).truncatedTo(ChronoUnit.MILLIS)
      .isBefore(normalizeTime(right).truncatedTo(ChronoUnit.MILLIS));
  }

  /**
   * Check if the the left date is after the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isAfterMillis(ZonedDateTime left, ZonedDateTime right) {
    return normalizeDateTime(left).toInstant().toEpochMilli() >
      normalizeDateTime(right).toInstant().toEpochMilli();
  }

  /**
   * Check if the the left date is after the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isAfterMillis(LocalDateTime left, LocalDateTime right) {
    return normalizeDateTime(left).toInstant(UTC).toEpochMilli() >
      normalizeDateTime(right).toInstant(UTC).toEpochMilli();
  }

  /**
   * Check if the the left time is after the right time in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * @param left the time to compare on the left.
   * @param right the time to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isAfterMillis(LocalTime left, LocalTime right) {
    return normalizeTime(left).truncatedTo(ChronoUnit.MILLIS)
      .isAfter(normalizeTime(right).truncatedTo(ChronoUnit.MILLIS));
  }

  /**
   * Check if the date is within the first and last dates, exclusively.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * @param date the date to check if is within.
   * @param first the date representing the beginning.
   * @param last the date representing the end.
   * @return true if date is within and false otherwise.
   */
  public static boolean isWithinMillis(LocalDateTime date, LocalDateTime first, LocalDateTime last) {
    if (normalizeDateTime(date).toInstant(UTC).toEpochMilli() >
      normalizeDateTime(first).toInstant(UTC).toEpochMilli()) {

      if (normalizeDateTime(date).toInstant(UTC).toEpochMilli() <
        normalizeDateTime(last).toInstant(UTC).toEpochMilli()) {

        return true;
      }
    }

    return false;
  }

  /**
   * Check if the date is within the first and last dates, exclusively.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * @param date the date to check if is within.
   * @param first the date representing the beginning.
   * @param last the date representing the end.
   * @return true if date is within and false otherwise.
   */
  public static boolean isWithinMillis(ZonedDateTime date, ZonedDateTime first, ZonedDateTime last) {
    if (normalizeDateTime(date).toInstant().toEpochMilli() >
      normalizeDateTime(first).toInstant().toEpochMilli()) {

      if (normalizeDateTime(date).toInstant().toEpochMilli() <
        normalizeDateTime(last).toInstant().toEpochMilli()) {

        return true;
      }
    }

    return false;
  }

  /**
   * Check if the the left date is the same as the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isSameMillis(ZonedDateTime left, ZonedDateTime right) {
    return normalizeDateTime(left).toInstant().toEpochMilli() ==
      normalizeDateTime(right).toInstant().toEpochMilli();
  }

  /**
   * Check if the the left date is the same as the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isSameMillis(LocalDateTime left, LocalDateTime right) {
    return normalizeDateTime(left).toInstant(UTC).toEpochMilli() ==
      normalizeDateTime(right).toInstant(UTC).toEpochMilli();
  }

  /**
   * Check if the the left time is the same as the right time in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static boolean isSameMillis(LocalTime left, LocalTime right) {
    return normalizeTime(left).truncatedTo(ChronoUnit.MILLIS) ==
      normalizeTime(right).truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Compare the the left date with the right date in milliseconds.
   *
   * The isBefore()/isAfter() methods tend to work with nanoseconds and cannot
   * be used safely for millisecond comparisons.
   *
   * The compareTo() method states that it compares to millis but this appears
   * to not be the case. Instead, this takes the approach of directly
   * converting to epoch millis to guarantee positioning and granularity before
   * comparing.
   *
   * @param left the date to compare on the left.
   * @param right the date to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static int compareToMillis(ZonedDateTime left, ZonedDateTime right) {
    if (normalizeDateTime(left).toInstant().toEpochMilli() ==
      normalizeDateTime(right).toInstant().toEpochMilli()) {

      return 0;
    }

    if (left.toInstant().toEpochMilli() < right.toInstant().toEpochMilli()) {
      return -1;
    }

    return 0;
  }

  /**
   * Compare the the left time with the right time in milliseconds.
   *
   * @param left the time to compare on the left.
   * @param right the time to compare on the right.
   * @return true if left is before right and false otherwise.
   */
  public static int compareToMillis(LocalTime left, LocalTime right) {
    return normalizeTime(left).truncatedTo(ChronoUnit.MILLIS)
      .compareTo(normalizeTime(right).truncatedTo(ChronoUnit.MILLIS));
  }
}
