package api.support.builders;

import static java.time.Year.isLeap;
import static java.time.ZoneOffset.UTC;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.folio.circulation.support.ClockManager;

public class FixedDueDateSchedule {
  final ZonedDateTime from;
  final ZonedDateTime to;
  public final ZonedDateTime due;

  private static FixedDueDateSchedule dueAtEnd(ZonedDateTime from, ZonedDateTime to) {
    return new FixedDueDateSchedule(from, to, to);
  }

  public FixedDueDateSchedule(ZonedDateTime from, ZonedDateTime to, ZonedDateTime due) {
    this.from = from;
    this.to = to;
    this.due = due;
  }

  public static FixedDueDateSchedule wholeYear(int year) {
    return dueAtEnd(
      ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, UTC),
      ZonedDateTime.of(year, 12, 31, 23, 59, 59, 0, UTC));
  }

  public static FixedDueDateSchedule wholeMonth(int year, int month) {
    final ZonedDateTime firstOfMonth = ZonedDateTime
      .of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    final ZonedDateTime lastOfMonth = firstOfMonth
      .withDayOfMonth(calculateLastDayOfMonth(firstOfMonth))
      .withHour(23)
      .withMinute(59)
      .withSecond(59);

    return dueAtEnd(firstOfMonth, lastOfMonth);
  }

  public static FixedDueDateSchedule wholeMonth(int year, int month, ZonedDateTime dueDate) {
    final ZonedDateTime firstOfMonth = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, UTC);

    final ZonedDateTime lastOfMonth = firstOfMonth
      .withDayOfMonth(firstOfMonth.getMonth().maxLength())
      .withHour(23)
      .withMinute(59)
      .withSecond(59);

    return new FixedDueDateSchedule(firstOfMonth, lastOfMonth, dueDate);
  }

  public static FixedDueDateSchedule todayOnly() {
    return forDay(ClockManager.getZonedDateTime());
  }

  public static FixedDueDateSchedule yesterdayOnly() {
    return forDay(ClockManager.getZonedDateTime().minusDays(1));
  }

  public static FixedDueDateSchedule forDay(ZonedDateTime day) {
    final ZonedDateTime beginningOfDay = day.with(LocalTime.MIN);

    final ZonedDateTime endOfDay = beginningOfDay
      .withHour(23)
      .withMinute(59)
      .withSecond(59);

    return new FixedDueDateSchedule(beginningOfDay, endOfDay, endOfDay);
  }

  private static int calculateLastDayOfMonth(ZonedDateTime firstOfMonth) {
    boolean isLeap = isLeap(firstOfMonth.getYear());

    if (firstOfMonth.getMonthValue() == 2 && !isLeap) {
      return firstOfMonth.getMonth().maxLength() - 1;
    }

    return firstOfMonth.getMonth().maxLength();
  }
}
