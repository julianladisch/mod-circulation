package org.folio.circulation.domain.policy;

import static org.folio.circulation.support.ClockManager.getClockManager;
import static org.folio.circulation.support.ValidationErrorFailure.failedValidation;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getIntegerProperty;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.results.Result.failed;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.utils.DateTimeUtil.isBeforeMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isSameMillis;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.folio.circulation.support.HttpFailure;
import org.folio.circulation.support.http.server.ValidationError;
import org.folio.circulation.support.results.Result;

import io.vertx.core.json.JsonObject;

public class Period {
  private static final Period ZERO_DURATION_PERIOD = minutes(0);

  private static final String MONTHS = "Months";
  private static final String WEEKS = "Weeks";
  private static final String DAYS = "Days";
  private static final String HOURS = "Hours";
  private static final String MINUTES = "Minutes";

  private static final String DURATION_KEY = "duration";
  private static final String INTERVAL_ID_KEY = "intervalId";

  private static final int MINUTES_PER_HOUR = 60;
  private static final int HOURS_PER_DAY = 24;
  private static final int DAYS_PER_WEEK = 7;
  private static final int DAYS_PER_MONTH = 31;
  private static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
  private static final int MINUTES_PER_WEEK = MINUTES_PER_DAY * DAYS_PER_WEEK;
  private static final int MINUTES_PER_MONTH = MINUTES_PER_DAY * DAYS_PER_MONTH;
  private static final Set<String> SUPPORTED_INTERVAL_IDS = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList(MONTHS, WEEKS, DAYS, HOURS, MINUTES)));

  private final Integer duration;
  private final String interval;

  private Period(Integer duration, String interval) {
    this.duration = duration;
    this.interval = interval;
  }

  public static Period months(int duration) {
    return from(duration, MONTHS);
  }

  public static Period weeks(Integer duration) {
    return from(duration, WEEKS);
  }

  public static Period days(Integer duration) {
    return from(duration, DAYS);
  }

  public static Period hours(int duration) {
    return from(duration, HOURS);
  }

  public static Period minutes(int duration) {
    return from(duration, MINUTES);
  }

  public static Period from(Integer duration, String interval) {
    return new Period(duration, interval);
  }

  public static Result<Period> from(
    JsonObject jsonObject,
    Supplier<HttpFailure> onUnrecognisedPeriod,
    Function<String, HttpFailure> onUnrecognisedInterval,
    IntFunction<HttpFailure> onUnrecognisedDuration) {

    String intervalId = getProperty(jsonObject, INTERVAL_ID_KEY);
    Integer duration = getIntegerProperty(jsonObject, DURATION_KEY, null);

    if (intervalId == null) {
      return failed(onUnrecognisedPeriod.get());
    }
    if (!SUPPORTED_INTERVAL_IDS.contains(intervalId)) {
      return failed(onUnrecognisedInterval.apply(intervalId));
    }
    if (duration == null) {
      return failed(onUnrecognisedPeriod.get());
    }
    if (duration <= 0) {
      return failed(onUnrecognisedDuration.apply(duration));
    }

    return succeeded(Period.from(duration, intervalId));
  }

  public static Period from(JsonObject jsonObject) {
    final String intervalId = getProperty(jsonObject, INTERVAL_ID_KEY);
    final Integer duration = getIntegerProperty(jsonObject, DURATION_KEY, 0);

    return from(duration, intervalId);
  }

  Result<ZonedDateTime> addTo(
    ZonedDateTime from,
    Supplier<ValidationError> onUnrecognisedPeriod,
    Function<String, ValidationError> onUnrecognisedInterval,
    IntFunction<ValidationError> onUnrecognisedDuration) {

    if(interval == null) {
      return failedValidation(onUnrecognisedPeriod.get());
    }

    if(duration == null) {
      return  failedValidation(onUnrecognisedPeriod.get());
    }

    if(duration <= 0) {
      return failedValidation(onUnrecognisedDuration.apply(duration));
    }

    switch (interval) {
      case MONTHS:
        return succeeded(from.plusMonths(duration));
      case WEEKS:
        return succeeded(from.plusWeeks(duration));
      case DAYS:
        return succeeded(from.plusDays(duration));
      case HOURS:
        return succeeded(from.plusHours(duration));
      case MINUTES:
        return succeeded(from.plusMinutes(duration));
      default:
        return failedValidation(onUnrecognisedInterval.apply(interval));
    }
  }

  public JsonObject asJson() {
    JsonObject representation = new JsonObject();

    representation.put(DURATION_KEY, duration);
    representation.put(INTERVAL_ID_KEY, interval);

    return representation;
  }

  public int toMinutes() {
    if (duration == null || interval == null) {
      return 0;
    }

    switch (interval) {
    case MONTHS:
      return duration * MINUTES_PER_MONTH;
    case WEEKS:
      return duration * MINUTES_PER_WEEK;
    case DAYS:
      return duration * MINUTES_PER_DAY;
    case HOURS:
      return duration * MINUTES_PER_HOUR;
    case MINUTES:
      return duration;
    default:
      return 0;
    }
  }

  public boolean hasPassedSinceDateTillNow(ZonedDateTime startDate) {
    final ZonedDateTime now = getClockManager().getZonedDateTime();
    final ZonedDateTime startPlusPeriod = plusDate(startDate);

    return isBeforeMillis(startPlusPeriod, now) || isSameMillis(startPlusPeriod, now);
  }

  public boolean hasNotPassedSinceDateTillNow(ZonedDateTime startDate) {
    return !hasPassedSinceDateTillNow(startDate);
  }

  public boolean isEqualToDateTillNow(ZonedDateTime startDate) {
    final ZonedDateTime now = getClockManager().getZonedDateTime();

    return isSameMillis(now, plusDate(startDate));
  }

  public boolean hasZeroDuration() {
    return duration == 0;
  }

  public static Period zeroDurationPeriod() {
    return ZERO_DURATION_PERIOD;
  }

  public ZonedDateTime plusDate(ZonedDateTime date) {
    switch (interval) {
      case MONTHS:
        return date.plusMonths(duration);
      case WEEKS:
        return date.plusWeeks(duration);
      case DAYS:
        return date.plusDays(duration);
      case HOURS:
        return date.plusHours(duration);
      case MINUTES:
      default:
        return date.plusMinutes(duration);
    }
  }

  public ZonedDateTime minusDate(ZonedDateTime date) {
    switch (interval) {
      case MONTHS:
        return date.minusMonths(duration);
      case WEEKS:
        return date.minusWeeks(duration);
      case DAYS:
        return date.minusDays(duration);
      case HOURS:
        return date.minusHours(duration);
      case MINUTES:
      default:
        return date.minusMinutes(duration);
    }
  }

  @Override
  public String toString() {
    return "Period{" +
      "duration=" + duration +
      ", interval='" + interval + '\'' +
      '}';
  }
}
