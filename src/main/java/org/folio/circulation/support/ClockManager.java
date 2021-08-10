package org.folio.circulation.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * A clock manager for safely getting and setting the time.
 */
public class ClockManager {
  private static Clock clock = Clock.systemUTC();

  /**
   * Set the clock assigned to the clock manager to a given clock.
   */
  public static void setClock(Clock clock) {
    if (clock == null) {
      throw new IllegalArgumentException("clock cannot be null");
    }

    ClockManager.clock = clock;
  }

  /**
   * Set the clock assigned to the clock manager to the system clock.
   */
  public static void setDefaultClock() {
    ClockManager.clock = Clock.systemUTC();
  }

  /**
   * Get the clock assigned the the clock manager.
   *
   * @return
   *   The clock currently being used by ClockManager.
   */
  public static Clock getClock() {
    return clock;
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   A ZonedDateTime as if now() is called.
   */
  public static ZonedDateTime getZonedDateTime() {
    return ZonedDateTime.now(clock);
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   An OffsetDateTime as if now() is called.
   */
  public static OffsetDateTime getOffsetDateTime() {
    return OffsetDateTime.now(clock);
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   A LocalDateTime as if now() is called.
   */
  public static LocalDateTime getLocalDateTime() {
    return LocalDateTime.now(clock);
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   A LocalDate as if now() is called.
   */
  public static LocalDate getLocalDate() {
    return LocalDate.now(clock);
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   A LocalDate as if now() is called.
   */
  public static LocalTime getLocalTime() {
    return LocalTime.now(clock);
  }

  /**
   * Get the current system time according to the clock manager.
   *
   * @return
   *   An Instant as if now() is called.
   */
  public static Instant getInstant() {
    return Instant.now(clock);
  }

  /**
   * Get the timezone of the system clock according to the clock manager.
   *
   * @return
   *   The current timezone as a ZoneId.
   */
  public static ZoneId getZoneId() {
    return clock.getZone();
  }

  /**
   * Get the timezone of the system clock according to the clock manager.
   *
   * @return
   *   The current timezone as a ZoneOffset.
   */
  public static ZoneOffset getZoneOffset() {
    return (ZoneOffset) clock.getZone();
  }
}
