package org.folio.circulation.support.utils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
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
   * Get standard formatters.
   * 
   * @return A list of standard formatters.
   */
  public static List<DateTimeFormatter> getFormatters() {
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
   * Parse the given value, returning a date using system time zone.
   *
   * For compatibility with JodaTime, when value is null, then used now().
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
   * For compatibility with JodaTime, when value is null, then used now().
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A date parsed from the value.
   */
  public static LocalDate parseDate(String value, ZoneId zone) {
    if (value == null) {
      return normalizeDate(null, zone);
    }

    List<DateTimeFormatter> formatters = getFormatters();

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
   * For compatibility with JodaTime, when value is null, then used now().
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
   * For compatibility with JodaTime, when value is null, then used now().
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A dateTime parsed from the value.
   */
  public static ZonedDateTime parseDateTime(String value, ZoneId zone) {
    if (value == null) {
      return normalizeDateTime(null, zone);
    }

    List<DateTimeFormatter> formatters = getFormatters();

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
   * For compatibility with JodaTime, when value is null, then used now().
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
   * For compatibility with JodaTime, when value is null, then used now().
   * For compatibility with JodaTime, when zone is null, then
   * system default.
   *
   * @param value The value to parse into a LocalDate.
   * @param zone The time zone to use when parsing.
   * @return A time parsed from the value.
   */
  public static LocalTime parseTime(String value, ZoneId zone) {
    if (value == null) {
      return normalizeTime(null, zone);
    }

    List<DateTimeFormatter> formatters = getFormatters();

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
   * @return The provided dateTime or if dateTime is null now().
   */
  public static ZonedDateTime normalizeDateTime(ZonedDateTime dateTime) {
    return normalizeDateTime(dateTime, null);
  }

  /**
   * Given a dateTime, normalize it.
   *
   * JodaTime defaults the now() when dateTime is null.
   * Java time does not.
   *
   * @param dateTime The dateTime to normalize. 
   * @param zone The zone to use if dateTime is null.
   * @return The provided dateTime or if dateTime is null now().
   */
  public static ZonedDateTime normalizeDateTime(ZonedDateTime dateTime, ZoneId zone) {
    if (dateTime == null) {
      return ZonedDateTime.now(normalizeZone(zone));
    }
    return dateTime;
  }

  /**
   * Given a date, normalize it.
   *
   * JodaTime defaults the now() when date is null.
   * Java time does not.
   *
   * @param dateTime The date to normalize. 
   * @return The provided date or if date is null now().
   */
  public static LocalDate normalizeDate(LocalDate date) {
    return normalizeDate(date, null);
  }

  /**
   * Given a date, normalize it.
   *
   * JodaTime defaults the now() when date is null.
   * Java time does not.
   *
   * @param date The date to normalize. 
   * @param zone The zone to use if date is null.
   * @return The provided date or if date is null now().
   */
  public static LocalDate normalizeDate(LocalDate date, ZoneId zone) {
    if (date == null) {
      return LocalDate.now(normalizeZone(zone));
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
   * @return The provided time or if time is null now().
   */
  public static LocalTime normalizeTime(LocalTime time) {
    return normalizeTime(time, null);
  }

  /**
   * Given a time, normalize it.
   *
   * JodaTime defaults the now() when time is null.
   * Java time does not.
   *
   * @param time The time to normalize. 
   * @param zone The zone to use if time is null.
   * @return The provided date or if time is null now().
   */
  public static LocalTime normalizeTime(LocalTime time, ZoneId zone) {
    if (time == null) {
      return LocalTime.now(normalizeZone(zone));
    }
    return time;
  }

  /**
   * Get the DateTime as a string using format "yyyy-MM-dd", in UTC.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String toDateString(ZonedDateTime dateTime) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string. 
    if (dateTime == null) {
      return "";
    }

    return dateTime.withZoneSameInstant(ZoneOffset.UTC)
      .format(ISO_LOCAL_DATE);
  }

  /**
   * Get the DateTime as a string using format "yyyy-MM-dd", in UTC.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String toDateString(OffsetDateTime dateTime) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string.
    if (dateTime == null) {
      return "";
    }

    return dateTime.withOffsetSameInstant(ZoneOffset.UTC)
      .format(ISO_LOCAL_DATE);
  }

  /**
   * Get the date as a string using format "yyyy-MM-dd".
   *
   * @param date The date to convert to a string.
   * @return The converted date string.
   */
  public static String toDateString(LocalDate date) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string. 
    if (date == null) {
      return "";
    }

    return date.format(ISO_LOCAL_DATE);
  }

  /**
   * Get the DateTime as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String toDateTimeString(ZonedDateTime dateTime) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string.
    if (dateTime == null) {
      return "";
    }

    return dateTime.withZoneSameInstant(ZoneOffset.UTC)
      .format(DATE_TIME);
  }

  /**
   * Get the DateTime as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * @param dateTime The dateTime to convert to a string.
   * @return The converted dateTime string.
   */
  public static String toDateTimeString(OffsetDateTime dateTime) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string.
    if (dateTime == null) {
      return "";
    }

    return dateTime.withOffsetSameInstant(ZoneOffset.UTC)
      .format(DATE_TIME);
  }

  /**
   * Get the date as a string using format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", in UTC.
   *
   * The time is set to Midnight.
   *
   * @param date The date to convert to a string.
   * @return The converted date string.
   */
  public static String toDateTimeString(LocalDate date) {
    // TODO: check to see if that when dateTime is null now() must be returned instead of an empty string.
    if (date == null) {
      return "";
    }

    return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      .format(DATE_TIME);
  }

  public static ZonedDateTime atEndOfTheDay(ZonedDateTime dateTime) {
    return dateTime
      .withHour(23)
      .withMinute(59)
      .withSecond(59);
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
    return ZonedDateTime.of(date, time, ZoneOffset.UTC);
  }

  public static ZoneOffset toZoneOffset(ZoneId zoneId) {
    return ZoneOffset.of(zoneId.getId());
  }

  public static ZoneOffset toZoneOffset(String timezone) {
    return ZoneOffset.of(ZoneId.of(timezone).getId());
  }

  public static ZonedDateTime toStartOfDayDateTime(LocalDate localDate) {
    return toUtcDateTime(localDate, LocalTime.MIDNIGHT);
  }
}
