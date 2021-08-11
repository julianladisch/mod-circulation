package org.folio.circulation.domain.policy.library;

import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.failureForAbsentTimetable;
import static org.folio.circulation.support.results.Result.failed;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.utils.DateTimeUtil.compareToMillis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.folio.circulation.support.results.Result;

public class EndOfCurrentHoursStrategy extends ShortTermLoansBaseStrategy {

  public static final LocalTime END_OF_A_DAY = LocalTime.MIDNIGHT.minusSeconds(1);

  private final ZonedDateTime currentTime;

  public EndOfCurrentHoursStrategy(ZonedDateTime currentTime, ZoneOffset zone) {
    super(zone);
    this.currentTime = currentTime;
  }

  @Override
  protected Result<ZonedDateTime> calculateIfClosed(
      LibraryTimetable libraryTimetable, LibraryInterval requestedInterval) {

    LibraryInterval currentTimeInterval = libraryTimetable.findInterval(currentTime);

    if (currentTimeInterval == null) {
      return failed(failureForAbsentTimetable());
    }

    if (hasLibraryRolloverWorkingDay(libraryTimetable, requestedInterval)){
      return succeeded(requestedInterval.getPrevious().getEndTime()
        .withZoneSameInstant(zone));
    }

    if (currentTimeInterval.isOpen()) {
      return succeeded(currentTimeInterval.getEndTime()
        .withZoneSameInstant(zone));
    }

    return succeeded(currentTimeInterval.getNext().getEndTime()
      .withZoneSameInstant(zone));
  }

  private boolean hasLibraryRolloverWorkingDay(LibraryTimetable libraryTimetable,
    LibraryInterval requestedInterval) {

    if (isNotSequenceOfWorkingDays(libraryTimetable, requestedInterval)) {
      return false;
    }

    LocalTime endLocalTime = libraryTimetable.getHead().getEndTime()
      .withZoneSameInstant(zone).toLocalTime();
    LocalTime startLocalTime = requestedInterval.getPrevious().getStartTime()
      .withZoneSameInstant(zone).toLocalTime();

    return isDateEqualToBoundaryValueOfDay(endLocalTime, END_OF_A_DAY)
      && isDateEqualToBoundaryValueOfDay(startLocalTime, LocalTime.MIDNIGHT);
  }

  private boolean isNotSequenceOfWorkingDays(LibraryTimetable libraryTimetable,
    LibraryInterval requestedInterval) {

    LocalDate start = requestedInterval.getPrevious().getStartTime()
      .withZoneSameInstant(zone).toLocalDate();
    LocalDate end = libraryTimetable.getHead().getEndTime()
      .withZoneSameInstant(zone).toLocalDate();

    return Math.abs(Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays()) == 0;
  }

  private boolean isDateEqualToBoundaryValueOfDay(LocalTime requestedInterval,
    LocalTime boundaryValueOfDay) {

    return compareToMillis(requestedInterval, boundaryValueOfDay) == 0;
  }
}
