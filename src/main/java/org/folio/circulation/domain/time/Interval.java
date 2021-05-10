package org.folio.circulation.domain.time;

import static java.time.Duration.ofMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isAfterMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isBeforeMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isSameMillis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * A simple time interval using an inclusive begin and a duration.
 * <p>
 * The begin represents a zoned date and time in which the interval
 * begins, inclusively.
 * <p>
 * The duration represents how long from the begin date and time.
 * <p>
 * This is immutable.
 */
@AllArgsConstructor
@Builder
@Value
@With
public class Interval {

  private final ZonedDateTime begin;
  private final Duration duration;

  /**
   * Initialize to current time at UTC and a 0 duration.
   */
  public Interval() {
    begin = ZonedDateTime.now(Clock.systemUTC());
    this.duration = ofMillis(0L);
  }

  /**
   * Initialize from date and time range, with timezone.
   *
   * @param begin The inclusive begin time.
   * @param end The exclusive end time.
   */
  public Interval(ZonedDateTime begin, ZonedDateTime end) {
    this.begin = begin;
    if (isBeforeMillis(begin, end)) {
      this.duration = ofMillis(end.toInstant().toEpochMilli()
        - begin.toInstant().toEpochMilli());
    }
    else {
      this.duration = ofMillis(0L);
    }
  }

  /**
   * Initialize from date and time range, without timezone.
   * <p>
   * This will use the system's timezone to locally represent the range.
   *
   * @param begin The inclusive begin time.
   * @param end The exclusive end time.
   */
  public Interval(LocalDateTime begin, LocalDateTime end) {
    final ZoneOffset zone = (ZoneOffset) Clock.systemDefaultZone().getZone();
    this.begin = ZonedDateTime.of(begin, zone);
    if (isBeforeMillis(begin, end)) {
      this.duration = ofMillis(end.toInstant(zone).toEpochMilli()
        - begin.toInstant(zone).toEpochMilli());
    }
    else {
      this.duration = ofMillis(0L);
    }
  }

  /**
   * Initialize from date and time range, in Epoch milliseconds.
   *
   * @param begin The inclusive begin time, in Epoch milliseconds.
   * @param end The exclusive end time, in Epoch milliseconds.
   * @param zone The time zone.
   */
  public Interval(long begin, long end) {
    this(begin, end, ZoneOffset.UTC);
  }

  /**
   * Initialize from date and time range, in Epoch milliseconds.
   *
   * @param begin The inclusive begin time, in Epoch milliseconds.
   * @param end The exclusive end time, in Epoch milliseconds.
   * @param zone The time zone.
   */
  public Interval(long begin, long end, ZoneId zone) {
    this.begin = ZonedDateTime.ofInstant(Instant.ofEpochMilli(begin), zone);
    if (begin < end) {
      this.duration = ofMillis(end - begin);
    }
    else {
      this.duration = ofMillis(0L);
    }
  }

  /**
   * Determine if passed date and time is within the duration.
   * <p>
   * The begin is inclusive and the end is exclusive.
   * 
   * @param dateTime The date to compare against.
   * @return A boolean of if the dateTime was found within the duration.
   */
  public boolean contains(ZonedDateTime dateTime) {
    return isSameMillis(begin, dateTime) || isAfterMillis(dateTime, begin)
      && isBeforeMillis(dateTime, begin.plus(duration));
  }

  /**
   * Retrieve the date and time the interval inclusively begins.
   *
   * @return The date and time.
   */
  public ZonedDateTime getStart() {
    return ZonedDateTime.from(begin);
  }

  /**
   * Retrieve the date and time the interval inclusively begins.
   *
   * @return The date and time.
   */
  public ZonedDateTime getEnd() {
    return begin.plus(duration);
  }

  /**
   * Retrieve the timezone information.
   *
   * @return The timezone.
   */
  public ZoneId getZone() {
    return begin.getZone();
  }

  /**
   * Determine if the intervals are immediately before or after without
   * overlapping.
   *
   * @param interval The interval to compare, set to null for now().
   * @return true if abuts, false otherwise.
   */
  public boolean abuts(Interval interval) {
    final long start = begin.toInstant().toEpochMilli();
    final long end = getEnd().toInstant().toEpochMilli();
    if (interval == null) {
      final long now = ZonedDateTime.now(Clock.systemUTC()).toInstant()
        .toEpochMilli(); 
      return now == start || now == end;
    }
    return interval.getEnd().toInstant().toEpochMilli() == start ||
      interval.getStart().toInstant().toEpochMilli() == end;
  }

  /**
   * Determine the difference in time between the intervals.
   *
   * @param interval The interval to use.
   * @return A new interval representing the gap or null if no gap found.
   */
  public Interval gap(Interval interval) {
    final long start = begin.toInstant().toEpochMilli();
    final long end = getEnd().toInstant().toEpochMilli();
    final long iStart = interval.getStart().toInstant().toEpochMilli();
    final long iEnd = interval.getEnd().toInstant().toEpochMilli();
    if (start > iEnd) {
      return new Interval(iEnd, start);
    }
    else if (iStart > end) {
      return new Interval(end, iStart);
    }
    return null;
  }
}
