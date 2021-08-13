package org.folio.circulation.domain;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.results.ResultBinding.flatMapResult;
import static org.folio.circulation.support.utils.DateTimeUtil.isAfterMillis;
import static org.folio.circulation.support.utils.DateTimeUtil.isBeforeMillis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.circulation.infrastructure.storage.CalendarRepository;
import org.folio.circulation.infrastructure.storage.loans.LoanPolicyRepository;
import org.folio.circulation.support.results.Result;

public class OverduePeriodCalculatorService {
  private static final int ZERO_MINUTES = 0;

  private final CalendarRepository calendarRepository;
  private final LoanPolicyRepository loanPolicyRepository;

  public OverduePeriodCalculatorService(CalendarRepository calendarRepository,
    LoanPolicyRepository loanPolicyRepository) {

    this.calendarRepository = calendarRepository;
    this.loanPolicyRepository = loanPolicyRepository;
  }

  public CompletableFuture<Result<Integer>> getMinutes(Loan loan,
      ZonedDateTime systemTime) {
    final Boolean shouldCountClosedPeriods = loan.getOverdueFinePolicy().getCountPeriodsWhenServicePointIsClosed();

    if (preconditionsAreMet(loan, systemTime, shouldCountClosedPeriods)) {
      return completedFuture(loan)
        .thenComposeAsync(loanPolicyRepository::lookupPolicy)
        .thenApply(r -> r.map(loan::withLoanPolicy))
        .thenCompose(r -> r.after(l -> getOverdueMinutes(l, systemTime, shouldCountClosedPeriods)
            .thenApply(flatMapResult(om -> adjustOverdueWithGracePeriod(l, om)))));
    }

    return completedFuture(succeeded(ZERO_MINUTES));
  }

  boolean preconditionsAreMet(Loan loan, ZonedDateTime systemTime,
      Boolean shouldCountClosedPeriods) {
    return shouldCountClosedPeriods != null && loan.isOverdue(systemTime);
  }

  CompletableFuture<Result<Integer>> getOverdueMinutes(Loan loan,
      ZonedDateTime systemTime, boolean shouldCountClosedPeriods) {
    return shouldCountClosedPeriods || getItemLocationPrimaryServicePoint(loan) == null
      ? minutesOverdueIncludingClosedPeriods(loan, systemTime)
      : minutesOverdueExcludingClosedPeriods(loan, systemTime);
  }

  private CompletableFuture<Result<Integer>> minutesOverdueIncludingClosedPeriods(
      Loan loan, ZonedDateTime systemTime) {

    final int overdueMinutes = (int) Duration.between(loan.getDueDate().toInstant(),
      systemTime.toInstant()).toMinutes();
    return completedFuture(succeeded(overdueMinutes));
  }

  private CompletableFuture<Result<Integer>> minutesOverdueExcludingClosedPeriods(
      Loan loan, ZonedDateTime returnDate) {
    ZonedDateTime dueDate = loan.getDueDate();
    String itemLocationPrimaryServicePoint = getItemLocationPrimaryServicePoint(loan).toString();
    return calendarRepository
      .fetchOpeningDaysBetweenDates(itemLocationPrimaryServicePoint, dueDate, returnDate, false)
      .thenApply(r -> r.next(openingDays -> getOpeningDaysDurationMinutes(
        openingDays, dueDate.toLocalDateTime(), returnDate.toLocalDateTime())));
  }

  Result<Integer> getOpeningDaysDurationMinutes(
    Collection<OpeningDay> openingDays, LocalDateTime dueDate, LocalDateTime returnDate) {

    return succeeded(
      openingDays.stream()
        .mapToInt(day -> getOpeningDayDurationMinutes(day, dueDate, returnDate))
        .sum());
  }

  private int getOpeningDayDurationMinutes(
    OpeningDay openingDay, LocalDateTime dueDate, LocalDateTime systemTime) {

    ZonedDateTime datePart = ZonedDateTime.of(openingDay.getDate(),
      LocalTime.MIDNIGHT, openingDay.getZone());

    return openingDay.getOpeningHour()
      .stream()
      .mapToInt(openingHour -> getOpeningHourDurationMinutes(
        openingHour, datePart, dueDate, systemTime))
      .sum();
  }

  private int getOpeningHourDurationMinutes(OpeningHour openingHour,
    ZonedDateTime datePart, LocalDateTime dueDate, LocalDateTime returnDate) {

    if (allNotNull(datePart, dueDate, openingHour.getStartTime(),
      openingHour.getEndTime())) {

      LocalDateTime startTime =  datePart
        .withHour(openingHour.getStartTime().getHour())
        .withMinute(openingHour.getStartTime().getMinute())
        .withSecond(openingHour.getStartTime().getSecond())
        .withNano(openingHour.getStartTime().getNano())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime();
      LocalDateTime endTime =  datePart
        .withHour(openingHour.getEndTime().getHour())
        .withMinute(openingHour.getEndTime().getMinute())
        .withSecond(openingHour.getEndTime().getSecond())
        .withNano(openingHour.getEndTime().getNano())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime();

      if (isAfterMillis(dueDate, startTime) && isBeforeMillis(dueDate, endTime)) {
        startTime = dueDate;
      }

      if (isAfterMillis(returnDate, startTime)
        && isBeforeMillis(returnDate, endTime)) {

        endTime = returnDate;
      }

      if (isAfterMillis(endTime, startTime) && isAfterMillis(endTime, dueDate)
        && isBeforeMillis(startTime, returnDate)) {

        return (int) Duration.between(startTime, endTime).toMinutes();
      }
    }

    return ZERO_MINUTES;
  }

  Result<Integer> adjustOverdueWithGracePeriod(Loan loan, int overdueMinutes) {
    int result;

    if (shouldIgnoreGracePeriod(loan)) {
      result = overdueMinutes;
    }
    else {
      result = overdueMinutes > getGracePeriodMinutes(loan) ? overdueMinutes : ZERO_MINUTES;
    }

    return Result.succeeded(result);
  }

  private boolean shouldIgnoreGracePeriod(Loan loan) {
    if (!loan.wasDueDateChangedByRecall()) {
      return false;
    }

    Boolean ignoreGracePeriodForRecalls = loan.getOverdueFinePolicy()
      .getIgnoreGracePeriodForRecalls();

    if (ignoreGracePeriodForRecalls == null) {
      return true;
    }

    return ignoreGracePeriodForRecalls;
  }

  private int getGracePeriodMinutes(Loan loan) {
    return loan.getLoanPolicy().getGracePeriod().toMinutes();
  }

  private UUID getItemLocationPrimaryServicePoint(Loan loan) {
    return loan.getItem().getLocation().getPrimaryServicePointId();
  }
}
